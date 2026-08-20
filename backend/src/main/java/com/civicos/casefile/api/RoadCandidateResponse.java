package com.civicos.casefile.api;

import java.util.UUID;

import com.civicos.road.domain.RoadSegment;

public record RoadCandidateResponse(
		UUID roadSegmentId,
		String externalReference,
		String name,
		String classification,
		String surfaceType) {

	public static RoadCandidateResponse from(RoadSegment roadSegment) {
		return new RoadCandidateResponse(
				roadSegment.getId(), roadSegment.getExternalReference(), roadSegment.getName(),
				roadSegment.getClassification().name(), roadSegment.getSurfaceType().name());
	}
}
