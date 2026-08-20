package com.civicos.conflict.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.civicos.conflict.domain.Conflict;
import com.civicos.intervention.domain.Intervention;

public record ConflictResponse(
		UUID id,
		String conflictNumber,
		UUID roadSegmentId,
		Conflict.Type type,
		Conflict.Severity severity,
		Conflict.Status status,
		Instant detectedAt,
		Instant resolvedAt,
		String explanation,
		List<UUID> interventionIds,
		long version) {

	public static ConflictResponse from(Conflict conflict) {
		return new ConflictResponse(
				conflict.getId(), conflict.getConflictNumber(), conflict.getRoadSegment().getId(),
				conflict.getType(), conflict.getSeverity(), conflict.getStatus(),
				conflict.getDetectedAt(), conflict.getResolvedAt(), conflict.getExplanation(),
				conflict.getInterventions().stream().map(Intervention::getId).sorted().toList(),
				conflict.getVersion());
	}
}
