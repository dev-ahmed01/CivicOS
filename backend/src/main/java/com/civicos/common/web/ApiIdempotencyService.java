package com.civicos.common.web;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Service
public class ApiIdempotencyService {

	private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9._:-]{8,100}");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final AuthorizationService authorizationService;

	public ApiIdempotencyService(
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper,
			AuthorizationService authorizationService) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
		this.authorizationService = authorizationService;
	}

	@Transactional
	public <T> T execute(
			String key,
			String operation,
			Object request,
			Class<T> responseType,
			Supplier<T> action) {
		String normalizedKey = validateKey(key);
		UUID userId = authorizationService.currentPrincipal().userId();
		String requestHash = hash(request);
		long lockId = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits()
				^ normalizedKey.hashCode();
		jdbcTemplate.query("select pg_advisory_xact_lock(?)", resultSet -> null, lockId);

		StoredResponse existing = jdbcTemplate.query(
				"""
				select operation, request_hash, response_body
				from api_idempotency_keys where user_id = ? and idempotency_key = ?
				""",
				resultSet -> resultSet.next()
						? new StoredResponse(
								resultSet.getString("operation"),
								resultSet.getString("request_hash"),
								resultSet.getString("response_body"))
						: null,
				userId, normalizedKey);
		if (existing != null) {
			if (!existing.operation().equals(operation) || !existing.requestHash().equals(requestHash)) {
				throw new DomainConflictException(
						"Idempotency key was already used for a different request.");
			}
			return read(existing.responseBody(), responseType);
		}

		T response = action.get();
		String responseBody = write(response);
		jdbcTemplate.update(
				"""
				insert into api_idempotency_keys (
				    id, user_id, idempotency_key, operation, request_hash, response_body)
				values (?, ?, ?, ?, ?, ?::jsonb)
				""",
				UUID.randomUUID(), userId, normalizedKey, operation, requestHash, responseBody);
		return response;
	}

	private String validateKey(String key) {
		if (key == null || !SAFE_KEY.matcher(key).matches()) {
			throw new DomainValidationException(
					"Idempotency-Key must contain 8 to 100 safe characters.");
		}
		return key;
	}

	private String hash(Object request) {
		try {
			byte[] bytes = objectMapper.writer()
					.with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
					.writeValueAsBytes(request);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (JsonProcessingException exception) {
			throw new DomainValidationException("Idempotent request must be serializable.");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	private <T> T read(String value, Class<T> responseType) {
		try {
			return objectMapper.readValue(value, responseType);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored idempotency response is invalid.", exception);
		}
	}

	private String write(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Idempotency response cannot be serialized.", exception);
		}
	}

	private record StoredResponse(String operation, String requestHash, String responseBody) {
	}
}
