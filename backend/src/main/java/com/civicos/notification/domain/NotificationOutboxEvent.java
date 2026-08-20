package com.civicos.notification.domain;

import java.time.Instant;
import java.util.UUID;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.validation.ValidationRules;
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
@Table(name = "notification_outbox")
public class NotificationOutboxEvent extends AbstractCreatedEntity {

	public enum Status { PENDING, RETRY_PENDING, DELIVERED, FAILED }

	@Column(name = "event_key", nullable = false, unique = true, updatable = false, length = 200)
	private String eventKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, updatable = false, length = 60)
	private Notification.Type type;

	@ManyToOne(optional = false)
	@JoinColumn(name = "recipient_id", nullable = false, updatable = false)
	private User recipient;

	@Column(nullable = false, updatable = false, length = 200)
	private String title;

	@Column(nullable = false, updatable = false, columnDefinition = "text")
	private String message;

	@Column(name = "target_type", updatable = false, length = 60)
	private String targetType;

	@Column(name = "target_id", updatable = false)
	private UUID targetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.PENDING;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	@Column(name = "last_error", columnDefinition = "text")
	private String lastError;

	@Version
	@Column(nullable = false)
	private long version;

	protected NotificationOutboxEvent() {
	}

	public static NotificationOutboxEvent request(
			String eventKey,
			Notification.Type type,
			User recipient,
			String title,
			String message,
			String targetType,
			UUID targetId,
			Instant requestedAt) {
		Notification template = Notification.inApp(
				type, recipient, title, message, targetType, targetId);
		NotificationOutboxEvent event = new NotificationOutboxEvent();
		event.eventKey = ValidationRules.requiredText(eventKey, "Notification event key");
		if (event.eventKey.length() > 200) {
			throw new com.civicos.common.domain.DomainValidationException(
					"Notification event key cannot exceed 200 characters.");
		}
		event.type = template.getType();
		event.recipient = template.getRecipient();
		event.title = template.getTitle();
		event.message = template.getMessage();
		event.targetType = template.getTargetType();
		event.targetId = template.getTargetId();
		event.nextAttemptAt = ValidationRules.required(requestedAt, "Notification request time");
		return event;
	}

	public boolean isDue(Instant now) {
		return (status == Status.PENDING || status == Status.RETRY_PENDING)
				&& !nextAttemptAt.isAfter(now);
	}

	public void markDelivered(Instant occurredAt) {
		if (status == Status.DELIVERED) {
			return;
		}
		status = Status.DELIVERED;
		deliveredAt = ValidationRules.required(occurredAt, "Notification delivery time");
		lastError = null;
		attemptCount++;
	}

	public void markFailed(String error, Instant nextAttemptAt, boolean terminal) {
		this.attemptCount++;
		this.lastError = ValidationRules.requiredText(error, "Notification delivery error");
		this.status = terminal ? Status.FAILED : Status.RETRY_PENDING;
		this.nextAttemptAt = ValidationRules.required(nextAttemptAt, "Notification retry time");
	}

	public String getEventKey() { return eventKey; }
	public Notification.Type getType() { return type; }
	public User getRecipient() { return recipient; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public Status getStatus() { return status; }
	public int getAttemptCount() { return attemptCount; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public Instant getDeliveredAt() { return deliveredAt; }
	public String getLastError() { return lastError; }
	public long getVersion() { return version; }
}
