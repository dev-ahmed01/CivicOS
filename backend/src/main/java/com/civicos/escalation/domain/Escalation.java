package com.civicos.escalation.domain;

import java.time.Instant;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.sla.domain.Sla;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "escalations")
public class Escalation extends AbstractCreatedEntity {

	public enum Status { OPEN, ACKNOWLEDGED, RESOLVED }

	@ManyToOne(optional = false)
	@JoinColumn(name = "sla_id", nullable = false)
	private Sla sla;

	@Column(nullable = false)
	private int level;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.OPEN;

	@Column(nullable = false, columnDefinition = "text")
	private String reason;

	@ManyToOne
	@JoinColumn(name = "escalated_to")
	private User escalatedTo;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	public Sla getSla() { return sla; }
	public int getLevel() { return level; }
	public Status getStatus() { return status; }
	public String getReason() { return reason; }
	public User getEscalatedTo() { return escalatedTo; }
	public Instant getResolvedAt() { return resolvedAt; }
}
