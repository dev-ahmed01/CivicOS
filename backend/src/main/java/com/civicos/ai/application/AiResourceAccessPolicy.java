package com.civicos.ai.application;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.repository.CitizenObservationRepository;
import com.civicos.casefile.repository.CivicCaseRepository;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.evidence.repository.EvidenceRepository;
import com.civicos.inspection.repository.InspectionRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;

@Component
public class AiResourceAccessPolicy {

	private final AuthorizationService authorizationService;
	private final CitizenObservationRepository observationRepository;
	private final CivicCaseRepository caseRepository;
	private final ConflictRepository conflictRepository;
	private final EvidenceRepository evidenceRepository;
	private final InterventionRepository interventionRepository;
	private final InspectionRepository inspectionRepository;

	public AiResourceAccessPolicy(
			AuthorizationService authorizationService,
			CitizenObservationRepository observationRepository,
			CivicCaseRepository caseRepository,
			ConflictRepository conflictRepository,
			EvidenceRepository evidenceRepository,
			InterventionRepository interventionRepository,
			InspectionRepository inspectionRepository) {
		this.authorizationService = authorizationService;
		this.observationRepository = observationRepository;
		this.caseRepository = caseRepository;
		this.conflictRepository = conflictRepository;
		this.evidenceRepository = evidenceRepository;
		this.interventionRepository = interventionRepository;
		this.inspectionRepository = inspectionRepository;
	}

	public void assertCanAnalyze(AiRequest request, CivicPrincipal principal) {
		boolean visible = switch (request.task()) {
			case CLASSIFICATION, MATCHING_ASSISTANCE -> observation(request.entityType(), request.entityId(), principal);
			case EVIDENCE_ANALYSIS -> evidence(request.entityType(), request.entityId(), principal);
			case CONFLICT_EXPLANATION, RECOMMENDATION_GENERATION -> conflict(
					request.entityType(), request.entityId(), principal);
			case CASE_SUMMARIZATION -> civicCase(request.entityType(), request.entityId(), principal);
		};
		if (!visible) {
			throw new AccessDeniedException("The AI target is outside the actor's permitted scope.");
		}
	}

	private boolean observation(String entityType, UUID entityId, CivicPrincipal principal) {
		assertType(entityType, "CITIZEN_OBSERVATION");
		return observationRepository.findById(entityId)
				.map(observation -> observation.getSubmittedBy().getId().equals(principal.userId())
						|| canViewCase(observation.getCivicCase().getId(), principal))
				.orElseThrow(() -> new DomainValidationException("Citizen observation does not exist."));
	}

	private boolean evidence(String entityType, UUID entityId, CivicPrincipal principal) {
		assertType(entityType, "EVIDENCE");
		return evidenceRepository.findById(entityId)
				.map(evidence -> evidence.getUploadedBy().getId().equals(principal.userId())
						|| canViewTarget(evidence.getTargetType(), evidence.getTargetId(), principal))
				.orElseThrow(() -> new DomainValidationException("Evidence does not exist."));
	}

	private boolean conflict(String entityType, UUID entityId, CivicPrincipal principal) {
		assertType(entityType, "CONFLICT");
		if (!conflictRepository.existsById(entityId)) {
			throw new DomainValidationException("Conflict does not exist.");
		}
		if (principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())) {
			return true;
		}
		if (principal.agencyId() != null
				&& conflictRepository.existsByIdAndInterventionAgencyId(entityId, principal.agencyId())) {
			return true;
		}
		return principal.roles().contains(SystemRole.INSPECTOR.name())
				&& conflictRepository.existsByIdAndAssignedInspectorId(entityId, principal.userId());
	}

	private boolean civicCase(String entityType, UUID entityId, CivicPrincipal principal) {
		assertType(entityType, "CIVIC_CASE");
		if (!caseRepository.existsById(entityId)) {
			throw new DomainValidationException("Civic case does not exist.");
		}
		return canViewCase(entityId, principal);
	}

	private boolean canViewTarget(String targetType, UUID targetId, CivicPrincipal principal) {
		return switch (targetType) {
			case "INTERVENTION" -> interventionRepository.findById(targetId)
					.map(intervention -> canView(intervention, principal)).orElse(false);
			case "CITIZEN_OBSERVATION" -> observationRepository.findById(targetId)
					.map(observation -> observation.getSubmittedBy().getId().equals(principal.userId())
							|| canViewCase(observation.getCivicCase().getId(), principal))
					.orElse(false);
			case "CIVIC_CASE" -> canViewCase(targetId, principal);
			default -> false;
		};
	}

	private boolean canView(Intervention intervention, CivicPrincipal principal) {
		return authorizationService.canAccessAgency(intervention.getAgency().getId())
				|| principal.roles().contains(SystemRole.INSPECTOR.name())
				&& inspectionRepository.existsByInterventionIdAndInspectorId(
						intervention.getId(), principal.userId());
	}

	private boolean canViewCase(UUID caseId, CivicPrincipal principal) {
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())
				|| principal.agencyId() != null
				&& interventionRepository.existsByCivicCaseIdAndAgencyId(caseId, principal.agencyId())
				|| observationRepository.existsByCivicCaseIdAndSubmittedById(caseId, principal.userId());
	}

	private void assertType(String actual, String expected) {
		if (!expected.equals(actual)) {
			throw new DomainValidationException("AI task requires entity type " + expected + ".");
		}
	}
}
