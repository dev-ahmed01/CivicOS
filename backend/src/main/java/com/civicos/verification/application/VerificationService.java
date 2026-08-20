package com.civicos.verification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.evidence.domain.Evidence;
import com.civicos.evidence.repository.EvidenceRepository;
import com.civicos.inspection.domain.Inspection;
import com.civicos.inspection.repository.InspectionRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;
import com.civicos.verification.config.VerificationPolicyProperties;
import com.civicos.verification.domain.Verification;
import com.civicos.verification.repository.VerificationRepository;
import com.civicos.workflow.application.InterventionWorkflowService;
import com.civicos.workflow.application.SeparationOfDutiesException;
import com.civicos.workflow.application.WorkflowTransitionResult;

@Service
public class VerificationService {
	private static final List<Evidence.Type> WORK_EVIDENCE_TYPES = List.of(
			Evidence.Type.BEFORE_WORK,
			Evidence.Type.DURING_WORK,
			Evidence.Type.COMPLETION,
			Evidence.Type.RESTORATION);

	private final VerificationRepository verificationRepository;
	private final InspectionRepository inspectionRepository;
	private final InterventionRepository interventionRepository;
	private final EvidenceRepository evidenceRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final AuthorizationService authorizationService;
	private final VerificationPolicyProperties policy;
	private final InterventionWorkflowService workflowService;
	private final Clock clock;

	public VerificationService(
			VerificationRepository verificationRepository,
			InspectionRepository inspectionRepository,
			InterventionRepository interventionRepository,
			EvidenceRepository evidenceRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			AuthorizationService authorizationService,
			VerificationPolicyProperties policy,
			InterventionWorkflowService workflowService,
			Clock clock) {
		this.verificationRepository = verificationRepository;
		this.inspectionRepository = inspectionRepository;
		this.interventionRepository = interventionRepository;
		this.evidenceRepository = evidenceRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
		this.policy = policy;
		this.workflowService = workflowService;
		this.clock = clock;
	}

