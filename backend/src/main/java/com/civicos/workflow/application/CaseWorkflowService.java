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

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.casefile.repository.CivicCaseRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;
import com.civicos.verification.domain.Verification;
import com.civicos.verification.repository.VerificationRepository;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;

@Service
public class CaseWorkflowService {

	private static final List<Verification.Result> SUCCESSFUL_VERIFICATIONS = List.of(
			Verification.Result.PASSED,
			Verification.Result.CONFIRMED);

	private final CivicCaseRepository caseRepository;
	private final VerificationRepository verificationRepository;
	private final AuditEventRepository auditEventRepository;
	private final UserRepository userRepository;
	private final WorkflowAuthorizationPolicy authorizationPolicy;
	private final Clock clock;

	public CaseWorkflowService(
			CivicCaseRepository caseRepository,
			VerificationRepository verificationRepository,
			AuditEventRepository auditEventRepository,
			UserRepository userRepository,
			WorkflowAuthorizationPolicy authorizationPolicy,
			Clock clock) {
		this.caseRepository = caseRepository;
		this.verificationRepository = verificationRepository;
		this.auditEventRepository = auditEventRepository;
		this.userRepository = userRepository;
		this.authorizationPolicy = authorizationPolicy;
		this.clock = clock;
	}

	@Transactional
	public WorkflowTransitionResult transition(
			UUID caseId,
			CivicCase.WorkflowAction action,
			long expectedVersion,
			String reason,
			String requestId) {
		CivicCase civicCase = caseRepository.findById(caseId)
				.orElseThrow(() -> new NoSuchElementException("Civic case not found: " + caseId));
		CivicPrincipal principal = authorize(action);
		assertVersion(civicCase, expectedVersion);
		assertCurrentState(civicCase, action);
		if (action == CivicCase.WorkflowAction.VERIFY) {
			assertSuccessfulVerification(civicCase.getId(), principal.userId());
		} else if (action == CivicCase.WorkflowAction.CLOSE) {
			assertAnySuccessfulVerification(civicCase.getId());
		}

		Instant transitionedAt = clock.instant();
		Map<String, Object> beforeState = state(civicCase.getStatus().name(), civicCase.getVersion());
		civicCase.transition(action, transitionedAt);
		caseRepository.saveAndFlush(civicCase);
		Map<String, Object> afterState = state(civicCase.getStatus().name(), civicCase.getVersion());

		User actor = userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + principal.userId()));
		auditEventRepository.save(AuditEvent.workflowTransition(
				actor,
				"CIVIC_CASE",
				civicCase.getId(),
				action.name(),
				beforeState,
				afterState,
				reason,
				requestId));

		return new WorkflowTransitionResult(
				"CIVIC_CASE",
				civicCase.getId(),
				action.name(),
				action.source().name(),
				action.target().name(),
				civicCase.getVersion(),
				transitionedAt);
	}

	private CivicPrincipal authorize(CivicCase.WorkflowAction action) {
		return switch (action) {
			case BEGIN_REVIEW -> authorizationPolicy.authorize(
					PermissionCode.OBSERVATION_TRIAGE, null, SystemRole.COORDINATOR);
			case START_PROGRESS -> authorizationPolicy.authorize(
					PermissionCode.OBSERVATION_MATCH, null,
					SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
			case REQUEST_VERIFICATION -> authorizationPolicy.authorize(
					PermissionCode.INSPECTION_CREATE, null,
					SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
			case VERIFY -> authorizationPolicy.authorize(
					PermissionCode.VERIFICATION_PASS, null, SystemRole.INSPECTOR);
			case CLOSE -> authorizationPolicy.authorize(
					PermissionCode.CLOSURE_APPROVAL, null,
					SystemRole.AGENCY_OFFICER, SystemRole.COORDINATOR);
		};
	}

	private void assertVersion(CivicCase civicCase, long expectedVersion) {
		if (civicCase.getVersion() != expectedVersion) {
			throw new StaleWorkflowVersionException("Civic case", expectedVersion, civicCase.getVersion());
		}
	}

	private void assertCurrentState(CivicCase civicCase, CivicCase.WorkflowAction action) {
		if (civicCase.getStatus() != action.source()) {
			throw new WorkflowActionNotAllowedException(
					"CIVIC_CASE", civicCase.getStatus().name(), action.name());
		}
	}

	private void assertSuccessfulVerification(UUID caseId, UUID actorId) {
		if (!verificationRepository.existsByTargetTypeAndTargetIdAndSubmittedByIdAndResultIn(
				"CIVIC_CASE", caseId, actorId, SUCCESSFUL_VERIFICATIONS)) {
			throw new WorkflowPreconditionException(
					"A successful authoritative verification by the current actor is required.");
		}
	}

	private void assertAnySuccessfulVerification(UUID caseId) {
		List<Verification> verifications = verificationRepository
				.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("CIVIC_CASE", caseId);
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
