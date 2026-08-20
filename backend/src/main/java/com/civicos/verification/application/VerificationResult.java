package com.civicos.verification.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.verification.domain.Verification;

public record VerificationResult(
		UUID verificationId,
		UUID interventionId,
		UUID inspectionId,
		Verification.Result result,
		String interventionStatus,
		long interventionVersion,
		int citizenFeedbackCount,
		Instant occurredAt) {
}
