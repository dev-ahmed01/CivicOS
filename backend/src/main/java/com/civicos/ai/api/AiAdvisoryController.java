package com.civicos.ai.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.ai.application.AiAdvisoryService;
import com.civicos.ai.application.AiExecutionResult;
import com.civicos.ai.application.AiRecommendationGenerationResult;
import com.civicos.ai.application.AiRecommendationResult;
import com.civicos.ai.application.AiRecommendationReviewService;
import com.civicos.ai.application.AiRequest;
import com.civicos.ai.domain.AiRecommendation;
import com.civicos.ai.domain.AiTask;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "Advisory AI", description = "Optional, non-authoritative assistance. Core workflows remain usable when AI is disabled.")
public class AiAdvisoryController {

	private final AiAdvisoryService advisoryService;
	private final AiRecommendationReviewService reviewService;
	private final ApiIdempotencyService idempotencyService;

	public AiAdvisoryController(
			AiAdvisoryService advisoryService,
			AiRecommendationReviewService reviewService,
			ApiIdempotencyService idempotencyService) {
		this.advisoryService = advisoryService;
		this.reviewService = reviewService;
		this.idempotencyService = idempotencyService;
	}

	@PostMapping("/classifications")
	@Operation(summary = "Suggest an observation classification", description = "Advisory only.")
	public AiExecutionResult classify(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody AdvisoryRequest body,
			HttpServletRequest request) {
		return execute(key, "AI_CLASSIFICATION", body, AiTask.CLASSIFICATION, request,
				() -> advisoryService.classify(aiRequest(body, AiTask.CLASSIFICATION, request)));
	}

	@PostMapping("/matching-assistance")
	@Operation(summary = "Suggest possible record matches", description = "Advisory only.")
	public AiExecutionResult matching(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody AdvisoryRequest body,
			HttpServletRequest request) {
		return execute(key, "AI_MATCHING", body, AiTask.MATCHING_ASSISTANCE, request,
				() -> advisoryService.assistMatching(aiRequest(body, AiTask.MATCHING_ASSISTANCE, request)));
	}

	@PostMapping("/evidence-analysis")
	@Operation(summary = "Analyse evidence", description = "Cannot accept, reject, or verify evidence.")
	public AiExecutionResult evidence(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody AdvisoryRequest body,
			HttpServletRequest request) {
		return execute(key, "AI_EVIDENCE", body, AiTask.EVIDENCE_ANALYSIS, request,
				() -> advisoryService.analyseEvidence(aiRequest(body, AiTask.EVIDENCE_ANALYSIS, request)));
	}

	@PostMapping("/conflict-explanations")
	@Operation(summary = "Explain a deterministic conflict", description = "Does not create or resolve conflicts.")
	public AiExecutionResult conflict(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody AdvisoryRequest body,
			HttpServletRequest request) {
		return execute(key, "AI_CONFLICT_EXPLANATION", body, AiTask.CONFLICT_EXPLANATION, request,
				() -> advisoryService.explainConflict(aiRequest(body, AiTask.CONFLICT_EXPLANATION, request)));
	}

	@PostMapping("/case-summaries")
	@Operation(summary = "Summarize a case", description = "Read-only advisory output.")
	public AiExecutionResult caseSummary(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody AdvisoryRequest body,
			HttpServletRequest request) {
		return execute(key, "AI_CASE_SUMMARY", body, AiTask.CASE_SUMMARIZATION, request,
				() -> advisoryService.summarizeCase(aiRequest(body, AiTask.CASE_SUMMARIZATION, request)));
	}

	@PostMapping("/coordination-recommendations")
	@Operation(summary = "Generate a coordination recommendation",
			description = "Always requires an authorised human accept/reject decision and cannot change official workflow state.")
	public AiRecommendationGenerationResult recommend(
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody AdvisoryRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(key, "AI_COORDINATION_RECOMMENDATION", body,
				AiRecommendationGenerationResult.class,
				() -> advisoryService.recommendCoordination(
						aiRequest(body, AiTask.RECOMMENDATION_GENERATION, request)));
	}

	@PostMapping("/recommendations/{recommendationId}/review")
	@Operation(summary = "Accept or reject an advisory recommendation",
			description = "Review records human disposition only; it does not approve or transition an intervention.")
	public AiRecommendationResult review(
			@PathVariable UUID recommendationId,
			@RequestHeader("Idempotency-Key") String key,
			@Valid @RequestBody ReviewRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(key, "AI_RECOMMENDATION_REVIEW:" + recommendationId, body,
				AiRecommendationResult.class,
				() -> reviewService.review(recommendationId, body.decision(), body.reason(),
						CorrelationIdFilter.requestId(request)));
	}

	private AiExecutionResult execute(
			String key,
			String operation,
			AdvisoryRequest body,
			AiTask task,
			HttpServletRequest request,
			java.util.function.Supplier<AiExecutionResult> action) {
		return idempotencyService.execute(key, operation + ":" + task, body,
				AiExecutionResult.class, action);
	}

	private AiRequest aiRequest(AdvisoryRequest body, AiTask task, HttpServletRequest request) {
		return new AiRequest(task, body.entityType(), body.entityId(), body.context(),
				CorrelationIdFilter.requestId(request));
	}

	public record AdvisoryRequest(
			@NotNull String entityType,
			@NotNull UUID entityId,
			Map<String, Object> context) {
	}

	public record ReviewRequest(@NotNull AiRecommendation.ReviewDecision decision, String reason) {
	}
}
