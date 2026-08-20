package com.civicos.workflow.application;

public class StaleWorkflowVersionException extends RuntimeException {

	public StaleWorkflowVersionException(String entityType, long expectedVersion, long actualVersion) {
		super("%s was modified concurrently (expected version %d, actual version %d)."
				.formatted(entityType, expectedVersion, actualVersion));
	}
}
