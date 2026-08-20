package com.civicos.notification.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.notification.domain.Notification;

public record NotificationResult(
		UUID notificationId,
		Notification.Type type,
		String title,
		String message,
		String targetType,
		UUID targetId,
		Instant readAt,
		long version,
		Instant createdAt) {
}
