package com.civicos.casefile.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.civicos.casefile.domain.CitizenObservation;

public interface CitizenObservationRepository extends JpaRepository<CitizenObservation, UUID> {
	List<CitizenObservation> findByCivicCaseIdOrderBySubmittedAtAsc(UUID caseId);
	List<CitizenObservation> findByRoadSegmentIdAndStatus(UUID roadSegmentId, CitizenObservation.Status status);
	Page<CitizenObservation> findBySubmittedById(UUID submittedById, Pageable pageable);
	boolean existsByCivicCaseIdAndSubmittedById(UUID civicCaseId, UUID submittedById);
}
