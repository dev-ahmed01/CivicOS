package com.civicos.sla.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.sla.domain.Sla;

public interface SlaRepository extends JpaRepository<Sla, UUID> {
	List<Sla> findByTargetTypeAndTargetId(String targetType, UUID targetId);
	List<Sla> findByStatusInAndDeadlineBefore(List<Sla.Status> statuses, Instant deadline);
}
