package com.civicos.approval.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.civicos.approval.domain.Approval;
import com.civicos.approval.config.ApprovalPolicyProperties;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.dependency.domain.Dependency;
import com.civicos.dependency.repository.DependencyRepository;
import com.civicos.evidence.domain.Evidence;
import com.civicos.evidence.repository.EvidenceRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.workflow.application.SeparationOfDutiesException;

@Component
public class ApprovalPolicy {

	private static final List<Conflict.Status> BLOCKING_STATES = List.of(
			Conflict.Status.OPEN, Conflict.Status.UNDER_REVIEW);

	private final DependencyRepository dependencyRepository;
	private final ConflictRepository conflictRepository;
	private final EvidenceRepository evidenceRepository;
	private final ApprovalPolicyProperties properties;

	public ApprovalPolicy(
			DependencyRepository dependencyRepository,
			ConflictRepository conflictRepository,
			EvidenceRepository evidenceRepository,
			ApprovalPolicyProperties properties) {
		this.dependencyRepository = dependencyRepository;
		this.conflictRepository = conflictRepository;
		this.evidenceRepository = evidenceRepository;
		this.properties = properties;
	}

	public void validate(
			Intervention intervention,
			UUID actorId,
			Approval.Decision decision) {
		if (intervention.getStatus() != Intervention.Status.APPROVAL_PENDING) {
			throw new DomainConflictException(
					"Approval requires the intervention to be in APPROVAL_PENDING state.");
		}
		if (intervention.getCreatedBy().getId().equals(actorId)) {
			throw new SeparationOfDutiesException();
		}
		if (decision == Approval.Decision.REJECT || decision == Approval.Decision.RETURN) {
			return;
		}
		boolean blockedDependency = dependencyRepository.findAllConnectedTo(List.of(intervention.getId()))
				.stream()
				.anyMatch(dependency -> dependency.isRequired()
						&& dependency.getStatus() == Dependency.Status.BLOCKED);
		if (blockedDependency) {
			throw new DomainConflictException("A required intervention dependency is blocked.");
		}
		if (conflictRepository.existsBlockingConflict(
				intervention.getId(), Conflict.Severity.HIGH, BLOCKING_STATES)) {
			throw new DomainConflictException("An unresolved HIGH conflict blocks approval.");
		}
		List<Evidence> evidence = evidenceRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
				"INTERVENTION", intervention.getId());
		for (String requiredType : properties.getRequiredEvidenceTypes()) {
			boolean accepted = evidence.stream().anyMatch(item ->
					item.getType().name().equalsIgnoreCase(requiredType)
							&& item.getStatus() == Evidence.Status.ACCEPTED);
			if (!accepted) {
				throw new DomainConflictException(
						"Required accepted evidence is missing: " + requiredType);
			}
		}
	}
}
