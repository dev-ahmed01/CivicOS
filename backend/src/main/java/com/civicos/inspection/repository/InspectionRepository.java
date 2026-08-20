package com.civicos.inspection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.inspection.domain.Inspection;

import jakarta.persistence.LockModeType;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {
	List<Inspection> findByInterventionIdOrderByCreatedAtAsc(UUID interventionId);
	List<Inspection> findByInspectorIdAndStatus(UUID inspectorId, Inspection.Status status);
	Optional<Inspection> findFirstByInterventionIdAndStatusOrderByCreatedAtDesc(
			UUID interventionId, Inspection.Status status);
	boolean existsByInterventionIdAndStatusIn(UUID interventionId, List<Inspection.Status> statuses);
	boolean existsByInterventionIdAndInspectorId(UUID interventionId, UUID inspectorId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select inspection from Inspection inspection where inspection.id = :id")
	Optional<Inspection> findForUpdate(@Param("id") UUID id);
}
