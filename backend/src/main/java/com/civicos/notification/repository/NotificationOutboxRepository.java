package com.civicos.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.notification.domain.NotificationOutboxEvent;

import jakarta.persistence.LockModeType;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, UUID> {
	Optional<NotificationOutboxEvent> findByEventKey(String eventKey);

	@Query("""
			select event.id from NotificationOutboxEvent event
			where event.status in :statuses and event.nextAttemptAt <= :now
			order by event.nextAttemptAt asc, event.createdAt asc
			""")
	List<UUID> findDueIds(
			@Param("statuses") List<NotificationOutboxEvent.Status> statuses,
			@Param("now") Instant now,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select event from NotificationOutboxEvent event where event.id = :id")
	Optional<NotificationOutboxEvent> findForUpdate(@Param("id") UUID id);
}
