package com.civicos.notification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientId);
	long countByRecipientIdAndReadAtIsNull(UUID recipientId);
}
