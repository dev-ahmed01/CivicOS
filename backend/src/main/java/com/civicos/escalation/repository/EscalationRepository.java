package com.civicos.escalation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.escalation.domain.Escalation;

public interface EscalationRepository extends JpaRepository<Escalation, UUID> {
	List<Escalation> findBySlaIdOrderByLevelAsc(UUID slaId);
	List<Escalation> findByStatus(Escalation.Status status);
}
