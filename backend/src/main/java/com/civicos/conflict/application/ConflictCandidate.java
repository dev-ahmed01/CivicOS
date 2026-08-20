package com.civicos.conflict.application;

import com.civicos.intervention.domain.Intervention;

public record ConflictCandidate(
		Intervention intervention,
		boolean sameRoadSegment,
		boolean spatialOverlap,
		double distanceMeters) {
}
