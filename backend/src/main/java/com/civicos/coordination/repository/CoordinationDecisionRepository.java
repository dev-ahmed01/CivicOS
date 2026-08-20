package com.civicos.coordination.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.coordination.domain.CoordinationDecision;

public interface CoordinationDecisionRepository extends JpaRepository<CoordinationDecision, UUID> {
	List<CoordinationDecision> findByConflictIdOrderByCreatedAtAsc(UUID conflictId);
}
