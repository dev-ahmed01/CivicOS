package com.civicos.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.notification.domain.Notification;

import jakarta.persistence.LockModeType;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientId);
	List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
	long countByRecipientIdAndReadAtIsNull(UUID recipientId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select notification from Notification notification where notification.id = :id")
	Optional<Notification> findForUpdate(@Param("id") UUID id);
}
