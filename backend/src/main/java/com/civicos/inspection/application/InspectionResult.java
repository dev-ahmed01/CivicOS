package com.civicos.inspection.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.inspection.domain.Inspection;

public record InspectionResult(
		UUID inspectionId,
		UUID interventionId,
		UUID inspectorId,
		Inspection.Status status,
		Inspection.Result result,
		long version,
		Instant occurredAt) {
}
