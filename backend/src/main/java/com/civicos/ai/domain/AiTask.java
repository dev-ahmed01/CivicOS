package com.civicos.ai.domain;

import com.civicos.auth.domain.PermissionCode;

public enum AiTask {
	CLASSIFICATION("classification", PermissionCode.OBSERVATION_TRIAGE),
	MATCHING_ASSISTANCE("matching", PermissionCode.OBSERVATION_MATCH),
	EVIDENCE_ANALYSIS("evidence", PermissionCode.EVIDENCE_REVIEW),
	CONFLICT_EXPLANATION("conflict", PermissionCode.CONFLICT_VIEW),
	RECOMMENDATION_GENERATION("recommendation", PermissionCode.COORDINATION_UPDATE),
	CASE_SUMMARIZATION("summary", PermissionCode.OBSERVATION_VIEW_RELEVANT);

	private final String resourceDirectory;
	private final PermissionCode permission;

	AiTask(String resourceDirectory, PermissionCode permission) {
		this.resourceDirectory = resourceDirectory;
		this.permission = permission;
	}

	public String resourceDirectory() {
		return resourceDirectory;
	}

	public PermissionCode permission() {
		return permission;
	}
}
