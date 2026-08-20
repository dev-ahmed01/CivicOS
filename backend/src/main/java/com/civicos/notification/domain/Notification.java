package com.civicos.notification.domain;

import java.time.Instant;
import java.util.UUID;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification extends AbstractCreatedEntity {

	@Column(name = "notification_type", nullable = false, length = 60)
	private String type;

	@ManyToOne(optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String message;

	@Column(name = "target_type", length = 60)
	private String targetType;

	@Column(name = "target_id")
	private UUID targetId;

	@Column(name = "read_at")
	private Instant readAt;

	public String getType() { return type; }
	public User getRecipient() { return recipient; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public Instant getReadAt() { return readAt; }
}
