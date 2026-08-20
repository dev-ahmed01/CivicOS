package com.civicos.dependency.api;

import java.time.Instant;
import java.util.UUID;

import com.civicos.dependency.domain.Dependency;

public record DependencyResponse(
		UUID id,
		UUID sourceInterventionId,
		UUID targetInterventionId,
		Dependency.Type type,
		boolean required,
		Dependency.Status status,
		String reason,
		long version,
		Instant createdAt) {

	public static DependencyResponse from(Dependency dependency) {
		return new DependencyResponse(
				dependency.getId(), dependency.getSourceIntervention().getId(),
				dependency.getTargetIntervention().getId(), dependency.getType(),
				dependency.isRequired(), dependency.getStatus(), dependency.getReason(),
				dependency.getVersion(), dependency.getCreatedAt());
	}
}
