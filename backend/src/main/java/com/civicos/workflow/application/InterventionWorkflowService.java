package com.civicos.workflow.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.approval.domain.Approval;
import com.civicos.approval.repository.ApprovalRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;
import com.civicos.verification.domain.Verification;
import com.civicos.verification.repository.VerificationRepository;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;

@Service
public class InterventionWorkflowService {

	private static final List<Approval.Status> AUTHORITATIVE_APPROVALS = List.of(
			Approval.Status.APPROVED,
			Approval.Status.APPROVED_WITH_CONDITIONS);
	private static final List<Verification.Result> SUCCESSFUL_VERIFICATIONS = List.of(
			Verification.Result.PASSED,
			Verification.Result.CONFIRMED);
	private static final List<Verification.Result> FAILED_VERIFICATIONS = List.of(
			Verification.Result.FAILED,
			Verification.Result.DISPUTED,
			Verification.Result.CANNOT_VERIFY);
	private static final List<Conflict.Status> BLOCKING_CONFLICT_STATES = List.of(
			Conflict.Status.OPEN,
			Conflict.Status.UNDER_REVIEW);

	private final InterventionRepository interventionRepository;
	private final ApprovalRepository approvalRepository;
	private final VerificationRepository verificationRepository;
	private final ConflictRepository conflictRepository;
	private final AuditEventRepository auditEventRepository;
	private final UserRepository userRepository;
	private final WorkflowAuthorizationPolicy authorizationPolicy;
	private final Clock clock;

	public InterventionWorkflowService(
			InterventionRepository interventionRepository,
			ApprovalRepository approvalRepository,
			VerificationRepository verificationRepository,
			ConflictRepository conflictRepository,
			AuditEventRepository auditEventRepository,
			UserRepository userRepository,
			WorkflowAuthorizationPolicy authorizationPolicy,
			Clock clock) {
		this.interventionRepository = interventionRepository;
		this.approvalRepository = approvalRepository;
		this.verificationRepository = verificationRepository;
		this.conflictRepository = conflictRepository;
		this.auditEventRepository = auditEventRepository;
		this.userRepository = userRepository;
		this.authorizationPolicy = authorizationPolicy;
		this.clock = clock;
	}

