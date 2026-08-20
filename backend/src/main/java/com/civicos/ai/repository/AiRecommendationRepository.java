package com.civicos.ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.civicos.ai.domain.AiRecommendation;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {
	List<AiRecommendation> findByConflictIdAndStatus(UUID conflictId, AiRecommendation.Status status);
	List<AiRecommendation> findByConflictIdOrderByCreatedAtDesc(UUID conflictId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	java.util.Optional<AiRecommendation> findForUpdateById(UUID id);
}
