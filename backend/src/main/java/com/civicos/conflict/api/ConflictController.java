package com.civicos.conflict.api;

import java.util.UUID;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;
import com.civicos.conflict.application.ConflictAnalysisResult;
import com.civicos.conflict.application.ConflictDetectionService;
import com.civicos.conflict.application.ConflictQueryService;
import com.civicos.conflict.application.ConflictResolutionResult;
import com.civicos.conflict.application.ConflictResolutionService;
import com.civicos.conflict.domain.Conflict;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Conflicts")
public class ConflictController {

	private final ConflictDetectionService conflictService;
	private final ConflictQueryService queryService;
	private final ConflictResolutionService resolutionService;
	private final ApiIdempotencyService idempotencyService;
	private final PageRequestFactory pageRequestFactory;

	public ConflictController(
			ConflictDetectionService conflictService,
			ConflictQueryService queryService,
			ConflictResolutionService resolutionService,
			ApiIdempotencyService idempotencyService,
			PageRequestFactory pageRequestFactory) {
		this.conflictService = conflictService;
		this.queryService = queryService;
		this.resolutionService = resolutionService;
		this.idempotencyService = idempotencyService;
		this.pageRequestFactory = pageRequestFactory;
	}

	@GetMapping("/conflicts")
	public PagedResponse<ConflictResponse> list(
			@RequestParam(required = false) Conflict.Status status,
			@RequestParam(required = false) Conflict.Severity severity,
			@RequestParam(required = false) UUID roadSegmentId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "detectedAt,desc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(queryService.list(status, severity, roadSegmentId,
				pageRequestFactory.create(page, size, sort,
						Set.of("detectedAt", "severity", "status", "conflictNumber"))),
				CorrelationIdFilter.requestId(request));
	}

	@GetMapping("/conflicts/{conflictId}")
	public ConflictResponse byId(@PathVariable UUID conflictId) {
		return queryService.byId(conflictId);
	}

	@PostMapping("/conflicts/{conflictId}/resolve")
	@Operation(summary = "Resolve or dismiss a conflict authoritatively")
	public ConflictResolutionResult resolve(
			@PathVariable UUID conflictId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ResolutionRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "CONFLICT_RESOLVE:" + conflictId, body,
				ConflictResolutionResult.class, () -> resolutionService.resolve(
						conflictId, body.outcome(), body.expectedVersion(), body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/interventions/{interventionId}/conflict-analysis")
	@Operation(summary = "Run deterministic spatial and temporal conflict analysis")
	public ConflictAnalysisResult analyse(
			@PathVariable UUID interventionId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "CONFLICT_ANALYSIS:" + interventionId,
				new AnalysisRequest(interventionId), ConflictAnalysisResult.class,
				() -> conflictService.analyse(interventionId, CorrelationIdFilter.requestId(request)));
	}

	private record AnalysisRequest(UUID interventionId) {
	}

	public record ResolutionRequest(
			@jakarta.validation.constraints.NotNull Conflict.Status outcome,
			long expectedVersion,
			@jakarta.validation.constraints.NotBlank String reason) {
	}
}
