package com.civicos.intervention.application;

import java.time.Instant;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;

import com.civicos.intervention.domain.Intervention;

public record CreateInterventionCommand(
		String interventionNumber,
		UUID caseId,
		UUID agencyId,
		Intervention.Type type,
		String description,
		UUID roadSegmentId,
		Geometry geometry,
		Instant plannedStart,
		Instant plannedEnd,
		Intervention.Priority priority) {
}
