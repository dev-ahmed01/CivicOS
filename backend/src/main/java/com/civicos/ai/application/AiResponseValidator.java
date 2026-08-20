package com.civicos.ai.application;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.civicos.ai.domain.AiConfidenceBand;
import com.civicos.ai.domain.AiTask;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AiResponseValidator {

	private static final Set<String> CLASSIFICATION_CATEGORIES = Set.of(
			"ROAD_CUTTING", "ROAD_DAMAGE", "UNRESTORED_EXCAVATION", "DRAINAGE_WORK",
			"UTILITY_WORK", "TRAFFIC_OBSTRUCTION", "DUPLICATE_REPORT", "OTHER");
	private static final Set<String> FORBIDDEN_ACTION_FIELDS = Set.of(
			"approved", "rejected", "statusChange", "newStatus", "closeCase",
			"slaOverride", "execute", "authorizationGranted");

	private final ObjectMapper objectMapper;

	public AiResponseValidator(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void validate(AiTask task, AiResponse response) {
		if (response == null) {
			throw invalid("AI response is absent.");
		}
		AiConfidenceBand.from(response.confidence());
		if (response.inputTokens() < 0 || response.outputTokens() < 0
				|| response.estimatedCost() == null
				|| response.estimatedCost().compareTo(BigDecimal.ZERO) < 0) {
			throw invalid("AI usage metrics cannot be negative.");
		}
		Map<String, Object> result = response.result();
		if (result == null || result.isEmpty()) {
			throw invalid("AI result is empty.");
		}
		assertNoAuthoritativeAction(result);
		objectMapper.valueToTree(response.structuredOutput());

		switch (task) {
			case CLASSIFICATION -> validateClassification(result);
			case MATCHING_ASSISTANCE -> {
				requireKey(result, "candidateInterventionId");
				requireCollection(result, "matchReasons");
			}
			case EVIDENCE_ANALYSIS -> {
				requireCollection(result, "observations");
				requireCollection(result, "possibleIssues");
			}
			case CONFLICT_EXPLANATION -> {
				requireText(result, "explanation");
				requireCollection(result, "reasons");
				requireCollection(result, "supportingFacts");
				requireCollection(result, "uncertainties");
			}
			case RECOMMENDATION_GENERATION -> {
				requireText(result, "recommendation");
				requireCollection(result, "recommendedSequence");
				requireCollection(result, "reasons");
				requireCollection(result, "risks");
				requireCollection(result, "supportingFacts");
				requireCollection(result, "uncertainties");
				requireCollection(result, "affectedInterventions");
				requireCollection(result, "affectedAgencies");
			}
			case CASE_SUMMARIZATION -> {
				requireText(result, "summary");
				requireCollection(result, "keyFacts");
				requireCollection(result, "pendingActions");
			}
		}
	}

	private void validateClassification(Map<String, Object> result) {
		String category = requireText(result, "category");
		if (!CLASSIFICATION_CATEGORIES.contains(category)) {
			throw invalid("AI classification category is outside the configured MVP taxonomy.");
		}
		requireText(result, "subCategory");
		String severity = requireText(result, "severity");
		if (!Set.of("LOW", "MEDIUM", "HIGH").contains(severity)) {
			throw invalid("AI classification severity is invalid.");
		}
		requireText(result, "reasoningSummary");
	}

	private String requireText(Map<String, Object> result, String field) {
		Object value = result.get(field);
		if (!(value instanceof String text) || text.isBlank() || text.length() > 4000) {
			throw invalid("AI response field must be bounded non-empty text: " + field + ".");
		}
		return text;
	}

	private void requireCollection(Map<String, Object> result, String field) {
		Object value = result.get(field);
		if (!(value instanceof Collection<?> collection) || collection.size() > 100) {
			throw invalid("AI response field must be a bounded collection: " + field + ".");
		}
	}

	private void requireKey(Map<String, Object> result, String field) {
		if (!result.containsKey(field)) {
			throw invalid("AI response field is required: " + field + ".");
		}
	}

	private void assertNoAuthoritativeAction(Object value) {
		if (value instanceof Map<?, ?> result) {
			for (Map.Entry<?, ?> entry : result.entrySet()) {
				String key = String.valueOf(entry.getKey());
				if (FORBIDDEN_ACTION_FIELDS.stream().anyMatch(field -> field.equalsIgnoreCase(key))) {
					throw invalid("AI response attempted to express an authoritative action.");
				}
				assertNoAuthoritativeAction(entry.getValue());
			}
		} else if (value instanceof Collection<?> collection) {
			collection.forEach(this::assertNoAuthoritativeAction);
		}
	}

	private AiInvalidResponseException invalid(String message) {
		return new AiInvalidResponseException(message);
	}
}
