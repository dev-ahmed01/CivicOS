package com.civicos.audit.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import com.civicos.audit.domain.AuditEvent;

public interface AuditEventRepository extends Repository<AuditEvent, UUID> {
	<S extends AuditEvent> S save(S event);
	Optional<AuditEvent> findById(UUID id);
	Optional<AuditEvent> findByEventId(UUID eventId);
	List<AuditEvent> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);
}
