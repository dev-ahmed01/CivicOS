package com.civicos.coordination.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.coordination.domain.CoordinationDecision;

public record CoordinationDecisionResult(
		UUID decisionId,
		UUID conflictId,
		UUID coordinatorId,
		CoordinationDecision.Type decisionType,
		String decisionText,
		UUID acceptedRecommendationId,
		Instant createdAt) {

	public static CoordinationDecisionResult from(CoordinationDecision decision) {
		return new CoordinationDecisionResult(
				decision.getId(), decision.getConflict().getId(), decision.getCoordinator().getId(),
				decision.getDecisionType(), decision.getDecisionText(),
				decision.getAcceptedRecommendation() == null
						? null
						: decision.getAcceptedRecommendation().getId(),
				decision.getCreatedAt());
	}
}
