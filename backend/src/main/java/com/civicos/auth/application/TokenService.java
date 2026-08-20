package com.civicos.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.config.SecurityProperties;
import com.civicos.auth.domain.RefreshToken;
import com.civicos.auth.repository.RefreshTokenRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class TokenService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

	private final JwtEncoder jwtEncoder;
	private final SecurityProperties properties;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final Clock clock;

	public TokenService(
			JwtEncoder jwtEncoder,
			SecurityProperties properties,
			RefreshTokenRepository refreshTokenRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.clock = clock;
	}

	@Transactional
	public TokenPair issueForLogin(UUID userId, String requestId) {
		User user = activeUser(userId);
		TokenPair pair = issue(user);
		auditEventRepository.save(AuditEvent.securityEvent(
				user, "AUTH_LOGIN_SUCCEEDED", "USER", user.getId(), requestId));
		return pair;
	}

	@Transactional
	public TokenPair refresh(String rawRefreshToken, String requestId) {
		Instant now = clock.instant();
		RefreshToken existing = refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
				.orElseThrow(InvalidRefreshTokenException::new);
		if (!existing.isActiveAt(now) || existing.getUser().getStatus() != User.Status.ACTIVE) {
			throw new InvalidRefreshTokenException();
		}
		existing.rotateAt(now);
		TokenPair pair = issue(existing.getUser());
		auditEventRepository.save(AuditEvent.securityEvent(
				existing.getUser(), "AUTH_TOKEN_REFRESHED", "USER", existing.getUser().getId(), requestId));
		return pair;
	}

	@Transactional
	public void logout(String rawRefreshToken, String requestId) {
		refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken)).ifPresent(token -> {
			token.revokeAt(clock.instant());
			auditEventRepository.save(AuditEvent.securityEvent(
					token.getUser(), "AUTH_LOGOUT", "USER", token.getUser().getId(), requestId));
		});
	}

	private TokenPair issue(User user) {
		Instant issuedAt = clock.instant();
		Instant accessExpiry = issuedAt.plus(properties.jwtExpirationMinutes(), ChronoUnit.MINUTES);
		String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
				JwtClaimsSet.builder()
						.issuer(properties.issuer())
						.issuedAt(issuedAt)
						.expiresAt(accessExpiry)
						.subject(user.getId().toString())
						.id(UUID.randomUUID().toString())
						.build()))
				.getTokenValue();
		String rawRefreshToken = randomToken();
		refreshTokenRepository.save(RefreshToken.issue(
				user,
				hash(rawRefreshToken),
				issuedAt.plus(properties.refreshExpirationDays(), ChronoUnit.DAYS)));
		return new TokenPair(
				accessToken,
				rawRefreshToken,
				"Bearer",
				properties.jwtExpirationMinutes() * 60);
	}

	private User activeUser(UUID userId) {
		User user = userRepository.findWithAuthoritiesById(userId)
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
		if (user.getStatus() != User.Status.ACTIVE) {
			throw new IllegalStateException("Authenticated user is not active.");
		}
		return user;
	}

	private String randomToken() {
		byte[] bytes = new byte[48];
		SECURE_RANDOM.nextBytes(bytes);
		return TOKEN_ENCODER.encodeToString(bytes);
	}

	private String hash(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}
}
