package com.civicos.audit.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.audit.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
	Optional<AuditEvent> findByEventId(UUID eventId);
	List<AuditEvent> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);
	Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);
}
