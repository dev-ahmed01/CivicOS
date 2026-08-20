package com.civicos.workflow.application;

import java.time.Instant;
import java.util.UUID;

public record WorkflowTransitionResult(
		String entityType,
		UUID entityId,
		String action,
		String previousStatus,
		String currentStatus,
		long version,
		Instant transitionedAt) {
}
