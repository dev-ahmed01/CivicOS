package com.civicos.audit.application;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.civicos.ai.repository.AiRecommendationRepository;
import com.civicos.ai.repository.AiRunRepository;
import com.civicos.approval.repository.ApprovalRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.repository.CitizenObservationRepository;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.dependency.repository.DependencyRepository;
import com.civicos.escalation.repository.EscalationRepository;
import com.civicos.evidence.repository.EvidenceRepository;
import com.civicos.inspection.repository.InspectionRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.sla.repository.SlaRepository;
import com.civicos.verification.repository.VerificationRepository;

@Component
public class AuditVisibilityPolicy {

	private final AuthorizationService authorizationService;
	private final InterventionRepository interventionRepository;
	private final ApprovalRepository approvalRepository;
	private final ConflictRepository conflictRepository;
	private final DependencyRepository dependencyRepository;
	private final EvidenceRepository evidenceRepository;
	private final InspectionRepository inspectionRepository;
	private final VerificationRepository verificationRepository;
	private final SlaRepository slaRepository;
	private final EscalationRepository escalationRepository;
	private final CitizenObservationRepository observationRepository;
	private final AiRunRepository aiRunRepository;
	private final AiRecommendationRepository aiRecommendationRepository;

	public AuditVisibilityPolicy(
			AuthorizationService authorizationService,
			InterventionRepository interventionRepository,
			ApprovalRepository approvalRepository,
			ConflictRepository conflictRepository,
			DependencyRepository dependencyRepository,
			EvidenceRepository evidenceRepository,
			InspectionRepository inspectionRepository,
			VerificationRepository verificationRepository,
			SlaRepository slaRepository,
			EscalationRepository escalationRepository,
			CitizenObservationRepository observationRepository,
			AiRunRepository aiRunRepository,
			AiRecommendationRepository aiRecommendationRepository) {
		this.authorizationService = authorizationService;
		this.interventionRepository = interventionRepository;
		this.approvalRepository = approvalRepository;
		this.conflictRepository = conflictRepository;
		this.dependencyRepository = dependencyRepository;
		this.evidenceRepository = evidenceRepository;
		this.inspectionRepository = inspectionRepository;
		this.verificationRepository = verificationRepository;
		this.slaRepository = slaRepository;
		this.escalationRepository = escalationRepository;
		this.observationRepository = observationRepository;
		this.aiRunRepository = aiRunRepository;
		this.aiRecommendationRepository = aiRecommendationRepository;
	}

	public void assertVisible(AuditEvent event, CivicPrincipal principal) {
		if (principal.roles().contains(SystemRole.ADMIN.name())
				|| event.getActor() != null && event.getActor().getId().equals(principal.userId())
				|| canView(event.getEntityType(), event.getEntityId(), principal)) {
			return;
		}
		throw new AccessDeniedException("The audit event is outside the actor's permitted scope.");
	}

	private boolean canView(String entityType, UUID entityId, CivicPrincipal principal) {
		return switch (entityType) {
			case "USER" -> entityId.equals(principal.userId());
			case "INTERVENTION" -> interventionRepository.findById(entityId)
					.map(intervention -> canView(intervention, principal)).orElse(false);
			case "APPROVAL" -> approvalRepository.findById(entityId)
					.map(approval -> canView(approval.getIntervention(), principal)).orElse(false);
			case "CONFLICT" -> conflictRepository.findById(entityId)
					.map(conflict -> conflict.getInterventions().stream()
							.anyMatch(intervention -> canView(intervention, principal))).orElse(false);
			case "DEPENDENCY" -> dependencyRepository.findById(entityId)
					.map(dependency -> canView(dependency.getSourceIntervention(), principal)
							|| canView(dependency.getTargetIntervention(), principal)).orElse(false);
			case "EVIDENCE" -> evidenceRepository.findById(entityId)
					.map(evidence -> evidence.getUploadedBy().getId().equals(principal.userId())
							|| canViewTarget(evidence.getTargetType(), evidence.getTargetId(), principal))
					.orElse(false);
			case "INSPECTION" -> inspectionRepository.findById(entityId)
					.map(inspection -> inspection.getInspector().getId().equals(principal.userId())
							|| canView(inspection.getIntervention(), principal)).orElse(false);
			case "VERIFICATION" -> verificationRepository.findById(entityId)
					.map(verification -> verification.getSubmittedBy() != null
							&& verification.getSubmittedBy().getId().equals(principal.userId())
							|| canViewTarget(verification.getTargetType(), verification.getTargetId(), principal))
					.orElse(false);
			case "SLA" -> slaRepository.findById(entityId)
					.map(sla -> canViewTarget(sla.getTargetType(), sla.getTargetId(), principal))
					.orElse(false);
			case "ESCALATION" -> escalationRepository.findById(entityId)
					.map(escalation -> canViewTarget(
							escalation.getSla().getTargetType(), escalation.getSla().getTargetId(), principal))
					.orElse(false);
			case "CIVIC_CASE" -> canViewCase(entityId, principal);
			case "AI_RUN" -> aiRunRepository.findById(entityId)
					.map(run -> run.getRequestedBy().getId().equals(principal.userId())
							|| canViewTarget(run.getEntityType(), run.getEntityId(), principal))
					.orElse(false);
			case "AI_RECOMMENDATION" -> aiRecommendationRepository.findById(entityId)
					.map(recommendation -> recommendation.getConflict().getInterventions().stream()
							.anyMatch(intervention -> canView(intervention, principal)))
					.orElse(false);
			case "ROAD", "ROAD_SEGMENT" -> principal.roles().contains(SystemRole.COORDINATOR.name());
			default -> false;
		};
	}

	private boolean canViewTarget(String targetType, UUID targetId, CivicPrincipal principal) {
		return switch (targetType) {
			case "INTERVENTION" -> interventionRepository.findById(targetId)
					.map(intervention -> canView(intervention, principal)).orElse(false);
			case "CITIZEN_OBSERVATION" -> observationRepository.findById(targetId)
					.map(observation -> observation.getSubmittedBy().getId().equals(principal.userId()))
					.orElse(false);
			case "CIVIC_CASE" -> canViewCase(targetId, principal);
			case "CONFLICT" -> conflictRepository.findById(targetId)
					.map(conflict -> conflict.getInterventions().stream()
							.anyMatch(intervention -> canView(intervention, principal)))
					.orElse(false);
			default -> false;
		};
	}

	private boolean canView(Intervention intervention, CivicPrincipal principal) {
		if (authorizationService.canAccessAgency(intervention.getAgency().getId())) {
			return true;
		}
		return principal.roles().contains(SystemRole.INSPECTOR.name())
				&& inspectionRepository.existsByInterventionIdAndInspectorId(
						intervention.getId(), principal.userId());
	}

	private boolean canViewCase(UUID caseId, CivicPrincipal principal) {
		if (principal.roles().contains(SystemRole.COORDINATOR.name())) {
			return true;
		}
		if (principal.agencyId() != null
				&& interventionRepository.existsByCivicCaseIdAndAgencyId(caseId, principal.agencyId())) {
			return true;
		}
		return observationRepository.existsByCivicCaseIdAndSubmittedById(caseId, principal.userId());
	}
}
