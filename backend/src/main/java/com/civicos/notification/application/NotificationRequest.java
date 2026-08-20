package com.civicos.notification.application;

import java.util.UUID;

import com.civicos.notification.domain.Notification;

public record NotificationRequest(
		String eventKey,
		Notification.Type type,
		UUID recipientId,
		String title,
		String message,
		String targetType,
		UUID targetId) {
}
