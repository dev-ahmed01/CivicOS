package com.civicos.workflow.application;

public class StaleWorkflowVersionException extends com.civicos.common.domain.StaleEntityVersionException {

	public StaleWorkflowVersionException(String entityType, long expectedVersion, long actualVersion) {
		super(entityType, expectedVersion, actualVersion);
	}
}
