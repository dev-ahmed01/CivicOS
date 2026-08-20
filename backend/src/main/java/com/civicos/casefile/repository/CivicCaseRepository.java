package com.civicos.casefile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.casefile.domain.CivicCase;

public interface CivicCaseRepository extends JpaRepository<CivicCase, UUID> {
	Optional<CivicCase> findByCaseNumber(String caseNumber);
	List<CivicCase> findByRoadSegmentIdAndStatus(UUID roadSegmentId, CivicCase.Status status);
}
