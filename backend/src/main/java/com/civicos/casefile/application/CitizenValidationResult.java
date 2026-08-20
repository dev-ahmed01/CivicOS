package com.civicos.casefile.application;

import java.time.Instant;
import java.util.UUID;

public record CitizenValidationResult(
		UUID verificationId,
		UUID observationId,
		CitizenValidationDecision decision,
		Instant recordedAt,
		boolean authoritativeWorkflowChanged) {
}
