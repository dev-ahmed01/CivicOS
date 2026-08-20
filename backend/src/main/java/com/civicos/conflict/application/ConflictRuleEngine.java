package com.civicos.conflict.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.civicos.conflict.config.ConflictPolicyProperties;
import com.civicos.conflict.domain.Conflict;
import com.civicos.dependency.domain.Dependency;
import com.civicos.intervention.domain.Intervention;
import com.civicos.road.domain.RoadSegment;

@Component
public class ConflictRuleEngine {

	private static final Set<Intervention.Type> EXCAVATION_TYPES = Set.of(
			Intervention.Type.UTILITY_EXCAVATION,
			Intervention.Type.WATER,
			Intervention.Type.SEWER,
			Intervention.Type.ELECTRICAL,
			Intervention.Type.TELECOM,
			Intervention.Type.DRAINAGE);
	private static final Set<Intervention.Type> RESTORATION_TYPES = Set.of(
			Intervention.Type.RESTORATION,
			Intervention.Type.RESURFACING);

	private final ConflictPolicyProperties policy;

	public ConflictRuleEngine(ConflictPolicyProperties policy) {
		this.policy = policy;
	}

	public List<ConflictFinding> evaluate(
			Intervention target,
			List<ConflictCandidate> candidates,
			List<Dependency> dependencies) {
		Map<String, ConflictFinding> findings = new LinkedHashMap<>();
		for (ConflictCandidate candidate : candidates) {
			evaluatePair(target, candidate, findings);
		}
		evaluateUnsafeSequencing(target, dependencies, findings);
		evaluateRestoration(target, candidates, findings);
		evaluateRepeatDigging(target, candidates, findings);
		evaluateMultiAgency(target, candidates, findings);
		return findings.values().stream()
				.sorted(Comparator.comparing((ConflictFinding finding) -> finding.type().name())
						.thenComparing(ConflictFinding::scopeKey))
				.toList();
	}

	private void evaluatePair(
			Intervention target,
			ConflictCandidate candidate,
			Map<String, ConflictFinding> findings) {
		Intervention other = candidate.intervention();
		boolean temporal = overlaps(target, other);
		boolean differentAgencies = !target.getAgency().getId().equals(other.getAgency().getId());
		Set<Intervention> pair = Set.of(target, other);

		if (candidate.sameRoadSegment() && temporal) {
			Conflict.Severity severity = candidate.spatialOverlap() && differentAgencies
					? Conflict.Severity.HIGH
					: Conflict.Severity.MEDIUM;
			addPairFinding(findings, target, other, Conflict.Type.SAME_ROAD_OVERLAP, severity,
					pair, "The interventions use the same road segment during overlapping planned windows.");
		}
		if (candidate.spatialOverlap()) {
			Conflict.Severity severity = temporal && differentAgencies
					? Conflict.Severity.HIGH
					: Conflict.Severity.MEDIUM;
			addPairFinding(findings, target, other, Conflict.Type.SPATIAL_OVERLAP, severity,
					pair, "The authoritative PostGIS geometries intersect.");
		}
		if (temporal) {
			Conflict.Severity severity = candidate.spatialOverlap()
					? Conflict.Severity.MEDIUM
					: Conflict.Severity.LOW;
			addPairFinding(findings, target, other, Conflict.Type.TEMPORAL_OVERLAP, severity,
					pair, "The planned execution intervals overlap.");
		}
	}

	private void evaluateUnsafeSequencing(
			Intervention target,
			List<Dependency> dependencies,
			Map<String, ConflictFinding> findings) {
		for (Dependency dependency : dependencies) {
			if (!dependency.isRequired() || dependency.getStatus() == Dependency.Status.CANCELLED) {
				continue;
			}
			Intervention source = dependency.getSourceIntervention();
			Intervention dependent = dependency.getTargetIntervention();
			boolean invalidOrder = effectiveCompletion(source).isAfter(dependent.getPlannedStart());
			if (dependency.getStatus() == Dependency.Status.BLOCKED || invalidOrder) {
				Set<Intervention> pair = Set.of(source, dependent);
				addPairFinding(findings, source, dependent, Conflict.Type.UNSAFE_SEQUENCING,
						Conflict.Severity.HIGH, pair,
						"A required dependency is scheduled before its prerequisite completes.");
			}
		}
	}

