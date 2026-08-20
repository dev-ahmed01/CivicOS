package com.civicos.ai.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.ai.config.AiProperties;
import com.civicos.ai.domain.AiConfidenceBand;
import com.civicos.ai.domain.AiRun;
import com.civicos.ai.prompt.AiPrompt;
import com.civicos.ai.repository.AiRunRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class AiRunLifecycleService {

	private final AiRunRepository runRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;

	public AiRunLifecycleService(
			AiRunRepository runRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository) {
		this.runRepository = runRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiRun start(
			AiRequest request,
			UUID requesterId,
			AiProperties.Selection selection,
			AiPrompt prompt,
			String inputHash,
			Map<String, Object> configuration) {
		User requester = userRepository.findById(requesterId)
				.orElseThrow(() -> new DomainValidationException("AI requester does not exist."));
		Map<String, Object> inputReference = Map.of(
				"entityType", request.entityType(),
				"entityId", request.entityId().toString(),
				"contextKeys", request.context().keySet().stream().sorted().toList());
		AiRun run = AiRun.requested(
				request.task(),
				requester,
				request.entityType(),
				request.entityId(),
				selection.provider(),
				selection.model(),
				selection.model(),
				prompt.version(),
				prompt.schemaVersion(),
				inputHash,
				inputReference,
				configuration);
		run.markRunning();
		return runRepository.saveAndFlush(run);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiExecutionResult complete(
			UUID runId,
			AiResponse response,
			boolean reviewRequired,
			long latencyMs,
			String requestId) {
		AiRun run = findForUpdate(runId);
		run.complete(response, reviewRequired, latencyMs);
		runRepository.save(run);
		Map<String, Object> after = new LinkedHashMap<>();
		after.put("operation", run.getOperation().name());
		after.put("status", run.getStatus().name());
		after.put("entityType", run.getEntityType());
		after.put("entityId", run.getEntityId().toString());
		after.put("provider", run.getProvider());
		after.put("model", run.getModel());
		after.put("promptVersion", run.getPromptVersion());
		after.put("schemaVersion", run.getSchemaVersion());
		after.put("confidence", run.getConfidence());
		after.put("reviewRequired", run.isReviewRequired());
		auditEventRepository.save(AuditEvent.domainMutation(
				run.getRequestedBy(), "AI_RUN_COMPLETED", "AI_RUN", run.getId(),
				null, after, "Advisory AI output generated and validated.", requestId));
		return result(run);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiExecutionResult fail(UUID runId, String errorMessage, long latencyMs, String requestId) {
		AiRun run = findForUpdate(runId);
		run.fail(errorMessage, latencyMs);
		runRepository.save(run);
		Map<String, Object> after = Map.of(
				"operation", run.getOperation().name(),
				"status", run.getStatus().name(),
				"entityType", run.getEntityType(),
				"entityId", run.getEntityId().toString(),
				"provider", run.getProvider(),
				"model", run.getModel(),
				"reviewRequired", true);
		auditEventRepository.save(AuditEvent.domainMutation(
				run.getRequestedBy(), "AI_RUN_FAILED", "AI_RUN", run.getId(),
				null, after, "AI failed; deterministic/manual workflow remains available.", requestId));
		return result(run);
	}

	private AiRun findForUpdate(UUID runId) {
		return runRepository.findForUpdateById(runId)
				.orElseThrow(() -> new DomainValidationException("AI run does not exist."));
	}

	private AiExecutionResult result(AiRun run) {
		return new AiExecutionResult(
				run.getId(),
				run.getStatus(),
				run.getOutput() == null ? Map.of() : run.getOutput(),
				run.getConfidence(),
				run.getConfidence() == null ? null : AiConfidenceBand.from(run.getConfidence()),
				run.isReviewRequired(),
				run.getProvider(),
				run.getModel(),
				run.getPromptVersion(),
				run.getSchemaVersion(),
				run.getErrorMessage());
	}
}
