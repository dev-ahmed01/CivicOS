package com.civicos.notification.application;

import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.common.validation.ValidationRules;
import com.civicos.notification.domain.NotificationOutboxEvent;
import com.civicos.notification.repository.NotificationOutboxRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class NotificationOutboxService {

	private final NotificationOutboxRepository outboxRepository;
	private final UserRepository userRepository;
	private final Clock clock;
	private final EntityManager entityManager;

	public NotificationOutboxService(
			NotificationOutboxRepository outboxRepository,
			UserRepository userRepository,
			Clock clock,
			EntityManager entityManager) {
		this.outboxRepository = outboxRepository;
		this.userRepository = userRepository;
		this.clock = clock;
		this.entityManager = entityManager;
	}

	@Transactional
	public UUID enqueue(NotificationRequest request) {
		ValidationRules.required(request, "Notification request");
		String eventKey = ValidationRules.requiredText(request.eventKey(), "Notification event key");
		entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
				.setParameter("lockId", (long) eventKey.hashCode())
				.getSingleResult();
		return outboxRepository.findByEventKey(eventKey)
				.map(NotificationOutboxEvent::getId)
				.orElseGet(() -> create(request, eventKey));
	}

	private UUID create(NotificationRequest request, String eventKey) {
		User recipient = userRepository.findById(request.recipientId())
				.orElseThrow(() -> new NoSuchElementException(
						"Notification recipient not found: " + request.recipientId()));
		NotificationOutboxEvent event = NotificationOutboxEvent.request(
				eventKey, request.type(), recipient, request.title(), request.message(),
				request.targetType(), request.targetId(), clock.instant());
		outboxRepository.saveAndFlush(event);
		return event.getId();
	}
}
