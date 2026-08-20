package com.civicos.conflict.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.conflict.application.ConflictAnalysisResult.DetectedConflict;
import com.civicos.conflict.application.ConflictAnalysisResult.Outcome;
import com.civicos.conflict.application.ConflictAnalysisResult.PersistenceAction;
import com.civicos.conflict.config.ConflictPolicyProperties;
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.dependency.domain.Dependency;
import com.civicos.dependency.repository.DependencyRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.ConflictCandidateProjection;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.notification.application.NotificationOutboxService;
import com.civicos.notification.application.NotificationRequest;
import com.civicos.notification.domain.Notification;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class ConflictDetectionService {

	private final InterventionRepository interventionRepository;
	private final DependencyRepository dependencyRepository;
	private final ConflictRepository conflictRepository;
	private final AuditEventRepository auditEventRepository;
	private final UserRepository userRepository;
	private final ScopedAuthorizationService authorizationService;
	private final ConflictRuleEngine ruleEngine;
	private final ConflictPolicyProperties policy;
	private final NotificationOutboxService notificationOutboxService;
	private final EntityManager entityManager;

	public ConflictDetectionService(
			InterventionRepository interventionRepository,
			DependencyRepository dependencyRepository,
			ConflictRepository conflictRepository,
			AuditEventRepository auditEventRepository,
			UserRepository userRepository,
			ScopedAuthorizationService authorizationService,
			ConflictRuleEngine ruleEngine,
			ConflictPolicyProperties policy,
			NotificationOutboxService notificationOutboxService,
			EntityManager entityManager) {
		this.interventionRepository = interventionRepository;
		this.dependencyRepository = dependencyRepository;
		this.conflictRepository = conflictRepository;
		this.auditEventRepository = auditEventRepository;
		this.userRepository = userRepository;
		this.authorizationService = authorizationService;
		this.ruleEngine = ruleEngine;
		this.policy = policy;
		this.notificationOutboxService = notificationOutboxService;
		this.entityManager = entityManager;
	}

	@Transactional
	public ConflictAnalysisResult analyse(UUID interventionId, String requestId) {
		Intervention target = intervention(interventionId);
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.CONFLICT_ANALYSE,
				target.getAgency().getId(),
				SystemRole.COORDINATOR);
		User actor = actor(principal);

		// Serializing analyses for a segment prevents duplicate deterministic conflict rows.
		acquireSegmentAnalysisLock(target.getRoadSegment().getId());
		List<ConflictCandidate> candidates = candidates(target);
		Set<UUID> analysedInterventionIds = candidates.stream()
				.map(candidate -> candidate.intervention().getId())
				.collect(Collectors.toCollection(LinkedHashSet::new));
		analysedInterventionIds.add(target.getId());
		List<Dependency> dependencies = dependencyRepository.findAllConnectedTo(analysedInterventionIds);
		List<ConflictFinding> findings = ruleEngine.evaluate(target, candidates, dependencies);

		List<DetectedConflict> detected = new ArrayList<>();
		for (ConflictFinding finding : findings) {
			detected.add(persist(finding, actor, requestId));
		}
		detected.sort(Comparator.comparing(result -> result.type().name()));

		Outcome outcome = detected.isEmpty() ? Outcome.NO_CONFLICT : Outcome.CONFLICTS_DETECTED;
		auditEventRepository.save(AuditEvent.domainMutation(
				actor,
				"CONFLICT_ANALYSIS_COMPLETED",
				"INTERVENTION",
				target.getId(),
				null,
				analysisState(outcome, detected),
				"Deterministic conflict analysis completed.",
				requestId));
		return new ConflictAnalysisResult(target.getId(), outcome, detected);
	}

	private List<ConflictCandidate> candidates(Intervention target) {
		Instant windowStart = target.getPlannedStart()
				.minus(policy.getRepeatDiggingWindowDays(), ChronoUnit.DAYS);
		Instant windowEnd = target.getPlannedEnd()
				.plus(policy.getRepeatDiggingWindowDays(), ChronoUnit.DAYS);
		List<ConflictCandidateProjection> relations = interventionRepository.findConflictCandidateRelations(
				target.getId(), windowStart, windowEnd, policy.getProximityMeters());
		Map<UUID, Intervention> interventions = interventionRepository.findAllById(
				relations.stream().map(ConflictCandidateProjection::getId).toList()).stream()
				.collect(Collectors.toMap(Intervention::getId, Function.identity()));
		return relations.stream()
				.map(relation -> new ConflictCandidate(
						candidate(interventions, relation.getId()),
						relation.getSameRoadSegment(),
						relation.getSpatialOverlap(),
						relation.getDistanceMeters()))
				.toList();
	}

	private DetectedConflict persist(ConflictFinding finding, User actor, String requestId) {
		String conflictNumber = conflictNumber(finding.scopeKey());
		Conflict conflict = conflictRepository.findByConflictNumber(conflictNumber).orElse(null);
		PersistenceAction action;
		Map<String, Object> before = null;
		if (conflict == null) {
			conflict = Conflict.create(
					conflictNumber,
					finding.roadSegment(),
					finding.type(),
					finding.severity(),
					finding.explanation(),
					finding.interventions());
			conflictRepository.saveAndFlush(conflict);
			action = PersistenceAction.CREATED;
		} else if (conflict.getStatus() == Conflict.Status.RESOLVED
				|| conflict.getStatus() == Conflict.Status.DISMISSED) {
			action = PersistenceAction.HUMAN_DECISION_PRESERVED;
		} else {
			before = conflictState(conflict);
			boolean changed = conflict.updateDetection(
					finding.severity(), finding.explanation(), finding.interventions());
			if (changed) {
				conflictRepository.saveAndFlush(conflict);
				action = PersistenceAction.UPDATED;
			} else {
				action = PersistenceAction.UNCHANGED;
			}
		}
		if (action == PersistenceAction.CREATED || action == PersistenceAction.UPDATED) {
			auditEventRepository.save(AuditEvent.domainMutation(
					actor,
					action == PersistenceAction.CREATED ? "CONFLICT_DETECTED" : "CONFLICT_DETECTION_UPDATED",
					"CONFLICT",
					conflict.getId(),
					before,
					conflictState(conflict),
					finding.explanation(),
					requestId));
			enqueueConflictNotifications(conflict);
		}
		return result(conflict, action);
	}

	private void enqueueConflictNotifications(Conflict conflict) {
		for (Intervention intervention : conflict.getInterventions()) {
			notificationOutboxService.enqueue(new NotificationRequest(
					"CONFLICT:" + conflict.getId() + ":" + conflict.getVersion()
							+ ":" + intervention.getCreatedBy().getId(),
					Notification.Type.CONFLICT_DETECTED,
					intervention.getCreatedBy().getId(),
					"Coordination conflict detected",
					"A " + conflict.getSeverity().name()
							+ " coordination conflict requires review for intervention "
							+ intervention.getInterventionNumber() + ".",
					"CONFLICT",
					conflict.getId()));
		}
	}

	private Map<String, Object> conflictState(Conflict conflict) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("conflictNumber", conflict.getConflictNumber());
		state.put("roadSegmentId", conflict.getRoadSegment().getId().toString());
		state.put("type", conflict.getType().name());
		state.put("severity", conflict.getSeverity().name());
		state.put("status", conflict.getStatus().name());
		state.put("explanation", conflict.getExplanation());
		state.put("affectedInterventionIds", affectedIds(conflict.getInterventions()).stream()
				.map(UUID::toString).toList());
		return state;
	}

	private Map<String, Object> analysisState(Outcome outcome, List<DetectedConflict> detected) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("outcome", outcome.name());
		state.put("findingCount", detected.size());
		state.put("conflictIds", detected.stream()
				.map(result -> result.conflictId().toString())
				.sorted()
				.toList());
		return state;
	}

	private DetectedConflict result(Conflict conflict, PersistenceAction action) {
		return new DetectedConflict(
				conflict.getId(),
				conflict.getConflictNumber(),
				conflict.getType(),
				conflict.getSeverity(),
				conflict.getStatus(),
				affectedIds(conflict.getInterventions()),
				action);
	}

	private List<UUID> affectedIds(Collection<Intervention> interventions) {
		return interventions.stream()
				.map(Intervention::getId)
				.sorted(Comparator.comparing(UUID::toString))
				.toList();
	}

	private String conflictNumber(String scopeKey) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(scopeKey.getBytes(StandardCharsets.UTF_8));
			return "CF-" + HexFormat.of().formatHex(digest, 0, 12).toUpperCase();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	private void acquireSegmentAnalysisLock(UUID roadSegmentId) {
		long lockId = roadSegmentId.getMostSignificantBits() ^ roadSegmentId.getLeastSignificantBits();
		entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
				.setParameter("lockId", lockId)
				.getSingleResult();
	}

	private Intervention intervention(UUID id) {
		return interventionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + id));
	}

	private Intervention candidate(Map<UUID, Intervention> interventions, UUID id) {
		Intervention intervention = interventions.get(id);
		if (intervention == null) {
			throw new IllegalStateException("Conflict candidate no longer exists: " + id);
		}
		return intervention;
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
	}
}
