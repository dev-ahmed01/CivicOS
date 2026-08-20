package com.civicos.notification.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.civicos.notification.config.NotificationProperties;
import com.civicos.notification.domain.NotificationOutboxEvent;
import com.civicos.notification.repository.NotificationOutboxRepository;

@Service
public class NotificationDispatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDispatcher.class);

	private static final List<NotificationOutboxEvent.Status> DUE_STATUSES = List.of(
			NotificationOutboxEvent.Status.PENDING,
			NotificationOutboxEvent.Status.RETRY_PENDING);

	private final NotificationOutboxRepository outboxRepository;
	private final NotificationOutboxProcessor processor;
	private final NotificationProperties properties;
	private final Clock clock;

	public NotificationDispatcher(
			NotificationOutboxRepository outboxRepository,
			NotificationOutboxProcessor processor,
			NotificationProperties properties,
			Clock clock) {
		this.outboxRepository = outboxRepository;
		this.processor = processor;
		this.properties = properties;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${civicos.notification.dispatcher-interval-ms:5000}")
	public void dispatchDue() {
		if (!properties.isDispatcherEnabled()) {
			return;
		}
		List<UUID> dueIds = outboxRepository.findDueIds(
				DUE_STATUSES, clock.instant(), PageRequest.of(0, properties.getBatchSize()));
		for (UUID dueId : dueIds) {
			try {
				processor.process(dueId);
			} catch (RuntimeException exception) {
				LOGGER.error("notification_outbox_dispatch_failed eventId={}", dueId, exception);
			}
		}
	}
}
