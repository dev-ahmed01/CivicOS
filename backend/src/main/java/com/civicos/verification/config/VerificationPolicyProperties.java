package com.civicos.verification.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.civicos.evidence.domain.Evidence;

import jakarta.validation.constraints.NotEmpty;

@Component
@Validated
@ConfigurationProperties(prefix = "civicos.verification")
public class VerificationPolicyProperties {

	@NotEmpty
	private Set<Evidence.Type> requiredEvidenceTypes = new LinkedHashSet<>(Set.of(
			Evidence.Type.COMPLETION,
			Evidence.Type.RESTORATION));

	public Set<Evidence.Type> getRequiredEvidenceTypes() {
		return Set.copyOf(requiredEvidenceTypes);
	}

	public void setRequiredEvidenceTypes(Set<Evidence.Type> requiredEvidenceTypes) {
		this.requiredEvidenceTypes = new LinkedHashSet<>(requiredEvidenceTypes);
	}
}
