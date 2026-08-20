package com.civicos.approval.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.approval.domain.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
	List<Approval> findByInterventionIdOrderByCreatedAtAsc(UUID interventionId);
	Optional<Approval> findByInterventionIdAndStatus(UUID interventionId, Approval.Status status);
}
