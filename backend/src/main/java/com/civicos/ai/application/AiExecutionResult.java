package com.civicos.ai.application;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.civicos.ai.domain.AiConfidenceBand;
import com.civicos.ai.domain.AiRun;

public record AiExecutionResult(
		UUID runId,
		AiRun.Status status,
		Map<String, Object> output,
		BigDecimal confidence,
		AiConfidenceBand confidenceBand,
		boolean humanReviewRequired,
		String provider,
		String model,
		String promptVersion,
		String schemaVersion,
		String errorMessage) {

	public static AiExecutionResult disabled() {
		return new AiExecutionResult(
				null, AiRun.Status.CANCELLED, Map.of(), null, null, true,
				null, null, null, null, "AI advisory capabilities are disabled.");
	}

	public static AiExecutionResult unavailable(String errorMessage) {
		return new AiExecutionResult(
				null, AiRun.Status.FAILED, Map.of(), null, null, true,
				null, null, null, null, errorMessage);
	}
}
