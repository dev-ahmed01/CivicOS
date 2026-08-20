package com.civicos.dependency.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.application.DomainMutationResult;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.dependency.domain.Dependency;
import com.civicos.dependency.repository.DependencyRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.notification.application.NotificationOutboxService;
import com.civicos.notification.application.NotificationRequest;
import com.civicos.notification.domain.Notification;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class DependencyManagementService {

	private static final long DEPENDENCY_GRAPH_LOCK = 20_260_820L;

	private final DependencyRepository dependencyRepository;
	private final InterventionRepository interventionRepository;
	private final AuditEventRepository auditEventRepository;
	private final UserRepository userRepository;
	private final ScopedAuthorizationService authorizationService;
	private final DependencyScheduleValidator scheduleValidator;
	private final NotificationOutboxService notificationOutboxService;
	private final EntityManager entityManager;
	private final Clock clock;

	public DependencyManagementService(
			DependencyRepository dependencyRepository,
			InterventionRepository interventionRepository,
			AuditEventRepository auditEventRepository,
			UserRepository userRepository,
			ScopedAuthorizationService authorizationService,
			DependencyScheduleValidator scheduleValidator,
			NotificationOutboxService notificationOutboxService,
			EntityManager entityManager,
			Clock clock) {
		this.dependencyRepository = dependencyRepository;
		this.interventionRepository = interventionRepository;
		this.auditEventRepository = auditEventRepository;
		this.userRepository = userRepository;
		this.authorizationService = authorizationService;
		this.scheduleValidator = scheduleValidator;
		this.notificationOutboxService = notificationOutboxService;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional
	public DomainMutationResult create(
			CreateDependencyCommand command,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.COORDINATION_UPDATE, null, SystemRole.COORDINATOR);
		if (command.sourceInterventionId() == null || command.targetInterventionId() == null) {
			throw new DomainValidationException("Source and target interventions are required.");
		}
		if (command.type() == null) {
			throw new DomainValidationException("Dependency type is required.");
		}
		if (command.reason() == null || command.reason().isBlank()) {
			throw new DomainValidationException("Dependency reason is required.");
		}
		lockDependencyGraph();
		Intervention source = intervention(command.sourceInterventionId());
		Intervention target = intervention(command.targetInterventionId());
		if (target.getStatus() == Intervention.Status.CLOSED) {
			throw new DomainConflictException("A dependency cannot be added to a closed target intervention.");
		}
		if (dependencyRepository.existsBySourceInterventionIdAndTargetInterventionIdAndType(
				source.getId(), target.getId(), command.type())) {
			throw new DomainConflictException("The dependency relationship already exists.");
		}
		if (command.required() && createsCycle(source.getId(), target.getId())) {
			throw new DomainConflictException("The required dependency would create a cycle.");
		}

		boolean blocked = scheduleValidator.scheduleIsBlocked(source, target, command.required());
		Dependency dependency = Dependency.create(
				source, target, command.type(), command.required(), command.reason(), blocked);
		dependencyRepository.saveAndFlush(dependency);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "DEPENDENCY_CREATED", "DEPENDENCY", dependency.getId(),
				null, state(dependency), command.reason(), requestId));
		if (dependency.getStatus() == Dependency.Status.BLOCKED) {
			notificationOutboxService.enqueue(new NotificationRequest(
					"DEPENDENCY:" + dependency.getId() + ":BLOCKED",
					Notification.Type.DEPENDENCY_BLOCKED,
					target.getCreatedBy().getId(),
					"Dependency blocked",
					"A required dependency is blocking intervention "
							+ target.getInterventionNumber() + ".",
					"DEPENDENCY",
					dependency.getId()));
		}
		return new DomainMutationResult(
				"DEPENDENCY", dependency.getId(), "CREATE", dependency.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult cancel(
			UUID dependencyId,
			long expectedVersion,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.COORDINATION_UPDATE, null, SystemRole.COORDINATOR);
		if (reason == null || reason.isBlank()) {
			throw new DomainValidationException("A cancellation reason is required.");
		}
		Dependency dependency = dependencyRepository.findForUpdate(dependencyId)
				.orElseThrow(() -> new NoSuchElementException("Dependency not found: " + dependencyId));
		if (dependency.getVersion() != expectedVersion) {
			throw new StaleEntityVersionException(
					"Dependency", expectedVersion, dependency.getVersion());
		}
		Map<String, Object> before = state(dependency);
		dependency.cancel();
		dependencyRepository.saveAndFlush(dependency);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "DEPENDENCY_CANCELLED", "DEPENDENCY", dependency.getId(),
				before, state(dependency), reason, requestId));
		return new DomainMutationResult(
				"DEPENDENCY", dependency.getId(), "CANCEL", dependency.getVersion(), occurredAt);
	}

	private boolean createsCycle(UUID sourceId, UUID targetId) {
		List<Dependency> dependencies = dependencyRepository
				.findByRequiredTrueAndStatusNot(Dependency.Status.CANCELLED);
		Map<UUID, Set<UUID>> graph = new HashMap<>();
		for (Dependency dependency : dependencies) {
			graph.computeIfAbsent(dependency.getSourceIntervention().getId(), ignored -> new HashSet<>())
					.add(dependency.getTargetIntervention().getId());
		}

		ArrayDeque<UUID> pending = new ArrayDeque<>();
		Set<UUID> visited = new HashSet<>();
		pending.add(targetId);
		while (!pending.isEmpty()) {
			UUID current = pending.removeFirst();
			if (current.equals(sourceId)) {
				return true;
			}
			if (visited.add(current)) {
				pending.addAll(graph.getOrDefault(current, Set.of()));
			}
		}
		return false;
	}

	private void lockDependencyGraph() {
		entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
				.setParameter("lockId", DEPENDENCY_GRAPH_LOCK)
				.getSingleResult();
	}

	private Intervention intervention(UUID id) {
		return interventionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + id));
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + principal.userId()));
	}

	private Map<String, Object> state(Dependency dependency) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("sourceInterventionId", dependency.getSourceIntervention().getId().toString());
		state.put("targetInterventionId", dependency.getTargetIntervention().getId().toString());
		state.put("type", dependency.getType().name());
		state.put("required", dependency.isRequired());
		state.put("status", dependency.getStatus().name());
		state.put("reason", dependency.getReason());
		state.put("version", dependency.getVersion());
		return state;
	}
}
