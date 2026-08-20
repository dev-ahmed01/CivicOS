package com.civicos.approval.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "approvals")
public class Approval extends AbstractCreatedEntity {

	public enum Status { PENDING, APPROVED, APPROVED_WITH_CONDITIONS, REJECTED, RETURNED }
	public enum Decision { APPROVE, APPROVE_WITH_CONDITIONS, REJECT, RETURN }

	@ManyToOne(optional = false)
	@JoinColumn(name = "intervention_id", nullable = false)
	private Intervention intervention;

	@ManyToOne
	@JoinColumn(name = "actor_id")
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private Status status = Status.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(length = 40)
	private Decision decision;

	@Column(columnDefinition = "text")
	private String reason;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private List<Map<String, Object>> conditions = new ArrayList<>();

	@Column(name = "decided_at")
	private Instant decidedAt;

	@Version
	@Column(nullable = false)
	private long version;

	protected Approval() {
	}

	public static Approval request(Intervention intervention) {
		Approval approval = new Approval();
		approval.intervention = ValidationRules.required(intervention, "Intervention");
		return approval;
	}

	public void decide(
			Decision decision,
			User actor,
			String reason,
			List<Map<String, Object>> conditions,
			Instant decidedAt) {
		if (status != Status.PENDING) {
			throw new DomainConflictException("The approval has already been decided.");
		}
		Decision validatedDecision = ValidationRules.required(decision, "Approval decision");
		User validatedActor = ValidationRules.required(actor, "Approval actor");
		Instant validatedDecidedAt = ValidationRules.required(decidedAt, "Decision time");
		String validatedReason = reason == null || reason.isBlank() ? null : reason.strip();
		List<Map<String, Object>> validatedConditions = immutableConditions(conditions);
		if ((validatedDecision == Decision.REJECT || validatedDecision == Decision.RETURN)
				&& validatedReason == null) {
			throw new DomainValidationException("A rejection or return requires a reason.");
		}
		if (validatedDecision == Decision.APPROVE_WITH_CONDITIONS
				&& validatedConditions.isEmpty()) {
			throw new DomainValidationException("Conditional approval requires at least one condition.");
		}
		this.decision = validatedDecision;
		this.actor = validatedActor;
		this.decidedAt = validatedDecidedAt;
		this.reason = validatedReason;
		this.conditions = validatedConditions;
		this.status = switch (validatedDecision) {
			case APPROVE -> Status.APPROVED;
			case APPROVE_WITH_CONDITIONS -> Status.APPROVED_WITH_CONDITIONS;
			case REJECT -> Status.REJECTED;
			case RETURN -> Status.RETURNED;
		};
	}

	public Intervention getIntervention() { return intervention; }
	public User getActor() { return actor; }
	public Status getStatus() { return status; }
	public Decision getDecision() { return decision; }
	public String getReason() { return reason; }
	public List<Map<String, Object>> getConditions() { return List.copyOf(conditions); }
	public Instant getDecidedAt() { return decidedAt; }
	public long getVersion() { return version; }

	private List<Map<String, Object>> immutableConditions(List<Map<String, Object>> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().map(Map::copyOf).toList();
	}
}
