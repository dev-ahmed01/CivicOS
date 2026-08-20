package com.civicos.dependency.application;

import java.util.UUID;

import com.civicos.dependency.domain.Dependency;

public record CreateDependencyCommand(
		UUID sourceInterventionId,
		UUID targetInterventionId,
		Dependency.Type type,
		boolean required,
		String reason) {
}
