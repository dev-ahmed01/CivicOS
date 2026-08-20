package com.civicos.audit.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.civicos.common.persistence.AbstractUuidEntity;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEvent extends AbstractUuidEntity {

	@Column(name = "event_id", nullable = false, unique = true, updatable = false)
	private UUID eventId = UUID.randomUUID();

	@ManyToOne
	@JoinColumn(name = "actor_id", updatable = false)
	private User actor;

	@Column(nullable = false, updatable = false, length = 100)
	private String action;

	@Column(name = "entity_type", nullable = false, updatable = false, length = 100)
	private String entityType;

	@Column(name = "entity_id", nullable = false, updatable = false)
	private UUID entityId;

	@CreationTimestamp
	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_state", updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> beforeState;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_state", updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> afterState;

	@Column(updatable = false, columnDefinition = "text")
	private String reason;

	@Column(name = "request_id", updatable = false)
	private UUID requestId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata = new LinkedHashMap<>();

	protected AuditEvent() {
	}

	public static AuditEvent securityEvent(
			User actor,
			String action,
			String entityType,
			UUID entityId,
			String requestId) {
		AuditEvent event = new AuditEvent();
		event.actor = actor;
		event.action = action;
		event.entityType = entityType;
		event.entityId = entityId;
		event.requestId = parseUuid(requestId);
		event.metadata.put("category", "SECURITY");
		if (requestId != null) {
			event.metadata.put("correlationId", requestId);
		}
		return event;
	}

	private static UUID parseUuid(String value) {
		try {
			return value == null ? null : UUID.fromString(value);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public UUID getEventId() { return eventId; }
	public User getActor() { return actor; }
	public String getAction() { return action; }
	public String getEntityType() { return entityType; }
	public UUID getEntityId() { return entityId; }
	public Instant getOccurredAt() { return occurredAt; }
	public Map<String, Object> getBeforeState() { return beforeState == null ? null : Map.copyOf(beforeState); }
	public Map<String, Object> getAfterState() { return afterState == null ? null : Map.copyOf(afterState); }
	public String getReason() { return reason; }
	public UUID getRequestId() { return requestId; }
	public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
}
