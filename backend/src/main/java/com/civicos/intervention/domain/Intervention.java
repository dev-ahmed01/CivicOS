package com.civicos.intervention.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;

import com.civicos.agency.domain.Agency;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.common.persistence.AbstractAuditableEntity;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.GeometryRules;
import com.civicos.common.validation.ValidationRules;
import com.civicos.road.domain.RoadSegment;
import com.civicos.user.domain.User;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "interventions")
public class Intervention extends AbstractAuditableEntity {

	public enum Type {
		UTILITY_EXCAVATION, WATER, SEWER, ELECTRICAL, TELECOM,
		DRAINAGE, RESTORATION, RESURFACING, ROADWORK, OTHER
	}

	public enum Status {
		DRAFT, SUBMITTED, UNDER_REVIEW, ANALYSIS, COORDINATION_REQUIRED,
		COORDINATION_COMPLETE, APPROVAL_PENDING, APPROVED, SCHEDULED,
		IN_PROGRESS, RESTORATION, EVIDENCE_PENDING, VERIFICATION_PENDING,
		VERIFIED, CLOSED, REJECTED, CANCELLED, ON_HOLD, REOPENED
	}

	public enum WorkflowAction {
		SUBMIT(Status.DRAFT, Status.SUBMITTED),
		BEGIN_REVIEW(Status.SUBMITTED, Status.UNDER_REVIEW),
		ANALYSE(Status.UNDER_REVIEW, Status.ANALYSIS),
		REQUIRE_COORDINATION(Status.ANALYSIS, Status.COORDINATION_REQUIRED),
		COMPLETE_COORDINATION(Status.COORDINATION_REQUIRED, Status.COORDINATION_COMPLETE),
		REQUEST_APPROVAL(Status.COORDINATION_COMPLETE, Status.APPROVAL_PENDING),
		APPROVE(Status.APPROVAL_PENDING, Status.APPROVED),
		REJECT(Status.APPROVAL_PENDING, Status.REJECTED),
		RETURN_FOR_COORDINATION(Status.APPROVAL_PENDING, Status.COORDINATION_REQUIRED),
		SCHEDULE(Status.APPROVED, Status.SCHEDULED),
		START(Status.SCHEDULED, Status.IN_PROGRESS),
		COMPLETE(Status.IN_PROGRESS, Status.RESTORATION),
		COMPLETE_RESTORATION(Status.RESTORATION, Status.EVIDENCE_PENDING),
		SUBMIT_EVIDENCE(Status.EVIDENCE_PENDING, Status.VERIFICATION_PENDING),
		FAIL_VERIFICATION(Status.VERIFICATION_PENDING, Status.REOPENED),
		BEGIN_CORRECTIVE_ACTION(Status.REOPENED, Status.RESTORATION),
		VERIFY(Status.VERIFICATION_PENDING, Status.VERIFIED),
		CLOSE(Status.VERIFIED, Status.CLOSED),
		REOPEN_CLOSED(Status.CLOSED, Status.REOPENED),
		REVISE_REJECTED(Status.REJECTED, Status.DRAFT),
		HOLD(EnumSet.of(
				Status.COORDINATION_REQUIRED, Status.COORDINATION_COMPLETE,
				Status.APPROVED, Status.SCHEDULED, Status.IN_PROGRESS,
				Status.RESTORATION, Status.EVIDENCE_PENDING,
				Status.VERIFICATION_PENDING), Status.ON_HOLD),
		RESUME(Status.ON_HOLD, null),
		CANCEL(EnumSet.of(
				Status.DRAFT, Status.SUBMITTED, Status.UNDER_REVIEW, Status.ANALYSIS,
				Status.COORDINATION_REQUIRED, Status.COORDINATION_COMPLETE,
				Status.APPROVAL_PENDING, Status.APPROVED, Status.SCHEDULED,
				Status.IN_PROGRESS, Status.RESTORATION, Status.EVIDENCE_PENDING,
				Status.VERIFICATION_PENDING, Status.ON_HOLD, Status.REOPENED),
				Status.CANCELLED);

		private final Set<Status> sources;
		private final Status target;

		WorkflowAction(Status source, Status target) {
			this(EnumSet.of(source), target);
		}

		WorkflowAction(Set<Status> sources, Status target) {
			this.sources = Set.copyOf(sources);
			this.target = target;
		}

		public Set<Status> sources() { return sources; }
		public boolean supports(Status status) { return sources.contains(status); }
		public Status target() { return target; }
	}

	public enum Priority { LOW, NORMAL, HIGH, CRITICAL }

	@Column(name = "intervention_number", nullable = false, unique = true, length = 50)
	private String interventionNumber;

	@ManyToOne(optional = false)
	@JoinColumn(name = "case_id", nullable = false)
	private CivicCase civicCase;

	@ManyToOne(optional = false)
	@JoinColumn(name = "agency_id", nullable = false)
	private Agency agency;

