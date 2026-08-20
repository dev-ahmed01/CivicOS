package com.civicos.evidence.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.evidence.domain.Evidence;

public record EvidenceResult(
		UUID evidenceId,
		String targetType,
		UUID targetId,
		Evidence.Type type,
		Evidence.Status status,
		String fileReference,
		String checksum,
		long fileSizeBytes,
		long version,
		Instant occurredAt) {
}
