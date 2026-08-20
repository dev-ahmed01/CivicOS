package com.civicos.verification.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.verification.application.VerificationCommand;
import com.civicos.verification.application.VerificationResult;
import com.civicos.verification.application.VerificationService;
import com.civicos.verification.domain.Verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/inspections/{inspectionId}/verification")
@Tag(name = "Verification")
public class VerificationController {

	private final VerificationService verificationService;
	private final ApiIdempotencyService idempotencyService;

	public VerificationController(
			VerificationService verificationService,
			ApiIdempotencyService idempotencyService) {
		this.verificationService = verificationService;
		this.idempotencyService = idempotencyService;
	}

	@PostMapping
	@Operation(summary = "Record authoritative verification",
			description = "AI output cannot invoke this endpoint or replace the assigned inspector decision.")
	public VerificationResult verify(
			@PathVariable UUID inspectionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody VerificationRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "VERIFICATION:" + inspectionId, body,
				VerificationResult.class, () -> verificationService.verify(body.interventionId(),
						new VerificationCommand(inspectionId, body.result(), body.reason(),
								body.expectedInterventionVersion()),
						CorrelationIdFilter.requestId(request)));
	}

	public record VerificationRequest(
			@NotNull UUID interventionId,
			@NotNull Verification.Result result,
			String reason,
			long expectedInterventionVersion) {
	}
}
