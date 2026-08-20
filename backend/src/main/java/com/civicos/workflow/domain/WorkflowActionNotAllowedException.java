package com.civicos.workflow.domain;

public class WorkflowActionNotAllowedException extends RuntimeException {

	public WorkflowActionNotAllowedException(String entityType, String currentState, String action) {
		super("Action %s is not allowed for %s in state %s."
				.formatted(action, entityType, currentState));
	}
}
