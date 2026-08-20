package com.civicos.conflict.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class ConflictResolutionService {

	private final ConflictRepository conflictRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService authorizationService;
	private final Clock clock;

	public ConflictResolutionService(
			ConflictRepository conflictRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService authorizationService,
			Clock clock) {
		this.conflictRepository = conflictRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
		this.clock = clock;
	}

	@Transactional
	public ConflictResolutionResult resolve(
			UUID conflictId,
			Conflict.Status outcome,
			long expectedVersion,
			String reason,
			String requestId) {
		String normalizedReason = ValidationRules.requiredText(reason, "Conflict resolution reason");
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.CONFLICT_RESOLVE, null, SystemRole.COORDINATOR);
		Conflict conflict = conflictRepository.findForUpdate(conflictId)
				.orElseThrow(() -> new NoSuchElementException("Conflict not found: " + conflictId));
		if (conflict.getVersion() != expectedVersion) {
			throw new StaleEntityVersionException(
					"Conflict", expectedVersion, conflict.getVersion());
		}
		Map<String, Object> before = state(conflict);
		Instant resolvedAt = clock.instant();
		conflict.resolve(outcome, resolvedAt);
		conflictRepository.saveAndFlush(conflict);
		User actor = userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
		String auditAction = outcome == Conflict.Status.DISMISSED
				? "CONFLICT_DISMISSED"
				: "CONFLICT_RESOLVED";
		auditEventRepository.save(AuditEvent.domainMutation(
				actor, auditAction, "CONFLICT", conflict.getId(),
				before, state(conflict), normalizedReason, requestId));
		return new ConflictResolutionResult(
				conflict.getId(), conflict.getStatus(), conflict.getVersion(), resolvedAt);
	}

	private Map<String, Object> state(Conflict conflict) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("status", conflict.getStatus().name());
		state.put("severity", conflict.getSeverity().name());
		state.put("version", conflict.getVersion());
		if (conflict.getResolvedAt() != null) {
			state.put("resolvedAt", conflict.getResolvedAt().toString());
		}
		return state;
	}
}
