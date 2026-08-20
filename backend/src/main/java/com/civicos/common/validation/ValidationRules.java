package com.civicos.common.validation;

import com.civicos.common.domain.DomainValidationException;

public final class ValidationRules {

	private ValidationRules() {
	}

	public static <T> T required(T value, String fieldName) {
		if (value == null) {
			throw new DomainValidationException(fieldName + " is required.");
		}
		return value;
	}

	public static String requiredText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new DomainValidationException(fieldName + " is required.");
		}
		return value.strip();
	}

	public static String optionalText(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
