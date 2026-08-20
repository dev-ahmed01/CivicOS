package com.civicos.coordination.application;

import java.util.Map;
import java.util.NoSuchElementException;
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
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.coordination.domain.CoordinationDecision;
import com.civicos.coordination.repository.CoordinationDecisionRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class CoordinationDecisionService {

	private final CoordinationDecisionRepository decisionRepository;
	private final ConflictRepository conflictRepository;
	private final AiRecommendationRepository recommendationRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService authorizationService;

	public CoordinationDecisionService(
			CoordinationDecisionRepository decisionRepository,
			ConflictRepository conflictRepository,
			AiRecommendationRepository recommendationRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService authorizationService) {
		this.decisionRepository = decisionRepository;
		this.conflictRepository = conflictRepository;
		this.recommendationRepository = recommendationRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional
	public CoordinationDecisionResult record(
			UUID conflictId,
			CoordinationDecision.Type decisionType,
			String decisionText,
			UUID acceptedRecommendationId,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.COORDINATION_UPDATE, null, SystemRole.COORDINATOR);
		Conflict conflict = conflictRepository.findById(conflictId)
				.orElseThrow(() -> new NoSuchElementException("Conflict not found: " + conflictId));
		User coordinator = userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated coordinator not found: " + principal.userId()));
		AiRecommendation recommendation = acceptedRecommendationId == null
				? null
				: recommendationRepository.findById(acceptedRecommendationId)
						.orElseThrow(() -> new NoSuchElementException(
								"AI recommendation not found: " + acceptedRecommendationId));
		CoordinationDecision decision = decisionRepository.saveAndFlush(CoordinationDecision.record(
				conflict, coordinator, decisionType, decisionText, recommendation));
		auditEventRepository.save(AuditEvent.domainMutation(
				coordinator, "COORDINATION_DECISION_RECORDED", "COORDINATION_DECISION",
				decision.getId(), null,
				Map.of(
						"conflictId", conflict.getId().toString(),
						"decisionType", decision.getDecisionType().name(),
						"acceptedRecommendationId", recommendation == null
								? ""
								: recommendation.getId().toString()),
				decision.getDecisionText(), requestId));
		return CoordinationDecisionResult.from(decision);
	}
}
