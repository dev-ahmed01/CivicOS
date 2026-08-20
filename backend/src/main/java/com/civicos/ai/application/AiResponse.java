package com.civicos.ai.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AiResponse(
		Map<String, Object> result,
		BigDecimal confidence,
		List<String> warnings,
		List<String> assumptions,
		List<String> sourceReferences,
		long inputTokens,
		long outputTokens,
		BigDecimal estimatedCost) {

	public AiResponse {
		result = result == null
				? Map.of()
				: Collections.unmodifiableMap(new LinkedHashMap<>(result));
		warnings = warnings == null ? List.of() : List.copyOf(warnings);
		assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
		sourceReferences = sourceReferences == null ? List.of() : List.copyOf(sourceReferences);
		estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
	}

	public Map<String, Object> structuredOutput() {
		return Map.of(
				"result", result,
				"confidence", confidence,
				"warnings", warnings,
				"assumptions", assumptions,
				"sourceReferences", sourceReferences);
	}
}
