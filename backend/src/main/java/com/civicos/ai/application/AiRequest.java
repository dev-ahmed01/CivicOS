package com.civicos.ai.application;

import java.util.Map;
import java.util.UUID;

import com.civicos.ai.domain.AiTask;

public record AiRequest(
		AiTask task,
		String entityType,
		UUID entityId,
		Map<String, Object> context,
		String requestId) {

	public AiRequest {
		context = context == null ? Map.of() : Map.copyOf(context);
	}
}
