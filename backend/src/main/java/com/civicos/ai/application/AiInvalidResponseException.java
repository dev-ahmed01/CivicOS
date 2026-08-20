package com.civicos.ai.application;

public class AiInvalidResponseException extends RuntimeException {
	public AiInvalidResponseException(String message) {
		super(message);
	}
}
