package com.civicos.ai.application;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.ai.domain.AiRecommendation;
import com.civicos.ai.domain.AiRun;
import com.civicos.ai.repository.AiRecommendationRepository;
import com.civicos.ai.repository.AiRunRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;

@Service
public class AiRecommendationPersistenceService {

	private final AiRunRepository runRepository;
	private final ConflictRepository conflictRepository;
	private final AiRecommendationRepository recommendationRepository;
	private final AuditEventRepository auditEventRepository;

	public AiRecommendationPersistenceService(
			AiRunRepository runRepository,
			ConflictRepository conflictRepository,
			AiRecommendationRepository recommendationRepository,
			AuditEventRepository auditEventRepository) {
		this.runRepository = runRepository;
		this.conflictRepository = conflictRepository;
		this.recommendationRepository = recommendationRepository;
		this.auditEventRepository = auditEventRepository;
	}

	@Transactional
	public AiRecommendationResult create(UUID runId, UUID conflictId, String requestId) {
		AiRun run = runRepository.findById(runId)
				.orElseThrow(() -> new DomainValidationException("AI run does not exist."));
		Conflict conflict = conflictRepository.findById(conflictId)
				.orElseThrow(() -> new DomainValidationException("Conflict does not exist."));
		Map<String, Object> result = result(run);
		AiRecommendation recommendation = recommendationRepository.save(AiRecommendation.create(
				run, conflict, "COORDINATION_SEQUENCE", result, run.getConfidence()));
		auditEventRepository.save(AuditEvent.domainMutation(
				run.getRequestedBy(), "AI_RECOMMENDATION_CREATED", "AI_RECOMMENDATION",
				recommendation.getId(), null,
				Map.of(
						"runId", run.getId().toString(),
						"conflictId", conflict.getId().toString(),
						"status", recommendation.getStatus().name(),
						"confidence", recommendation.getConfidence()),
				"AI recommendation is advisory and pending coordinator review.", requestId));
		return AiRecommendationResult.from(recommendation);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> result(AiRun run) {
		if (run.getOutput() == null || !(run.getOutput().get("result") instanceof Map<?, ?> result)) {
			throw new DomainValidationException("AI recommendation output is unavailable.");
		}
		return Map.copyOf((Map<String, Object>) result);
	}
}
