package com.civicos.ai.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.ai.repository.AiRecommendationRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.conflict.repository.ConflictRepository;

@Service
public class AiRecommendationQueryService {

	private final AiRecommendationRepository recommendationRepository;
	private final ConflictRepository conflictRepository;
	private final ScopedAuthorizationService authorizationService;

	public AiRecommendationQueryService(
			AiRecommendationRepository recommendationRepository,
			ConflictRepository conflictRepository,
			ScopedAuthorizationService authorizationService) {
		this.recommendationRepository = recommendationRepository;
		this.conflictRepository = conflictRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public List<AiRecommendationResult> forConflict(UUID conflictId) {
		authorize();
		if (!conflictRepository.existsById(conflictId)) {
			throw new NoSuchElementException("Conflict not found: " + conflictId);
		}
		return recommendationRepository.findByConflictIdOrderByCreatedAtDesc(conflictId).stream()
				.map(AiRecommendationResult::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AiRecommendationResult byId(UUID recommendationId) {
		authorize();
		return recommendationRepository.findById(recommendationId)
				.map(AiRecommendationResult::from)
				.orElseThrow(() -> new NoSuchElementException(
						"AI recommendation not found: " + recommendationId));
	}

	private void authorize() {
		authorizationService.authorize(
				PermissionCode.COORDINATION_VIEW, null, SystemRole.COORDINATOR);
	}
}
