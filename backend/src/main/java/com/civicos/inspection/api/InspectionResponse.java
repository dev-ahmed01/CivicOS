package com.civicos.inspection.api;

import java.time.Instant;
import java.util.UUID;

import com.civicos.inspection.domain.Inspection;
import com.civicos.intervention.domain.Intervention;

public record InspectionResponse(
		UUID id,
		UUID interventionId,
		String interventionNumber,
		String interventionDescription,
		Intervention.Type interventionType,
		Intervention.Status interventionStatus,
		long interventionVersion,
		UUID agencyId,
		String agencyName,
		UUID roadSegmentId,
		String roadSegmentName,
		String geometryWkt,
		Instant plannedStart,
		Instant plannedEnd,
		UUID inspectorId,
		Inspection.Status status,
		Inspection.Result result,
		Instant startedAt,
		Instant completedAt,
		String notes,
		long version,
		Instant createdAt) {

	public static InspectionResponse from(Inspection inspection) {
		Intervention intervention = inspection.getIntervention();
		return new InspectionResponse(
				inspection.getId(), intervention.getId(), intervention.getInterventionNumber(),
				intervention.getDescription(), intervention.getType(), intervention.getStatus(),
				intervention.getVersion(), intervention.getAgency().getId(),
				intervention.getAgency().getName(), intervention.getRoadSegment().getId(),
				intervention.getRoadSegment().getName(), intervention.getGeometry().toText(),
				intervention.getPlannedStart(), intervention.getPlannedEnd(),
				inspection.getInspector().getId(), inspection.getStatus(), inspection.getResult(),
				inspection.getStartedAt(), inspection.getCompletedAt(), inspection.getNotes(),
				inspection.getVersion(), inspection.getCreatedAt());
	}
}
