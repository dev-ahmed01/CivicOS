package com.civicos.inspection.domain;

import java.time.Instant;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.intervention.domain.Intervention;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inspections")
public class Inspection extends AbstractCreatedEntity {

	public enum Status { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }
	public enum Result { PASSED, FAILED, CONDITIONAL }

	@ManyToOne(optional = false)
	@JoinColumn(name = "intervention_id", nullable = false)
	private Intervention intervention;

	@ManyToOne(optional = false)
	@JoinColumn(name = "inspector_id", nullable = false)
	private User inspector;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.SCHEDULED;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private Result result;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(columnDefinition = "text")
	private String notes;

	@Version
	@Column(nullable = false)
	private long version;

	protected Inspection() {
	}

	public static Inspection schedule(Intervention intervention, User inspector) {
		Inspection inspection = new Inspection();
		inspection.intervention = ValidationRules.required(intervention, "Intervention");
		inspection.inspector = ValidationRules.required(inspector, "Inspector");
		return inspection;
	}

	public void start(Instant startedAt) {
		if (status != Status.SCHEDULED) {
			throw new DomainConflictException("Only a scheduled inspection can be started.");
		}
		this.status = Status.IN_PROGRESS;
		this.startedAt = ValidationRules.required(startedAt, "Inspection start time");
	}

	public void complete(Result result, String notes, Instant completedAt) {
		if (status != Status.IN_PROGRESS) {
			throw new DomainConflictException("Only an in-progress inspection can be completed.");
		}
		if (result == null) {
			throw new DomainValidationException("Inspection result is required.");
		}
		if (result != Result.PASSED && (notes == null || notes.isBlank())) {
			throw new DomainValidationException("Inspection notes are required for a non-passing result.");
		}
		Instant completion = ValidationRules.required(completedAt, "Inspection completion time");
		if (completion.isBefore(startedAt)) {
			throw new DomainValidationException("Inspection completion cannot precede its start.");
		}
		this.result = result;
		this.notes = notes == null || notes.isBlank() ? null : notes.strip();
		this.completedAt = completion;
		this.status = Status.COMPLETED;
	}

	public Intervention getIntervention() { return intervention; }
	public User getInspector() { return inspector; }
	public Status getStatus() { return status; }
	public Result getResult() { return result; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public String getNotes() { return notes; }
	public long getVersion() { return version; }
}
