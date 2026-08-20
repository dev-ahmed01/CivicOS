package com.civicos.notification.delivery;

import java.util.UUID;

import com.civicos.notification.domain.Notification;

public record NotificationDelivery(
		UUID recipientId,
		String recipientAddress,
		Notification.Type type,
		String title,
		String message,
		String targetType,
		UUID targetId) {
}
