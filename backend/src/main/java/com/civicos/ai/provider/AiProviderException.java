package com.civicos.ai.provider;

public class AiProviderException extends RuntimeException {

	private final boolean retryable;

	public AiProviderException(String message, boolean retryable) {
		super(message);
		this.retryable = retryable;
	}

	public AiProviderException(String message, boolean retryable, Throwable cause) {
		super(message, cause);
		this.retryable = retryable;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
