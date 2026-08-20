package com.civicos.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.notification.config.NotificationProperties;
import com.civicos.notification.domain.Notification;
import com.civicos.notification.domain.NotificationOutboxEvent;
import com.civicos.notification.repository.NotificationOutboxRepository;
import com.civicos.notification.repository.NotificationRepository;

@Service
public class NotificationOutboxProcessor {

	private final NotificationOutboxRepository outboxRepository;
	private final NotificationRepository notificationRepository;
	private final NotificationProperties properties;
	private final Clock clock;

	public NotificationOutboxProcessor(
			NotificationOutboxRepository outboxRepository,
			NotificationRepository notificationRepository,
			NotificationProperties properties,
			Clock clock) {
		this.outboxRepository = outboxRepository;
		this.notificationRepository = notificationRepository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(UUID eventId) {
		NotificationOutboxEvent event = outboxRepository.findForUpdate(eventId).orElse(null);
		Instant now = clock.instant();
		if (event == null || !event.isDue(now)) {
			return;
		}
		try {
			Notification notification = Notification.inApp(
					event.getType(), event.getRecipient(), event.getTitle(), event.getMessage(),
					event.getTargetType(), event.getTargetId());
			notificationRepository.saveAndFlush(notification);
			event.markDelivered(now);
			outboxRepository.saveAndFlush(event);
		} catch (RuntimeException exception) {
			boolean terminal = event.getAttemptCount() + 1 >= properties.getMaxAttempts();
			long delay = properties.getRetryDelaySeconds()
					* Math.max(1L, 1L << Math.min(event.getAttemptCount(), 10));
			event.markFailed(safeError(exception), now.plus(delay, ChronoUnit.SECONDS), terminal);
			outboxRepository.save(event);
		}
	}

	private String safeError(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message.length() > 1_000 ? message.substring(0, 1_000) : message;
	}
}
