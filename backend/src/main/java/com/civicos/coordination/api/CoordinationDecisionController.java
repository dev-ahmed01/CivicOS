package com.civicos.coordination.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.coordination.application.CoordinationDecisionQueryService;
import com.civicos.coordination.application.CoordinationDecisionResult;
import com.civicos.coordination.application.CoordinationDecisionService;
import com.civicos.coordination.domain.CoordinationDecision;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/conflicts/{conflictId}/coordination-decisions")
@Tag(name = "Coordination")
public class CoordinationDecisionController {

	private final CoordinationDecisionQueryService queryService;
	private final CoordinationDecisionService decisionService;
	private final ApiIdempotencyService idempotencyService;

	public CoordinationDecisionController(
			CoordinationDecisionQueryService queryService,
			CoordinationDecisionService decisionService,
			ApiIdempotencyService idempotencyService) {
		this.queryService = queryService;
		this.decisionService = decisionService;
		this.idempotencyService = idempotencyService;
	}

	@GetMapping
	@Operation(summary = "List the human coordination decisions for a conflict")
	public List<CoordinationDecisionResult> list(@PathVariable UUID conflictId) {
		return queryService.forConflict(conflictId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Record a human coordination decision",
			description = "Records the decision without approving work, changing schedules, or resolving the conflict.")
	public CoordinationDecisionResult record(
			@PathVariable UUID conflictId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody DecisionRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(
				idempotencyKey, "COORDINATION_DECISION:" + conflictId, body,
				CoordinationDecisionResult.class,
				() -> decisionService.record(
						conflictId, body.decisionType(), body.decisionText(),
						body.acceptedRecommendationId(), CorrelationIdFilter.requestId(request)));
	}

	public record DecisionRequest(
			@NotNull CoordinationDecision.Type decisionType,
			@NotBlank String decisionText,
			UUID acceptedRecommendationId) {
	}
}
