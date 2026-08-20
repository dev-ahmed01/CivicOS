package com.civicos.auth.application;

public record TokenPair(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresInSeconds) {

	@Override
	public String toString() {
		return "TokenPair[tokenType=" + tokenType + ", expiresInSeconds=" + expiresInSeconds + "]";
	}
}
