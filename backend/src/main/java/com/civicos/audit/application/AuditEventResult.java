package com.civicos.audit.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResult(
		UUID eventId,
		UUID actorId,
		String action,
		String entityType,
		UUID entityId,
		Instant occurredAt,
		Map<String, Object> beforeState,
		Map<String, Object> afterState,
		String reason,
		UUID requestId,
		Map<String, Object> metadata) {
}
