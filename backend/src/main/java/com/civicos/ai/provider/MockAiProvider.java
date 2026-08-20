package com.civicos.ai.provider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.civicos.ai.application.AiRequest;
import com.civicos.ai.application.AiResponse;
import com.civicos.ai.prompt.AiPrompt;

@Component
public class MockAiProvider implements AiProvider {

	@Override
	public String providerId() {
		return "mock";
	}

	@Override
	public AiResponse execute(AiRequest request, AiPrompt prompt, String model) {
		Map<String, Object> result = switch (request.task()) {
			case CLASSIFICATION -> classification();
			case MATCHING_ASSISTANCE -> matching(request.context());
			case EVIDENCE_ANALYSIS -> evidence();
			case CONFLICT_EXPLANATION -> conflictExplanation();
			case RECOMMENDATION_GENERATION -> recommendation(request.context());
			case CASE_SUMMARIZATION -> summary(request.context());
		};
		return new AiResponse(
				result,
				confidence(request),
				List.of("Deterministic mock provider output; human or rule confirmation remains required."),
				List.of("Only the supplied structured context was considered."),
				List.of(request.entityType() + ":" + request.entityId()),
				120,
				80,
				BigDecimal.ZERO);
	}

	private Map<String, Object> classification() {
		return Map.of(
				"category", "ROAD_CUTTING",
				"subCategory", "ACTIVE_EXCAVATION",
				"severity", "MEDIUM",
				"reasoningSummary", "The supplied observation requires road-cutting triage.");
	}

	private Map<String, Object> matching(Map<String, Object> context) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("candidateInterventionId", context.get("candidateInterventionId"));
		result.put("matchReasons", List.of(
				"Candidate was supplied by deterministic spatial and temporal filtering."));
		return result;
	}

	private Map<String, Object> evidence() {
		return Map.of(
				"observations", List.of("Evidence metadata is available for human inspection."),
				"possibleIssues", List.of("Visual condition must be confirmed by an inspector."));
	}

	private Map<String, Object> conflictExplanation() {
		return Map.of(
				"explanation", "The deterministic conflict should be reviewed as a coordination risk.",
				"reasons", List.of("The conflict was supplied by the deterministic conflict engine."),
				"supportingFacts", List.of("CivicOS conflict record"),
				"uncertainties", List.of("Final scheduling constraints require agency confirmation."));
	}

	private Map<String, Object> recommendation(Map<String, Object> context) {
		List<Object> interventions = listValue(context.get("affectedInterventions"));
		List<Object> agencies = listValue(context.get("affectedAgencies"));
		List<Map<String, Object>> sequence = new ArrayList<>();
		for (Object intervention : interventions) {
			sequence.add(Map.of("interventionId", String.valueOf(intervention), "order", sequence.size() + 1));
		}
		return Map.of(
				"recommendedSequence", sequence,
				"recommendation", "Coordinate prerequisite utility work before final road restoration.",
				"reasons", List.of("Sequence remains subject to deterministic dependency validation."),
				"risks", List.of("Agency-confirmed schedules may require revision."),
				"supportingFacts", List.of("Deterministic conflict and supplied intervention references"),
				"uncertainties", List.of("No authoritative schedule change has been made."),
				"affectedInterventions", interventions,
				"affectedAgencies", agencies);
	}

	private Map<String, Object> summary(Map<String, Object> context) {
		return Map.of(
				"summary", "This advisory summary reflects only the bounded CivicOS case context.",
				"keyFacts", listValue(context.get("keyFacts")),
				"pendingActions", listValue(context.get("pendingActions")));
	}

	private BigDecimal confidence(AiRequest request) {
		Object configured = request.context().get("mockConfidence");
		if (configured instanceof Number number) {
			return new BigDecimal(number.toString());
		}
		return new BigDecimal("0.84");
	}

	private List<Object> listValue(Object value) {
		if (value instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		return List.of();
	}
}