	private void evaluateRestoration(
			Intervention target,
			List<ConflictCandidate> candidates,
			Map<String, ConflictFinding> findings) {
		Set<Intervention> affected = new LinkedHashSet<>();
		if (isRestoration(target)) {
			affected.add(target);
			candidates.stream()
					.filter(candidate -> sameCorridor(candidate) && isExcavation(candidate.intervention()))
					.map(ConflictCandidate::intervention)
					.filter(excavation -> target.getPlannedStart().isBefore(excavation.getPlannedEnd()))
					.forEach(affected::add);
		} else if (isExcavation(target)) {
			affected.add(target);
			candidates.stream()
					.filter(candidate -> sameCorridor(candidate) && isRestoration(candidate.intervention()))
					.map(ConflictCandidate::intervention)
					.filter(restoration -> restoration.getPlannedStart().isBefore(target.getPlannedEnd()))
					.forEach(affected::add);
		}
		if (affected.size() >= 2) {
			RoadSegment segment = canonicalSegment(affected);
			String key = groupKey(Conflict.Type.RESTORATION_BEFORE_EXCAVATION_COMPLETION, segment);
			findings.put(key, new ConflictFinding(
					key, segment,
					Conflict.Type.RESTORATION_BEFORE_EXCAVATION_COMPLETION,
					Conflict.Severity.HIGH,
					affected,
					"Restoration or resurfacing is planned before all affected excavation work completes."));
		}
	}

	private void evaluateRepeatDigging(
			Intervention target,
			List<ConflictCandidate> candidates,
			Map<String, ConflictFinding> findings) {
		Set<Intervention> affected = new LinkedHashSet<>();
		List<Intervention> excavations = new ArrayList<>();
		if (isExcavation(target)) {
			excavations.add(target);
		}
		candidates.stream()
				.filter(this::sameCorridor)
				.map(ConflictCandidate::intervention)
				.filter(this::isExcavation)
				.forEach(excavations::add);
		for (int first = 0; first < excavations.size(); first++) {
			for (int second = first + 1; second < excavations.size(); second++) {
				if (withinRepeatWindow(excavations.get(first), excavations.get(second))) {
					affected.add(excavations.get(first));
					affected.add(excavations.get(second));
				}
			}
		}

		String explanation = "Multiple excavation activities affect the same corridor within the configured repeat-work window.";
		if (affected.size() < 2) {
			affected.clear();
			if (isRestoration(target)) {
				affected.add(target);
				candidates.stream()
						.filter(this::sameCorridor)
						.map(ConflictCandidate::intervention)
						.filter(this::isExcavation)
						.filter(excavation -> followsWithinRepeatWindow(target, excavation))
						.forEach(affected::add);
			} else if (isExcavation(target)) {
				affected.add(target);
				candidates.stream()
						.filter(this::sameCorridor)
						.map(ConflictCandidate::intervention)
						.filter(this::isRestoration)
						.filter(restoration -> followsWithinRepeatWindow(restoration, target))
						.forEach(affected::add);
			}
			explanation = "Excavation is planned shortly after restoration within the configured repeat-work window.";
		}
		if (affected.size() >= 2) {
			RoadSegment segment = canonicalSegment(affected);
			String key = groupKey(Conflict.Type.REPEAT_DIGGING_RISK, segment);
			findings.put(key, new ConflictFinding(
					key, segment, Conflict.Type.REPEAT_DIGGING_RISK,
					Conflict.Severity.MEDIUM, affected, explanation));
		}
	}

