package com.civicos.intervention.api;

import java.time.Instant;
import java.util.UUID;

import com.civicos.intervention.domain.Intervention;

public record InterventionResponse(
		UUID id,
		String interventionNumber,
		UUID caseId,
		UUID agencyId,
		Intervention.Type type,
		String description,
		UUID roadSegmentId,
		String geometryWkt,
		Instant plannedStart,
		Instant plannedEnd,
		Instant actualStart,
		Instant actualEnd,
		Intervention.Status status,
		Intervention.Status heldFromStatus,
		Intervention.Priority priority,
		UUID createdBy,
		long version,
		Instant createdAt,
		Instant updatedAt) {

	public static InterventionResponse from(Intervention intervention) {
		return new InterventionResponse(
				intervention.getId(), intervention.getInterventionNumber(),
				intervention.getCivicCase().getId(), intervention.getAgency().getId(),
				intervention.getType(), intervention.getDescription(),
				intervention.getRoadSegment().getId(), intervention.getGeometry().toText(),
				intervention.getPlannedStart(), intervention.getPlannedEnd(),
				intervention.getActualStart(), intervention.getActualEnd(),
				intervention.getStatus(), intervention.getHeldFromStatus(), intervention.getPriority(),
				intervention.getCreatedBy().getId(), intervention.getVersion(),
				intervention.getCreatedAt(), intervention.getUpdatedAt());
	}
}
