package com.civicos.ai.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.ValidationRules;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class AiRequestPolicy {

	private static final int MAX_CONTEXT_BYTES = 32_768;
	private static final Set<String> SENSITIVE_KEYS = Set.of(
			"password", "passwordhash", "token", "accesstoken", "refreshtoken",
			"secret", "apikey", "privatekey", "credential", "credentials",
			"email", "phone", "fullname", "filebytes", "filecontent", "base64", "binary");

	private final ObjectMapper objectMapper;

	public AiRequestPolicy(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public NormalizedRequest validate(AiRequest request) {
		AiRequest validated = ValidationRules.required(request, "AI request");
		ValidationRules.required(validated.task(), "AI task");
		String entityType = ValidationRules.requiredText(validated.entityType(), "AI entity type").toUpperCase();
		if (entityType.length() > 100) {
			throw new DomainValidationException("AI entity type must not exceed 100 characters.");
		}
		ValidationRules.required(validated.entityId(), "AI entity id");

		JsonNode tree = objectMapper.valueToTree(validated.context());
		assertSafeKeys(tree, "context");
		byte[] canonical = canonicalBytes(tree);
		if (canonical.length > MAX_CONTEXT_BYTES) {
			throw new DomainValidationException("AI context must not exceed 32768 bytes.");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> copiedContext = objectMapper.convertValue(tree, LinkedHashMap.class);
		AiRequest normalized = new AiRequest(
				validated.task(), entityType, validated.entityId(), copiedContext, validated.requestId());
		return new NormalizedRequest(normalized, sha256(canonical));
	}

	private void assertSafeKeys(JsonNode node, String path) {
		if (node instanceof ObjectNode objectNode) {
			objectNode.properties().forEach(entry -> {
				String normalized = entry.getKey().replaceAll("[^A-Za-z0-9]", "")
						.toLowerCase(Locale.ROOT);
				if (SENSITIVE_KEYS.contains(normalized)) {
					throw new DomainValidationException(
							"Sensitive field is not allowed in AI context: " + path + "." + entry.getKey());
				}
				assertSafeKeys(entry.getValue(), path + "." + entry.getKey());
			});
		} else if (node.isArray()) {
			for (int index = 0; index < node.size(); index++) {
				assertSafeKeys(node.get(index), path + "[" + index + "]");
			}
		}
	}

	private byte[] canonicalBytes(JsonNode tree) {
		try {
			return objectMapper.writer()
					.with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
					.writeValueAsBytes(tree);
		} catch (JsonProcessingException exception) {
			throw new DomainValidationException("AI context must be valid structured JSON data.");
		}
	}

	private String sha256(byte[] value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	public record NormalizedRequest(AiRequest request, String inputHash) {
	}
}
