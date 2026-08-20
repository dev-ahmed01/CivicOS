package com.civicos.conflict.application;

import java.util.Set;

import com.civicos.conflict.domain.Conflict;
import com.civicos.intervention.domain.Intervention;
import com.civicos.road.domain.RoadSegment;

public record ConflictFinding(
		String scopeKey,
		RoadSegment roadSegment,
		Conflict.Type type,
		Conflict.Severity severity,
		Set<Intervention> interventions,
		String explanation) {

	public ConflictFinding {
		interventions = Set.copyOf(interventions);
	}
}
