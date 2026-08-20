package com.civicos.ai.application;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.ai.domain.AiRecommendation;
import com.civicos.ai.repository.AiRecommendationRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class AiRecommendationReviewService {

	private final AiRecommendationRepository recommendationRepository;
	private final UserRepository userRepository;
	private final ScopedAuthorizationService authorizationService;
	private final AuditEventRepository auditEventRepository;
	private final AiMetrics metrics;

	public AiRecommendationReviewService(
			AiRecommendationRepository recommendationRepository,
			UserRepository userRepository,
			ScopedAuthorizationService authorizationService,
			AuditEventRepository auditEventRepository,
			AiMetrics metrics) {
		this.recommendationRepository = recommendationRepository;
		this.userRepository = userRepository;
		this.authorizationService = authorizationService;
		this.auditEventRepository = auditEventRepository;
		this.metrics = metrics;
	}

	@Transactional
	public AiRecommendationResult review(
			UUID recommendationId,
			AiRecommendation.ReviewDecision decision,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.COORDINATION_UPDATE, null, SystemRole.COORDINATOR);
		User reviewer = userRepository.findById(principal.userId())
				.orElseThrow(() -> new DomainValidationException("AI recommendation reviewer does not exist."));
		AiRecommendation recommendation = recommendationRepository.findForUpdateById(recommendationId)
				.orElseThrow(() -> new DomainValidationException("AI recommendation does not exist."));
		Map<String, Object> before = Map.of("status", recommendation.getStatus().name());
		recommendation.review(decision, reviewer, reason);
		recommendationRepository.save(recommendation);
		auditEventRepository.save(AuditEvent.domainMutation(
				reviewer, "AI_RECOMMENDATION_REVIEWED", "AI_RECOMMENDATION",
				recommendation.getId(), before,
				Map.of(
						"status", recommendation.getStatus().name(),
						"reviewedBy", reviewer.getId().toString()),
				recommendation.getReviewReason(), requestId));
		metrics.recordReview(recommendation.getStatus());
		return AiRecommendationResult.from(recommendation);
	}
}