	@Transactional
	public VerificationResult verify(
			UUID interventionId, VerificationCommand command, String requestId) {
		ValidationRules.required(command, "Verification command");
		ValidationRules.required(command.inspectionId(), "Inspection");
		ValidationRules.required(command.result(), "Verification result");
		if (command.result() != Verification.Result.PASSED
				&& command.result() != Verification.Result.FAILED
				&& command.result() != Verification.Result.CONDITIONAL) {
			throw new DomainValidationException(
					"Official verification result must be PASSED, FAILED, or CONDITIONAL.");
		}
		authorizationService.authorize(permission(command.result()));
		CivicPrincipal principal = authorizationService.currentPrincipal();
		assertInspector(principal);

		Intervention intervention = interventionRepository.findById(interventionId)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + interventionId));
		if (intervention.getStatus() != Intervention.Status.COMPLETED_PENDING_VERIFICATION) {
			throw new DomainConflictException(
					"Authoritative verification requires an intervention pending verification.");
		}
		if (intervention.getCreatedBy().getId().equals(principal.userId())) {
			throw new SeparationOfDutiesException();
		}
		if (evidenceRepository.existsByTargetTypeAndTargetIdAndUploadedByIdAndTypeIn(
				"INTERVENTION", interventionId, principal.userId(), WORK_EVIDENCE_TYPES)) {
			throw new SeparationOfDutiesException();
		}
		Inspection inspection = inspectionRepository.findById(command.inspectionId())
				.orElseThrow(() -> new NoSuchElementException(
						"Inspection not found: " + command.inspectionId()));
		validateInspection(intervention, inspection, principal, command.result());
		validateReason(command.result(), command.reason());
		validateRequiredEvidence(interventionId);

		User inspector = actor(principal);
		Verification verification = Verification.fieldInspector(
				interventionId, command.result(), inspector, command.reason());
		verificationRepository.saveAndFlush(verification);
		int citizenFeedbackCount = citizenFeedbackCount(interventionId);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				inspector, "VERIFICATION_RECORDED", "VERIFICATION", verification.getId(),
				null, state(verification, inspection.getId(), citizenFeedbackCount),
				command.reason(), requestId));

		WorkflowTransitionResult transition = transition(interventionId, command, requestId);
		String status = transition == null ? intervention.getStatus().name() : transition.currentStatus();
		long version = transition == null ? intervention.getVersion() : transition.version();
		return new VerificationResult(
				verification.getId(), interventionId, inspection.getId(), verification.getResult(),
				status, version, citizenFeedbackCount, occurredAt);
	}

	private WorkflowTransitionResult transition(
			UUID interventionId, VerificationCommand command, String requestId) {
		return switch (command.result()) {
			case PASSED, CONFIRMED -> workflowService.transition(
					interventionId, Intervention.WorkflowAction.VERIFY,
					command.expectedInterventionVersion(), command.reason(), requestId);
			case FAILED, DISPUTED, CANNOT_VERIFY -> workflowService.transition(
					interventionId, Intervention.WorkflowAction.FAIL_VERIFICATION,
					command.expectedInterventionVersion(), command.reason(), requestId);
			case CONDITIONAL -> null;
		};
	}

	private void validateInspection(
			Intervention intervention,
			Inspection inspection,
			CivicPrincipal principal,
			Verification.Result result) {
		if (!inspection.getIntervention().getId().equals(intervention.getId())) {
			throw new DomainValidationException("Inspection does not belong to the intervention.");
		}
		if (!inspection.getInspector().getId().equals(principal.userId())) {
			throw new AccessDeniedException("Only the assigned inspector can verify this inspection.");
		}
		if (inspection.getStatus() != Inspection.Status.COMPLETED || inspection.getResult() == null) {
			throw new DomainConflictException("A completed inspection is required for verification.");
		}
		boolean compatible = switch (result) {
			case PASSED, CONFIRMED -> inspection.getResult() == Inspection.Result.PASSED;
			case FAILED, DISPUTED, CANNOT_VERIFY -> inspection.getResult() == Inspection.Result.FAILED;
			case CONDITIONAL -> inspection.getResult() == Inspection.Result.CONDITIONAL;
		};
		if (!compatible) {
			throw new DomainConflictException(
					"Verification result must agree with the authoritative inspection result.");
		}
	}

	private void validateRequiredEvidence(UUID interventionId) {
		List<Evidence.Type> missing = policy.getRequiredEvidenceTypes().stream()
				.filter(type -> !evidenceRepository.existsByTargetTypeAndTargetIdAndTypeAndStatus(
						"INTERVENTION", interventionId, type, Evidence.Status.ACCEPTED))
				.sorted()
				.toList();
		if (!missing.isEmpty()) {
			throw new DomainConflictException("Required accepted evidence is missing: " + missing);
		}
	}

	private int citizenFeedbackCount(UUID interventionId) {
		return (int) verificationRepository
				.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("INTERVENTION", interventionId)
				.stream()
				.filter(item -> item.getSource() == Verification.Source.CITIZEN)
				.count();
	}

	private PermissionCode permission(Verification.Result result) {
		return switch (result) {
			case PASSED, CONFIRMED -> PermissionCode.VERIFICATION_PASS;
			case FAILED, CONDITIONAL, DISPUTED, CANNOT_VERIFY -> PermissionCode.VERIFICATION_FAIL;
		};
	}

	private void validateReason(Verification.Result result, String reason) {
		if (result != Verification.Result.PASSED && result != Verification.Result.CONFIRMED
				&& (reason == null || reason.isBlank())) {
			throw new DomainValidationException("A reason is required for a non-passing verification.");
		}
	}

	private void assertInspector(CivicPrincipal principal) {
		boolean inspector = principal.roles().contains(SystemRole.INSPECTOR.name());
		boolean adminOverride = principal.roles().contains(SystemRole.ADMIN.name())
				&& principal.permissions().contains(PermissionCode.ADMIN_OVERRIDE.name());
		if (!inspector && !adminOverride) {
			throw new AccessDeniedException("Only an inspector can record authoritative verification.");
		}
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
	}

	private Map<String, Object> state(
			Verification verification, UUID inspectionId, int citizenFeedbackCount) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("targetType", verification.getTargetType());
		state.put("targetId", verification.getTargetId().toString());
		state.put("source", verification.getSource().name());
		state.put("result", verification.getResult().name());
		state.put("submittedBy", verification.getSubmittedBy().getId().toString());
		state.put("inspectionId", inspectionId.toString());
		state.put("citizenFeedbackCount", citizenFeedbackCount);
		return state;
	}
}