	@Transactional
	public WorkflowTransitionResult transition(
			UUID interventionId,
			Intervention.WorkflowAction action,
			long expectedVersion,
			String reason,
			String requestId) {
		Intervention intervention = interventionRepository.findById(interventionId)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + interventionId));
		CivicPrincipal principal = authorize(action, intervention.getAgency().getId());
		assertVersion(intervention, expectedVersion);
		assertCurrentState(intervention, action);
		assertBusinessPreconditions(intervention, action, principal.userId());

		Instant transitionedAt = clock.instant();
		Map<String, Object> beforeState = state(intervention.getStatus().name(), intervention.getVersion());
		intervention.transition(action, transitionedAt);
		interventionRepository.saveAndFlush(intervention);
		Map<String, Object> afterState = state(intervention.getStatus().name(), intervention.getVersion());

		User actor = userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + principal.userId()));
		auditEventRepository.save(AuditEvent.workflowTransition(
				actor,
				"INTERVENTION",
				intervention.getId(),
				action.name(),
				beforeState,
				afterState,
				reason,
				requestId));

		return new WorkflowTransitionResult(
				"INTERVENTION",
				intervention.getId(),
				action.name(),
				action.source().name(),
				action.target().name(),
				intervention.getVersion(),
				transitionedAt);
	}

	private CivicPrincipal authorize(Intervention.WorkflowAction action, UUID agencyId) {
		return switch (action) {
			case SUBMIT -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_SUBMIT, agencyId, SystemRole.AGENCY_OFFICER);
			case BEGIN_REVIEW -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_UPDATE, agencyId, SystemRole.COORDINATOR);
			case REQUIRE_COORDINATION -> authorizationPolicy.authorize(
					PermissionCode.COORDINATION_UPDATE, agencyId, SystemRole.COORDINATOR);
			case APPROVE -> authorizationPolicy.authorize(
					PermissionCode.APPROVAL_APPROVE, agencyId,
					SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
			case SCHEDULE -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_SCHEDULE, agencyId, SystemRole.AGENCY_OFFICER);
			case START -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_START, agencyId, SystemRole.AGENCY_OFFICER);
			case COMPLETE -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_COMPLETE, agencyId, SystemRole.AGENCY_OFFICER);
			case FAIL_VERIFICATION -> authorizationPolicy.authorize(
					PermissionCode.VERIFICATION_FAIL, agencyId, SystemRole.INSPECTOR);
			case BEGIN_CORRECTIVE_ACTION -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_UPDATE, agencyId, SystemRole.AGENCY_OFFICER);
			case RESUME_CORRECTIVE_WORK -> authorizationPolicy.authorize(
					PermissionCode.INTERVENTION_RESUME, agencyId, SystemRole.AGENCY_OFFICER);
			case VERIFY -> authorizationPolicy.authorize(
					PermissionCode.VERIFICATION_PASS, agencyId, SystemRole.INSPECTOR);
			case CLOSE -> authorizationPolicy.authorize(
					PermissionCode.CLOSURE_APPROVAL, agencyId,
					SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
		};
	}

	private void assertVersion(Intervention intervention, long expectedVersion) {
		if (intervention.getVersion() != expectedVersion) {
			throw new StaleWorkflowVersionException(
					"Intervention", expectedVersion, intervention.getVersion());
		}
	}

	private void assertCurrentState(Intervention intervention, Intervention.WorkflowAction action) {
		if (intervention.getStatus() != action.source()) {
			throw new WorkflowActionNotAllowedException(
					"INTERVENTION", intervention.getStatus().name(), action.name());
		}
	}

	private void assertBusinessPreconditions(
			Intervention intervention,
			Intervention.WorkflowAction action,
			UUID actorId) {
		switch (action) {
			case APPROVE -> assertApprovalPreconditions(intervention, actorId);
			case VERIFY -> assertVerification(intervention.getId(), actorId, SUCCESSFUL_VERIFICATIONS);
			case FAIL_VERIFICATION -> assertVerification(intervention.getId(), actorId, FAILED_VERIFICATIONS);
			case CLOSE -> assertAnySuccessfulVerification(intervention.getId());
			default -> {
				// The state graph itself supplies the remaining preconditions in Phase 5.
			}
		}
	}

	private void assertApprovalPreconditions(Intervention intervention, UUID actorId) {
		if (intervention.getCreatedBy().getId().equals(actorId)) {
			throw new SeparationOfDutiesException();
		}
		if (conflictRepository.existsBlockingConflict(
				intervention.getId(), Conflict.Severity.HIGH, BLOCKING_CONFLICT_STATES)) {
			throw new WorkflowPreconditionException(
					"The intervention has an unresolved blocking conflict.");
		}
		if (!approvalRepository.existsByInterventionIdAndActorIdAndStatusIn(
				intervention.getId(), actorId, AUTHORITATIVE_APPROVALS)) {
			throw new WorkflowPreconditionException(
					"An authoritative approval by the current actor is required.");
		}
	}

	private void assertVerification(UUID interventionId, UUID actorId, List<Verification.Result> results) {
		Verification latest = verificationRepository
				.findFirstByTargetTypeAndTargetIdOrderByCreatedAtDesc("INTERVENTION", interventionId)
				.orElse(null);
		if (latest == null || latest.getSubmittedBy() == null
				|| !latest.getSubmittedBy().getId().equals(actorId)
				|| !results.contains(latest.getResult())) {
			throw new WorkflowPreconditionException(
					"The latest authoritative verification must match the current actor and action.");
		}
	}

	private void assertAnySuccessfulVerification(UUID interventionId) {
		List<Verification> verifications = verificationRepository
				.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("INTERVENTION", interventionId);
		if (verifications.stream().noneMatch(verification ->
				SUCCESSFUL_VERIFICATIONS.contains(verification.getResult()))) {
			throw new WorkflowPreconditionException(
					"A successful final verification is required before closure.");
		}
	}

	private Map<String, Object> state(String status, long version) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("status", status);
		state.put("version", version);
		return state;
	}
}
