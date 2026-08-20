package com.civicos.conflict.application;

import java.util.List;
import java.util.UUID;

import com.civicos.conflict.domain.Conflict;

public record ConflictAnalysisResult(
		UUID interventionId,
		Outcome outcome,
		List<DetectedConflict> conflicts) {

	public enum Outcome { NO_CONFLICT, CONFLICTS_DETECTED }

	public record DetectedConflict(
			UUID conflictId,
			String conflictNumber,
			Conflict.Type type,
			Conflict.Severity severity,
			Conflict.Status status,
			List<UUID> affectedInterventionIds,
			PersistenceAction persistenceAction) {
	}

	public enum PersistenceAction { CREATED, UPDATED, UNCHANGED, HUMAN_DECISION_PRESERVED }

	public ConflictAnalysisResult {
		conflicts = List.copyOf(conflicts);
	}
}
