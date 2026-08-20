package com.civicos.auth.application;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("The refresh token is invalid, expired, or revoked.");
	}
}
