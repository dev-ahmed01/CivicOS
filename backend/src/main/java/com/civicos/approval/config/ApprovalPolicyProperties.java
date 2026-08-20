package com.civicos.approval.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "civicos.approval")
public class ApprovalPolicyProperties {

	private List<String> requiredEvidenceTypes = new ArrayList<>();

	public List<String> getRequiredEvidenceTypes() {
		return List.copyOf(requiredEvidenceTypes);
	}

	public void setRequiredEvidenceTypes(List<String> requiredEvidenceTypes) {
		this.requiredEvidenceTypes = requiredEvidenceTypes == null
				? new ArrayList<>()
				: new ArrayList<>(requiredEvidenceTypes);
	}
}
