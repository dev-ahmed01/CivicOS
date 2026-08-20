package com.civicos.approval.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.approval.domain.Approval;
import com.civicos.approval.repository.ApprovalRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.notification.application.NotificationOutboxService;
import com.civicos.notification.application.NotificationRequest;
import com.civicos.notification.domain.Notification;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;
import com.civicos.workflow.application.InterventionWorkflowService;

@Service
public class ApprovalService {

	private final ApprovalRepository approvalRepository;
	private final InterventionRepository interventionRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService authorizationService;
	private final ApprovalPolicy approvalPolicy;
	private final NotificationOutboxService notificationOutboxService;
	private final InterventionWorkflowService workflowService;
	private final Clock clock;

	public ApprovalService(
			ApprovalRepository approvalRepository,
			InterventionRepository interventionRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService authorizationService,
			ApprovalPolicy approvalPolicy,
			NotificationOutboxService notificationOutboxService,
			InterventionWorkflowService workflowService,
			Clock clock) {
		this.approvalRepository = approvalRepository;
		this.interventionRepository = interventionRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
		this.approvalPolicy = approvalPolicy;
		this.notificationOutboxService = notificationOutboxService;
		this.workflowService = workflowService;
		this.clock = clock;
	}

	@Transactional
	public ApprovalResult request(UUID interventionId, String reason, String requestId) {
		Intervention intervention = intervention(interventionId);
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.APPROVAL_REQUEST,
				intervention.getAgency().getId(),
				SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
		if (intervention.getStatus() != Intervention.Status.COORDINATION_COMPLETE) {
			throw new DomainConflictException(
					"Approval can only be requested in COORDINATION_COMPLETE state.");
		}
		if (approvalRepository.findByInterventionIdAndStatus(
				interventionId, Approval.Status.PENDING).isPresent()) {
			throw new DomainConflictException("A pending approval already exists.");
		}
		Approval approval = Approval.request(intervention);
		approvalRepository.saveAndFlush(approval);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "APPROVAL_REQUESTED", "APPROVAL", approval.getId(),
				null, state(approval), reason, requestId));
		workflowService.transition(
				interventionId, Intervention.WorkflowAction.REQUEST_APPROVAL,
				intervention.getVersion(), reason, requestId);
		notificationOutboxService.enqueue(new NotificationRequest(
				"APPROVAL:" + approval.getId() + ":PENDING",
				Notification.Type.APPROVAL_PENDING,
				intervention.getCreatedBy().getId(),
				"Approval pending",
				"Intervention " + intervention.getInterventionNumber()
						+ " is awaiting an authorised approval decision.",
				"INTERVENTION",
				intervention.getId()));
		return result(approval, occurredAt);
	}

	@Transactional
	public ApprovalResult decide(
			UUID approvalId,
			ApprovalDecisionCommand command,
			String requestId) {
		Approval approval = approvalRepository.findForUpdate(approvalId)
				.orElseThrow(() -> new NoSuchElementException("Approval not found: " + approvalId));
		Intervention intervention = approval.getIntervention();
		PermissionCode permission = permission(command.decision());
		CivicPrincipal principal = authorizationService.authorize(
				permission,
				intervention.getAgency().getId(),
				SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
		if (approval.getVersion() != command.expectedVersion()) {
			throw new StaleEntityVersionException(
					"Approval", command.expectedVersion(), approval.getVersion());
		}
		User actor = actor(principal);
		approvalPolicy.validate(intervention, actor.getId(), command.decision());
		Map<String, Object> before = state(approval);
		Instant occurredAt = clock.instant();
		approval.decide(command.decision(), actor, command.reason(), command.conditions(), occurredAt);
		approvalRepository.saveAndFlush(approval);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor, "APPROVAL_DECIDED", "APPROVAL", approval.getId(),
				before, state(approval), command.reason(), requestId));
		workflowService.transition(
				intervention.getId(), workflowAction(command.decision()),
				intervention.getVersion(), command.reason(), requestId);
		return result(approval, occurredAt);
	}

	private Intervention.WorkflowAction workflowAction(Approval.Decision decision) {
		return switch (decision) {
			case APPROVE, APPROVE_WITH_CONDITIONS -> Intervention.WorkflowAction.APPROVE;
			case REJECT -> Intervention.WorkflowAction.REJECT;
			case RETURN -> Intervention.WorkflowAction.RETURN_FOR_COORDINATION;
		};
	}

	private PermissionCode permission(Approval.Decision decision) {
		if (decision == null) {
			throw new com.civicos.common.domain.DomainValidationException("Approval decision is required.");
		}
		return switch (decision) {
			case APPROVE -> PermissionCode.APPROVAL_APPROVE;
			case APPROVE_WITH_CONDITIONS -> PermissionCode.APPROVAL_CONDITIONAL;
			case REJECT -> PermissionCode.APPROVAL_REJECT;
			case RETURN -> PermissionCode.APPROVAL_RETURN;
		};
	}

	private Map<String, Object> state(Approval approval) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("interventionId", approval.getIntervention().getId().toString());
		state.put("status", approval.getStatus().name());
		if (approval.getDecision() != null) {
			state.put("decision", approval.getDecision().name());
		}
		if (approval.getReason() != null) {
			state.put("reason", approval.getReason());
		}
		state.put("conditions", approval.getConditions());
		if (approval.getActor() != null) {
			state.put("actorId", approval.getActor().getId().toString());
		}
		state.put("version", approval.getVersion());
		return state;
	}

	private ApprovalResult result(Approval approval, Instant occurredAt) {
		return new ApprovalResult(
				approval.getId(), approval.getIntervention().getId(), approval.getStatus(),
				approval.getDecision(), approval.getVersion(), occurredAt);
	}

	private Intervention intervention(UUID id) {
		return interventionRepository.findForUpdate(id)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + id));
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
	}
}