	private void evaluateMultiAgency(
			Intervention target,
			List<ConflictCandidate> candidates,
			Map<String, ConflictFinding> findings) {
		Set<Intervention> affected = new LinkedHashSet<>();
		affected.add(target);
		candidates.stream()
				.filter(this::sameCorridor)
				.map(ConflictCandidate::intervention)
				.forEach(affected::add);
		long agencyCount = affected.stream()
				.map(intervention -> intervention.getAgency().getId())
				.distinct()
				.count();
		if (agencyCount >= policy.getMultiAgencyMinimum()) {
			RoadSegment segment = canonicalSegment(affected);
			String key = groupKey(Conflict.Type.MULTI_AGENCY_COORDINATION, segment);
			findings.put(key, new ConflictFinding(
					key, segment, Conflict.Type.MULTI_AGENCY_COORDINATION,
					Conflict.Severity.HIGH, affected,
					"Multiple agencies require coordinated access or sequencing on the shared road corridor."));
		}
	}

	private void addPairFinding(
			Map<String, ConflictFinding> findings,
			Intervention first,
			Intervention second,
			Conflict.Type type,
			Conflict.Severity severity,
			Set<Intervention> interventions,
			String explanation) {
		String key = pairKey(type, first, second);
		findings.putIfAbsent(key, new ConflictFinding(
				key, canonicalSegment(interventions), type,
				elevateForCriticalPriority(severity, interventions),
				interventions, explanation));
	}

	private Conflict.Severity elevateForCriticalPriority(
			Conflict.Severity severity,
			Set<Intervention> interventions) {
		boolean critical = interventions.stream()
				.anyMatch(intervention -> intervention.getPriority() == Intervention.Priority.CRITICAL);
		return critical ? Conflict.Severity.HIGH : severity;
	}

	private boolean overlaps(Intervention first, Intervention second) {
		return first.getPlannedStart().isBefore(second.getPlannedEnd())
				&& second.getPlannedStart().isBefore(first.getPlannedEnd());
	}

	private boolean followsWithinRepeatWindow(
			Intervention restoration,
			Intervention excavation) {
		java.time.Instant restoredAt = effectiveCompletion(restoration);
		if (excavation.getPlannedStart().isBefore(restoredAt)) {
			return false;
		}
		Duration gap = Duration.between(restoredAt, excavation.getPlannedStart());
		return gap.compareTo(Duration.ofDays(policy.getRepeatDiggingWindowDays())) <= 0;
	}

	private boolean withinRepeatWindow(Intervention first, Intervention second) {
		Duration gap;
		if (overlaps(first, second)) {
			gap = Duration.ZERO;
		} else if (first.getPlannedEnd().isBefore(second.getPlannedStart())) {
			gap = Duration.between(first.getPlannedEnd(), second.getPlannedStart());
		} else {
			gap = Duration.between(second.getPlannedEnd(), first.getPlannedStart());
		}
		return gap.compareTo(Duration.ofDays(policy.getRepeatDiggingWindowDays())) <= 0;
	}

	private java.time.Instant effectiveCompletion(Intervention intervention) {
		return intervention.getActualEnd() == null ? intervention.getPlannedEnd() : intervention.getActualEnd();
	}

	private boolean sameCorridor(ConflictCandidate candidate) {
		return candidate.sameRoadSegment() || candidate.spatialOverlap()
				|| candidate.distanceMeters() <= policy.getProximityMeters();
	}

	private boolean isExcavation(Intervention intervention) {
		return EXCAVATION_TYPES.contains(intervention.getType());
	}

	private boolean isRestoration(Intervention intervention) {
		return RESTORATION_TYPES.contains(intervention.getType());
	}

	private String pairKey(Conflict.Type type, Intervention first, Intervention second) {
		List<String> ids = List.of(first.getId().toString(), second.getId().toString()).stream()
				.sorted()
				.toList();
		return type.name() + ":" + String.join(":", ids);
	}

	private String groupKey(Conflict.Type type, RoadSegment segment) {
		return type.name() + ":" + segment.getId();
	}

	private RoadSegment canonicalSegment(Set<Intervention> interventions) {
		return interventions.stream()
				.map(Intervention::getRoadSegment)
				.min(Comparator.comparing(segment -> segment.getId().toString()))
				.orElseThrow();
	}
}
