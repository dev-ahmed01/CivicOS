package com.civicos.casefile.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.casefile.domain.CitizenObservation;

public interface CitizenObservationRepository extends JpaRepository<CitizenObservation, UUID> {
	List<CitizenObservation> findByCivicCaseIdOrderBySubmittedAtAsc(UUID caseId);
	List<CitizenObservation> findByRoadSegmentIdAndStatus(UUID roadSegmentId, CitizenObservation.Status status);
	boolean existsByCivicCaseIdAndSubmittedById(UUID civicCaseId, UUID submittedById);
}
