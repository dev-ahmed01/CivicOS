package com.civicos.conflict.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.civicos.common.persistence.AbstractUuidEntity;
import com.civicos.intervention.domain.Intervention;
import com.civicos.road.domain.RoadSegment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "conflicts")
public class Conflict extends AbstractUuidEntity {

	public enum Type {
		SAME_ROAD_OVERLAP, SPATIAL_OVERLAP, TEMPORAL_OVERLAP, UNSAFE_SEQUENCING,
		REPEAT_DIGGING_RISK, RESTORATION_BEFORE_EXCAVATION_COMPLETION,
		MULTI_AGENCY_COORDINATION
	}

	public enum Severity { LOW, MEDIUM, HIGH }
	public enum Status { OPEN, UNDER_REVIEW, RESOLVED, DISMISSED }

	@Column(name = "conflict_number", nullable = false, unique = true, length = 50)
	private String conflictNumber;

	@ManyToOne(optional = false)
	@JoinColumn(name = "road_segment_id", nullable = false)
	private RoadSegment roadSegment;

	@Enumerated(EnumType.STRING)
	@Column(name = "conflict_type", nullable = false, length = 70)
	private Type type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Severity severity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.OPEN;

	@CreationTimestamp
	@Column(name = "detected_at", nullable = false, updatable = false)
	private Instant detectedAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Column(nullable = false, columnDefinition = "text")
	private String explanation;

	@Version
	@Column(nullable = false)
	private long version;

	@ManyToMany
	@JoinTable(
			name = "conflict_interventions",
			joinColumns = @JoinColumn(name = "conflict_id"),
			inverseJoinColumns = @JoinColumn(name = "intervention_id"))
	private Set<Intervention> interventions = new LinkedHashSet<>();

	public String getConflictNumber() { return conflictNumber; }
	public RoadSegment getRoadSegment() { return roadSegment; }
	public Type getType() { return type; }
	public Severity getSeverity() { return severity; }
	public Status getStatus() { return status; }
	public Instant getDetectedAt() { return detectedAt; }
	public Instant getResolvedAt() { return resolvedAt; }
	public String getExplanation() { return explanation; }
	public long getVersion() { return version; }
	public Set<Intervention> getInterventions() { return Set.copyOf(interventions); }
}
