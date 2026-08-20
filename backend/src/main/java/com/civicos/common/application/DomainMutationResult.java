package com.civicos.common.application;

import java.time.Instant;
import java.util.UUID;

public record DomainMutationResult(
		String entityType,
		UUID entityId,
		String action,
		long version,
		Instant occurredAt) {
}
