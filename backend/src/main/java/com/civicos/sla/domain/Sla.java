package com.civicos.sla.domain;

import java.time.Instant;
import java.util.UUID;

import com.civicos.common.persistence.AbstractUuidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sla_instances")
public class Sla extends AbstractUuidEntity {

	public enum Status { NORMAL, AT_RISK, BREACHED, PAUSED, COMPLETED }

	@Column(name = "target_type", nullable = false, length = 60)
	private String targetType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@Column(name = "sla_type", nullable = false, length = 60)
	private String slaType;

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

	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public String getSlaType() { return slaType; }
	public Instant getStartAt() { return startAt; }
	public Instant getDeadline() { return deadline; }
	public Status getStatus() { return status; }
	public Instant getPausedAt() { return pausedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public long getVersion() { return version; }
}
