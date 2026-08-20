package com.civicos.intervention.repository;

import java.util.UUID;

public interface ConflictCandidateProjection {
	UUID getId();
	boolean getSameRoadSegment();
	boolean getSpatialOverlap();
	double getDistanceMeters();
}
