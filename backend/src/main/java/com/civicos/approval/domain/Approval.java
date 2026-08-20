package com.civicos.approval.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.civicos.common.persistence.AbstractCreatedEntity;
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

	public Intervention getIntervention() { return intervention; }
	public User getActor() { return actor; }
	public Status getStatus() { return status; }
	public Decision getDecision() { return decision; }
	public String getReason() { return reason; }
	public List<Map<String, Object>> getConditions() { return List.copyOf(conditions); }
	public Instant getDecidedAt() { return decidedAt; }
	public long getVersion() { return version; }
}
