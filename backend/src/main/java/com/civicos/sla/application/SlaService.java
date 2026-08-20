package com.civicos.sla.application;

import java.time.Clock;
import java.time.Duration;
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
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.escalation.application.EscalationService;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.notification.application.NotificationOutboxService;
import com.civicos.notification.application.NotificationRequest;
import com.civicos.notification.domain.Notification;
import com.civicos.sla.config.SlaPolicyProperties;
import com.civicos.sla.domain.Sla;
import com.civicos.sla.repository.SlaRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class SlaService {

	private static final List<Sla.Status> OPEN_STATUSES = List.of(
			Sla.Status.NORMAL, Sla.Status.AT_RISK, Sla.Status.BREACHED, Sla.Status.PAUSED);

	private final SlaRepository slaRepository;
	private final AuditEventRepository auditEventRepository;
	private final UserRepository userRepository;
	private final ScopedAuthorizationService authorizationService;
	private final SlaDeadlineCalculator deadlineCalculator;
	private final SlaPolicyProperties policy;
	private final EscalationService escalationService;
	private final InterventionRepository interventionRepository;
	private final NotificationOutboxService notificationOutboxService;
	private final Clock clock;
	private final EntityManager entityManager;

	public SlaService(
			SlaRepository slaRepository,
			AuditEventRepository auditEventRepository,
			UserRepository userRepository,
			ScopedAuthorizationService authorizationService,
			SlaDeadlineCalculator deadlineCalculator,
			SlaPolicyProperties policy,
			EscalationService escalationService,
			InterventionRepository interventionRepository,
			NotificationOutboxService notificationOutboxService,
			Clock clock,
			EntityManager entityManager) {
		this.slaRepository = slaRepository;
		this.auditEventRepository = auditEventRepository;
		this.userRepository = userRepository;
		this.authorizationService = authorizationService;
		this.deadlineCalculator = deadlineCalculator;
		this.policy = policy;
		this.escalationService = escalationService;
		this.interventionRepository = interventionRepository;
		this.notificationOutboxService = notificationOutboxService;
		this.clock = clock;
		this.entityManager = entityManager;
	}

	@Transactional
	public SlaResult create(CreateSlaCommand command, String reason, String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.SLA_MANAGE, null, SystemRole.ADMIN);
		return create(command, actor(principal), reason, requestId);
	}

	@Transactional
	public SlaResult createSystem(CreateSlaCommand command, String reason, String requestId) {
		return create(command, null, reason, requestId);
	}

	@Transactional
	public SlaResult pause(UUID slaId, long expectedVersion, String reason, String requestId) {
		return mutate(slaId, expectedVersion, reason, requestId, "SLA_PAUSED", Sla::pause);
	}

	@Transactional
	public SlaResult resume(UUID slaId, long expectedVersion, String reason, String requestId) {
		return mutate(slaId, expectedVersion, reason, requestId, "SLA_RESUMED", Sla::resume);
	}

	@Transactional
	public SlaResult complete(UUID slaId, long expectedVersion, String reason, String requestId) {
		return mutate(slaId, expectedVersion, reason, requestId, "SLA_COMPLETED", Sla::complete);
	}

	@Transactional
	public SlaResult monitorOne(UUID slaId, Instant assessedAt, String requestId) {
		Sla sla = slaRepository.findForUpdate(slaId)
				.orElseThrow(() -> new NoSuchElementException("SLA not found: " + slaId));
		Map<String, Object> before = state(sla);
		boolean changed = sla.assess(
				assessedAt, Duration.ofHours(policy.getAtRiskBeforeHours()));
		if (!changed) {
			if (sla.getStatus() == Sla.Status.BREACHED) {
				escalationService.createForBreach(sla, requestId);
			}
			return result(sla);
		}
		slaRepository.saveAndFlush(sla);
		String action = sla.getStatus() == Sla.Status.BREACHED
				? "SLA_BREACHED" : "SLA_AT_RISK";
		auditEventRepository.save(AuditEvent.domainMutation(
				null, action, "SLA", sla.getId(), before, state(sla),
				"Deterministic SLA monitoring assessment.", requestId));
		enqueueDeadlineNotification(sla);
		if (sla.getStatus() == Sla.Status.BREACHED) {
			escalationService.createForBreach(sla, requestId);
		}
		return result(sla);
	}

	private void enqueueDeadlineNotification(Sla sla) {
		if (!"INTERVENTION".equals(sla.getTargetType())) {
			return;
		}
		interventionRepository.findById(sla.getTargetId()).ifPresent(intervention -> {
			Notification.Type type = sla.getStatus() == Sla.Status.BREACHED
					? Notification.Type.SLA_BREACHED
					: Notification.Type.DEADLINE_APPROACHING;
			String title = sla.getStatus() == Sla.Status.BREACHED
					? "SLA breached" : "SLA deadline approaching";
			notificationOutboxService.enqueue(new NotificationRequest(
					"SLA:" + sla.getId() + ":" + sla.getStatus() + ":" + sla.getVersion(),
					type,
					intervention.getCreatedBy().getId(),
					title,
					"The " + sla.getSlaType().name() + " SLA for intervention "
							+ intervention.getInterventionNumber() + " is " + sla.getStatus().name() + ".",
					"INTERVENTION",
					intervention.getId()));
		});
	}

	private SlaResult create(
			CreateSlaCommand command,
			User actor,
			String reason,
			String requestId) {
		String targetType = command.targetType() == null ? null : command.targetType().strip().toUpperCase();
		if (command.targetId() != null && command.slaType() != null) {
			acquireCreationLock(command.targetId(), command.slaType());
		}
		if (targetType != null && command.targetId() != null && command.slaType() != null) {
			Sla existing = slaRepository.findActive(
					targetType, command.targetId(), command.slaType(), OPEN_STATUSES).orElse(null);
			if (existing != null) {
				return result(existing);
			}
		}
		Instant startAt = command.startAt() == null ? clock.instant() : command.startAt();
		Instant deadline = deadlineCalculator.calculate(startAt, command.slaType(), command.urgency());
		Sla sla = Sla.create(targetType, command.targetId(), command.slaType(), startAt, deadline);
		slaRepository.saveAndFlush(sla);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor, "SLA_CREATED", "SLA", sla.getId(), null, state(sla), reason, requestId));
		return result(sla);
	}

	private SlaResult mutate(
			UUID slaId,
			long expectedVersion,
			String reason,
			String requestId,
			String action,
			SlaMutation mutation) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.SLA_MANAGE, null, SystemRole.ADMIN);
		Sla sla = slaRepository.findForUpdate(slaId)
				.orElseThrow(() -> new NoSuchElementException("SLA not found: " + slaId));
		if (sla.getVersion() != expectedVersion) {
			throw new StaleEntityVersionException("SLA", expectedVersion, sla.getVersion());
		}
		Map<String, Object> before = state(sla);
		mutation.apply(sla, clock.instant());
		slaRepository.saveAndFlush(sla);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), action, "SLA", sla.getId(), before, state(sla), reason, requestId));
		return result(sla);
	}

	private Map<String, Object> state(Sla sla) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("targetType", sla.getTargetType());
		state.put("targetId", sla.getTargetId().toString());
		state.put("slaType", sla.getSlaType().name());
		state.put("startAt", sla.getStartAt().toString());
		state.put("deadline", sla.getDeadline().toString());
		state.put("status", sla.getStatus().name());
		if (sla.getPausedAt() != null) {
			state.put("pausedAt", sla.getPausedAt().toString());
		}
		if (sla.getCompletedAt() != null) {
			state.put("completedAt", sla.getCompletedAt().toString());
		}
		state.put("version", sla.getVersion());
		return state;
	}

	private SlaResult result(Sla sla) {
		return new SlaResult(
				sla.getId(), sla.getTargetType(), sla.getTargetId(), sla.getSlaType(),
				sla.getStatus(), sla.getDeadline(), sla.getVersion());
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
	}

	private void acquireCreationLock(UUID targetId, Sla.Type slaType) {
		long lockId = targetId.getMostSignificantBits()
				^ targetId.getLeastSignificantBits()
				^ slaType.ordinal();
		entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
				.setParameter("lockId", lockId)
				.getSingleResult();
	}

	@FunctionalInterface
	private interface SlaMutation {
		void apply(Sla sla, Instant occurredAt);
	}
}
