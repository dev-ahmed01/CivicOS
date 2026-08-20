package com.civicos.verification.application;

import java.util.UUID;

import com.civicos.verification.domain.Verification;

public record VerificationCommand(
		UUID inspectionId,
		Verification.Result result,
		String reason,
		long expectedInterventionVersion) {
}
