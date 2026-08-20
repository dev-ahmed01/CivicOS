package com.civicos.auth.domain;

import java.time.Instant;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AbstractCreatedEntity {

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Version
	@Column(nullable = false)
	private long version;

	protected RefreshToken() {
	}

	private RefreshToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken issue(User user, String tokenHash, Instant expiresAt) {
		return new RefreshToken(user, tokenHash, expiresAt);
	}

	public boolean isActiveAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void rotateAt(Instant now) {
		lastUsedAt = now;
		revokedAt = now;
	}

	public void revokeAt(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public User getUser() { return user; }
	public String getTokenHash() { return tokenHash; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getRevokedAt() { return revokedAt; }
	public Instant getLastUsedAt() { return lastUsedAt; }
	public long getVersion() { return version; }
}
