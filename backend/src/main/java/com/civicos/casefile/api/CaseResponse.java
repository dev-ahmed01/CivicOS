package com.civicos.casefile.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.civicos.casefile.domain.CitizenObservation;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.intervention.domain.Intervention;

public record CaseResponse(
		UUID id,
		String caseNumber,
		CivicCase.Source source,
		CivicCase.Status status,
		CivicCase.Priority priority,
		UUID roadSegmentId,
		List<UUID> observationIds,
		List<UUID> interventionIds,
		Instant closedAt,
		long version,
		Instant createdAt,
		Instant updatedAt) {

	public static CaseResponse from(
			CivicCase civicCase,
			List<CitizenObservation> observations,
			List<Intervention> interventions) {
		return new CaseResponse(
				civicCase.getId(), civicCase.getCaseNumber(), civicCase.getSource(),
				civicCase.getStatus(), civicCase.getPriority(), civicCase.getRoadSegment().getId(),
				observations.stream().map(CitizenObservation::getId).toList(),
				interventions.stream().map(Intervention::getId).sorted().toList(),
				civicCase.getClosedAt(), civicCase.getVersion(),
				civicCase.getCreatedAt(), civicCase.getUpdatedAt());
	}
}
