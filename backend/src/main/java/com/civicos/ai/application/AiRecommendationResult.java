package com.civicos.ai.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.civicos.ai.domain.AiRecommendation;

public record AiRecommendationResult(
		UUID recommendationId,
		UUID runId,
		UUID conflictId,
		Map<String, Object> recommendation,
		BigDecimal confidence,
		AiRecommendation.Status status,
		UUID reviewedBy,
		Instant reviewedAt,
		String reviewReason) {

	public static AiRecommendationResult from(AiRecommendation recommendation) {
		return new AiRecommendationResult(
				recommendation.getId(), recommendation.getAiRun().getId(),
				recommendation.getConflict().getId(), recommendation.getRecommendation(),
				recommendation.getConfidence(), recommendation.getStatus(),
				recommendation.getReviewedBy() == null ? null : recommendation.getReviewedBy().getId(),
				recommendation.getReviewedAt(), recommendation.getReviewReason());
	}
}
