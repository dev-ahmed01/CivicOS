package com.civicos.notification.domain;

import java.time.Instant;
import java.util.UUID;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.validation.ValidationRules;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notifications")
public class Notification extends AbstractCreatedEntity {
	public enum Type {
		INTERVENTION_SUBMITTED,
		CONFLICT_DETECTED,
		TASK_ASSIGNED,
		APPROVAL_PENDING,
		DEADLINE_APPROACHING,
		SLA_BREACHED,
		DEPENDENCY_BLOCKED,
		WORK_STARTED,
		RESTORATION_PENDING,
		EVIDENCE_SUBMITTED,
		VERIFICATION_REQUESTED,
		VERIFICATION_FAILED,
		CITIZEN_VALIDATION,
		INTERVENTION_REOPENED
	}

	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(name = "notification_type", nullable = false, updatable = false, length = 60)
	private Type type;

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

	@Column(name = "read_at")
	private Instant readAt;

	@Version
	@Column(nullable = false)
	private long version;

	protected Notification() {
	}

	public static Notification inApp(
			Type type,
			User recipient,
			String title,
			String message,
			String targetType,
			UUID targetId) {
		Notification notification = new Notification();
		notification.type = ValidationRules.required(type, "Notification type");
		notification.recipient = ValidationRules.required(recipient, "Notification recipient");
		notification.title = boundedText(title, "Notification title", 200);
		notification.message = ValidationRules.requiredText(message, "Notification message");
		if ((targetType == null) != (targetId == null)) {
			throw new com.civicos.common.domain.DomainValidationException(
					"Notification target type and id must be supplied together.");
		}
		notification.targetType = targetType == null
				? null : boundedText(targetType, "Notification target type", 60).toUpperCase();
		notification.targetId = targetId;
		return notification;
	}

	public boolean markRead(Instant occurredAt) {
		if (readAt != null) {
			return false;
		}
		readAt = ValidationRules.required(occurredAt, "Notification read time");
		return true;
	}

	private static String boundedText(String value, String fieldName, int maximumLength) {
		String normalized = ValidationRules.requiredText(value, fieldName);
		if (normalized.length() > maximumLength) {
			throw new com.civicos.common.domain.DomainValidationException(
					fieldName + " cannot exceed " + maximumLength + " characters.");
		}
		return normalized;
	}

	public Type getType() { return type; }
	public User getRecipient() { return recipient; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public Instant getReadAt() { return readAt; }
	public long getVersion() { return version; }
}
