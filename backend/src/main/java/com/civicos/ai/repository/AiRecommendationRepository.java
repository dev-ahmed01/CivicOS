package com.civicos.ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.ai.domain.AiRecommendation;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {
	List<AiRecommendation> findByConflictIdAndStatus(UUID conflictId, AiRecommendation.Status status);
}
