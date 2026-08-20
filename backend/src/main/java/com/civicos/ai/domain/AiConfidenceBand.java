package com.civicos.ai.domain;

import java.math.BigDecimal;

import com.civicos.common.domain.DomainValidationException;

public enum AiConfidenceBand {
	LOW,
	MEDIUM,
	HIGH,
	VERY_HIGH;

	public static AiConfidenceBand from(BigDecimal confidence) {
		if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0
				|| confidence.compareTo(BigDecimal.ONE) > 0) {
			throw new DomainValidationException("AI confidence must be between 0 and 1.");
		}
		if (confidence.compareTo(new BigDecimal("0.50")) < 0) {
			return LOW;
		}
		if (confidence.compareTo(new BigDecimal("0.75")) < 0) {
			return MEDIUM;
		}
		if (confidence.compareTo(new BigDecimal("0.90")) < 0) {
			return HIGH;
		}
		return VERY_HIGH;
	}
}
