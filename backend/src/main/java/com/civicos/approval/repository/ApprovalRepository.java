package com.civicos.approval.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.civicos.approval.domain.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
	List<Approval> findByInterventionIdOrderByCreatedAtAsc(UUID interventionId);
	Optional<Approval> findByInterventionIdAndStatus(UUID interventionId, Approval.Status status);
	boolean existsByInterventionIdAndActorIdAndStatusIn(
			UUID interventionId,
			UUID actorId,
			List<Approval.Status> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select approval from Approval approval where approval.id = :id")
	Optional<Approval> findForUpdate(@Param("id") UUID id);
}
