package com.civicos.inspection.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.inspection.domain.Inspection;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {
	List<Inspection> findByInterventionIdOrderByCreatedAtAsc(UUID interventionId);
	List<Inspection> findByInspectorIdAndStatus(UUID inspectorId, Inspection.Status status);
}
