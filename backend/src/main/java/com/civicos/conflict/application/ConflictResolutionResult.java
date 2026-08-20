package com.civicos.conflict.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.conflict.domain.Conflict;

public record ConflictResolutionResult(
		UUID conflictId,
		Conflict.Status status,
		long version,
		Instant resolvedAt) {
}
