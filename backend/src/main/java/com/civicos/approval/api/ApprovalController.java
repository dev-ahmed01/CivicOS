package com.civicos.approval.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.approval.application.ApprovalDecisionCommand;
import com.civicos.approval.application.ApprovalResult;
import com.civicos.approval.application.ApprovalService;
import com.civicos.approval.domain.Approval;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Approvals")
public class ApprovalController {

	private final ApprovalService approvalService;
	private final ApiIdempotencyService idempotencyService;

	public ApprovalController(ApprovalService approvalService, ApiIdempotencyService idempotencyService) {
		this.approvalService = approvalService;
		this.idempotencyService = idempotencyService;
	}

	@PostMapping("/interventions/{interventionId}/approval-requests")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Request an authoritative intervention approval")
	public ApprovalResult request(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody ReasonRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "APPROVAL_REQUEST:" + interventionId, body,
				ApprovalResult.class, () -> approvalService.request(interventionId, body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/approval-requests/{approvalId}/decision")
	@Operation(summary = "Record an authoritative approval decision")
	public ApprovalResult decide(
			@PathVariable UUID approvalId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody DecisionRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "APPROVAL_DECISION:" + approvalId, body,
				ApprovalResult.class, () -> approvalService.decide(approvalId,
						new ApprovalDecisionCommand(body.decision(), body.reason(), body.conditions(),
								body.expectedVersion()), CorrelationIdFilter.requestId(request)));
	}

	public record ReasonRequest(@NotBlank String reason) {
	}

	public record DecisionRequest(
			@NotNull Approval.Decision decision,
			String reason,
			List<Map<String, Object>> conditions,
			long expectedVersion) {
	}
}
