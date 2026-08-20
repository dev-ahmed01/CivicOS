package com.civicos.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.notification.domain.Notification;
import com.civicos.notification.repository.NotificationRepository;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final AuthorizationService authorizationService;
	private final Clock clock;

	public NotificationService(
			NotificationRepository notificationRepository,
			AuthorizationService authorizationService,
			Clock clock) {
		this.notificationRepository = notificationRepository;
		this.authorizationService = authorizationService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<NotificationResult> listForCurrentUser(boolean unreadOnly) {
		UUID recipientId = authorizationService.currentPrincipal().userId();
		List<Notification> notifications = unreadOnly
				? notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipientId)
				: notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
		return notifications.stream().map(this::result).toList();
	}

	@Transactional(readOnly = true)
	public Page<NotificationResult> listForCurrentUser(boolean unreadOnly, Pageable pageable) {
		UUID recipientId = authorizationService.currentPrincipal().userId();
		Page<Notification> notifications = unreadOnly
				? notificationRepository.findByRecipientIdAndReadAtIsNull(recipientId, pageable)
				: notificationRepository.findByRecipientId(recipientId, pageable);
		return notifications.map(this::result);
	}

	@Transactional(readOnly = true)
	public long unreadCount() {
		return notificationRepository.countByRecipientIdAndReadAtIsNull(
				authorizationService.currentPrincipal().userId());
	}

	@Transactional
	public NotificationResult markRead(UUID notificationId) {
		Notification notification = notificationRepository.findForUpdate(notificationId)
				.orElseThrow(() -> new NoSuchElementException(
						"Notification not found: " + notificationId));
		CivicPrincipal principal = authorizationService.currentPrincipal();
		if (!notification.getRecipient().getId().equals(principal.userId())) {
			throw new AccessDeniedException("A notification can be read only by its recipient.");
		}
		notification.markRead(clock.instant());
		notificationRepository.saveAndFlush(notification);
		return result(notification);
	}

	@Transactional
	public int markAllRead() {
		UUID recipientId = authorizationService.currentPrincipal().userId();
		List<Notification> unread = notificationRepository
				.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipientId);
		Instant occurredAt = clock.instant();
		unread.forEach(notification -> notification.markRead(occurredAt));
		notificationRepository.saveAll(unread);
		return unread.size();
	}

	private NotificationResult result(Notification notification) {
		return new NotificationResult(
				notification.getId(), notification.getType(), notification.getTitle(),
				notification.getMessage(), notification.getTargetType(), notification.getTargetId(),
				notification.getReadAt(), notification.getVersion(), notification.getCreatedAt());
	}
}
