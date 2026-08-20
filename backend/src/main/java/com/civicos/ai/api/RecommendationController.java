package com.civicos.ai.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.ai.application.AiRecommendationQueryService;
import com.civicos.ai.application.AiRecommendationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Recommendations")
public class RecommendationController {

	private final AiRecommendationQueryService queryService;

	public RecommendationController(AiRecommendationQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping("/conflicts/{conflictId}/recommendations")
	@Operation(summary = "List advisory recommendations for a conflict")
	public List<AiRecommendationResult> forConflict(@PathVariable UUID conflictId) {
		return queryService.forConflict(conflictId);
	}

	@GetMapping("/recommendations/{recommendationId}")
	@Operation(summary = "Get one advisory recommendation")
	public AiRecommendationResult byId(@PathVariable UUID recommendationId) {
		return queryService.byId(recommendationId);
	}
}