	@Enumerated(EnumType.STRING)
	@Column(name = "intervention_type", nullable = false, length = 60)
	private Type type;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@ManyToOne(optional = false)
	@JoinColumn(name = "road_segment_id", nullable = false)
	private RoadSegment roadSegment;

	@Column(nullable = false, columnDefinition = "geometry(Geometry,4326)")
	private Geometry geometry;

	@Column(name = "planned_start", nullable = false)
	private Instant plannedStart;

	@Column(name = "planned_end", nullable = false)
	private Instant plannedEnd;

	@Column(name = "actual_start")
	private Instant actualStart;

	@Column(name = "actual_end")
	private Instant actualEnd;

	@Enumerated(EnumType.STRING)
	@Column(name = "held_from_status", length = 60)
	private Status heldFromStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 60)
	private Status status = Status.DRAFT;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Priority priority = Priority.NORMAL;

	@ManyToOne(optional = false)
	@JoinColumn(name = "created_by", nullable = false, updatable = false)
	private User createdBy;

	@Version
	@Column(nullable = false)
	private long version;

	protected Intervention() {
	}

	public static Intervention create(
			String interventionNumber,
			CivicCase civicCase,
			Agency agency,
			Type type,
			String description,
			RoadSegment roadSegment,
			Geometry geometry,
			Instant plannedStart,
			Instant plannedEnd,
			Priority priority,
			User createdBy) {
		Intervention intervention = new Intervention();
		intervention.interventionNumber = ValidationRules.requiredText(
				interventionNumber, "Intervention number");
		intervention.createdBy = ValidationRules.required(createdBy, "Created by");
		intervention.applyDraftDetails(
				civicCase, agency, type, description, roadSegment,
				geometry, plannedStart, plannedEnd, priority);
		return intervention;
	}

	public void updateDraft(
			CivicCase civicCase,
			Agency agency,
			Type type,
			String description,
			RoadSegment roadSegment,
			Geometry geometry,
			Instant plannedStart,
			Instant plannedEnd,
			Priority priority) {
		if (status != Status.DRAFT) {
			throw new DomainConflictException("Only a DRAFT intervention can be edited directly.");
		}
		applyDraftDetails(
				civicCase, agency, type, description, roadSegment,
				geometry, plannedStart, plannedEnd, priority);
	}

	public String getInterventionNumber() { return interventionNumber; }
	public CivicCase getCivicCase() { return civicCase; }
	public Agency getAgency() { return agency; }
	public Type getType() { return type; }
	public String getDescription() { return description; }
	public RoadSegment getRoadSegment() { return roadSegment; }
	public Geometry getGeometry() { return geometry; }
	public Instant getPlannedStart() { return plannedStart; }
	public Instant getPlannedEnd() { return plannedEnd; }
	public Instant getActualStart() { return actualStart; }
	public Instant getActualEnd() { return actualEnd; }
	public Status getHeldFromStatus() { return heldFromStatus; }
	public Status getStatus() { return status; }
	public Priority getPriority() { return priority; }
	public User getCreatedBy() { return createdBy; }
	public long getVersion() { return version; }

	public void transition(WorkflowAction action, Instant occurredAt) {
		if (!action.supports(status)) {
			throw new WorkflowActionNotAllowedException("INTERVENTION", status.name(), action.name());
		}

		Status previousStatus = status;
		if (action == WorkflowAction.RESUME) {
			if (heldFromStatus == null) {
				throw new DomainConflictException("Held intervention has no resumable prior state.");
			}
			status = heldFromStatus;
			heldFromStatus = null;
		} else {
			status = action.target();
			if (action == WorkflowAction.HOLD) {
				heldFromStatus = previousStatus;
			} else if (action == WorkflowAction.CANCEL) {
				heldFromStatus = null;
			}
		}
		switch (action) {
			case START -> actualStart = occurredAt;
			case COMPLETE_RESTORATION -> actualEnd = occurredAt;
			case FAIL_VERIFICATION, REOPEN_CLOSED -> actualEnd = null;
			default -> {
				// The remaining transitions do not alter execution timestamps.
			}
		}
	}

	private void applyDraftDetails(
			CivicCase civicCase,
			Agency agency,
			Type type,
			String description,
			RoadSegment roadSegment,
			Geometry geometry,
			Instant plannedStart,
			Instant plannedEnd,
			Priority priority) {
		this.civicCase = ValidationRules.required(civicCase, "Civic case");
		this.agency = ValidationRules.required(agency, "Agency");
		this.type = ValidationRules.required(type, "Intervention type");
		this.description = ValidationRules.requiredText(description, "Intervention description");
		this.roadSegment = ValidationRules.required(roadSegment, "Road segment");
		this.geometry = GeometryRules.validWgs84(geometry, "Intervention geometry");
		this.plannedStart = ValidationRules.required(plannedStart, "Planned start");
		this.plannedEnd = ValidationRules.required(plannedEnd, "Planned end");
		if (!plannedEnd.isAfter(plannedStart)) {
			throw new DomainValidationException("Planned end must be after planned start.");
		}
		this.priority = ValidationRules.required(priority, "Intervention priority");
	}
}
