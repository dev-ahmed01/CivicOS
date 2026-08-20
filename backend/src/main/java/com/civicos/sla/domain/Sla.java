package com.civicos.sla.domain;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

import com.civicos.common.persistence.AbstractUuidEntity;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.validation.ValidationRules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sla_instances")
public class Sla extends AbstractUuidEntity {

	public enum Type {
		REVIEW, COORDINATION, APPROVAL, PRE_WORK, EXECUTION,
		EVIDENCE, VERIFICATION, CORRECTION, CLOSURE
	}
	public enum Status { NORMAL, AT_RISK, BREACHED, PAUSED, COMPLETED }

	@Column(name = "target_type", nullable = false, length = 60)
	private String targetType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@Enumerated(EnumType.STRING)
	@Column(name = "sla_type", nullable = false, length = 60)
	private Type slaType;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(nullable = false)
	private Instant deadline;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.NORMAL;

	@Column(name = "paused_at")
	private Instant pausedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Version
	@Column(nullable = false)
	private long version;

	protected Sla() {
	}

	public static Sla create(
			String targetType,
			UUID targetId,
			Type slaType,
			Instant startAt,
			Instant deadline) {
		Sla sla = new Sla();
		sla.targetType = ValidationRules.requiredText(targetType, "SLA target type").toUpperCase();
		sla.targetId = ValidationRules.required(targetId, "SLA target id");
		sla.slaType = ValidationRules.required(slaType, "SLA type");
		sla.startAt = ValidationRules.required(startAt, "SLA start time");
		sla.deadline = ValidationRules.required(deadline, "SLA deadline");
		if (!deadline.isAfter(startAt)) {
			throw new com.civicos.common.domain.DomainValidationException(
					"SLA deadline must be after its start time.");
		}
		return sla;
	}

	public boolean assess(Instant now, Duration atRiskBefore) {
		if (status == Status.COMPLETED || status == Status.PAUSED || status == Status.BREACHED) {
			return false;
		}
		Status next = !now.isBefore(deadline)
				? Status.BREACHED
				: !now.isBefore(deadline.minus(atRiskBefore)) ? Status.AT_RISK : Status.NORMAL;
		if (next == status) {
			return false;
		}
		status = next;
		return true;
	}

	public void pause(Instant occurredAt) {
		if (status == Status.COMPLETED || status == Status.PAUSED || status == Status.BREACHED) {
			throw new DomainConflictException("Only an active SLA can be paused.");
		}
		status = Status.PAUSED;
		pausedAt = ValidationRules.required(occurredAt, "Pause time");
	}

	public void resume(Instant occurredAt) {
		if (status != Status.PAUSED) {
			throw new DomainConflictException("Only a PAUSED SLA can be resumed.");
		}
		Instant resumedAt = ValidationRules.required(occurredAt, "Resume time");
		deadline = deadline.plus(Duration.between(pausedAt, resumedAt));
		pausedAt = null;
		status = Status.NORMAL;
	}

	public void complete(Instant occurredAt) {
		if (status == Status.COMPLETED) {
			throw new DomainConflictException("The SLA is already complete.");
		}
		status = Status.COMPLETED;
		pausedAt = null;
		completedAt = ValidationRules.required(occurredAt, "Completion time");
	}

	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public Type getSlaType() { return slaType; }
	public Instant getStartAt() { return startAt; }
	public Instant getDeadline() { return deadline; }
	public Status getStatus() { return status; }
	public Instant getPausedAt() { return pausedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public long getVersion() { return version; }
}
