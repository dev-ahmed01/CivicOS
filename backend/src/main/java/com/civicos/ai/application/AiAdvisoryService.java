package com.civicos.ai.application;

import org.springframework.stereotype.Service;

import com.civicos.ai.domain.AiRun;
import com.civicos.ai.domain.AiTask;
import com.civicos.common.domain.DomainValidationException;

@Service
public class AiAdvisoryService {

	private final AiGateway aiGateway;
	private final AiRecommendationPersistenceService recommendationPersistenceService;

	public AiAdvisoryService(
			AiGateway aiGateway,
			AiRecommendationPersistenceService recommendationPersistenceService) {
		this.aiGateway = aiGateway;
		this.recommendationPersistenceService = recommendationPersistenceService;
	}

	public AiExecutionResult classify(AiRequest request) {
		return execute(request, AiTask.CLASSIFICATION);
	}

	public AiExecutionResult assistMatching(AiRequest request) {
		return execute(request, AiTask.MATCHING_ASSISTANCE);
	}

	public AiExecutionResult analyseEvidence(AiRequest request) {
		return execute(request, AiTask.EVIDENCE_ANALYSIS);
	}

	public AiExecutionResult explainConflict(AiRequest request) {
		return execute(request, AiTask.CONFLICT_EXPLANATION);
	}

	public AiExecutionResult summarizeCase(AiRequest request) {
		return execute(request, AiTask.CASE_SUMMARIZATION);
	}

	public AiRecommendationGenerationResult recommendCoordination(AiRequest request) {
		assertTask(request, AiTask.RECOMMENDATION_GENERATION);
		if (!"CONFLICT".equalsIgnoreCase(request.entityType())) {
			throw new DomainValidationException("Coordination recommendations require a conflict reference.");
		}
		AiExecutionResult execution = aiGateway.execute(request);
		if (execution.status() != AiRun.Status.COMPLETED) {
			return new AiRecommendationGenerationResult(execution, null);
		}
		AiRecommendationResult recommendation = recommendationPersistenceService.create(
				execution.runId(), request.entityId(), request.requestId());
		return new AiRecommendationGenerationResult(execution, recommendation);
	}

	private AiExecutionResult execute(AiRequest request, AiTask expectedTask) {
		assertTask(request, expectedTask);
		return aiGateway.execute(request);
	}

	private void assertTask(AiRequest request, AiTask expectedTask) {
		if (request == null || request.task() != expectedTask) {
			throw new DomainValidationException("AI request task does not match the advisory operation.");
		}
	}
}
