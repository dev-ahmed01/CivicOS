package com.civicos.ai.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.civicos.ai.application.AiResponse;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.validation.ValidationRules;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "ai_runs")
public class AiRun extends AbstractCreatedEntity {

	public enum Status { REQUESTED, RUNNING, COMPLETED, FAILED, CANCELLED }

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 60)
	private AiTask operation;

	@ManyToOne(optional = false)
	@JoinColumn(name = "requested_by", nullable = false, updatable = false)
	private User requestedBy;

	@Column(name = "entity_type", nullable = false, updatable = false, length = 100)
	private String entityType;

	@Column(name = "entity_id", nullable = false, updatable = false)
	private UUID entityId;

	@Column(nullable = false, updatable = false, length = 100)
	private String provider;

	@Column(nullable = false, updatable = false, length = 150)
	private String model;

	@Column(name = "model_version", nullable = false, updatable = false, length = 100)
	private String modelVersion;

	@Column(name = "prompt_version", nullable = false, updatable = false, length = 50)
	private String promptVersion;

	@Column(name = "schema_version", nullable = false, updatable = false, length = 50)
	private String schemaVersion;

	@Column(name = "input_hash", nullable = false, updatable = false, length = 64)
	private String inputHash;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> configuration = new LinkedHashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "input_reference", nullable = false, updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> inputReference = new LinkedHashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> output;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "input_tokens")
	private Long inputTokens;

	@Column(name = "output_tokens")
	private Long outputTokens;

	@Column(name = "estimated_cost", precision = 19, scale = 6)
	private BigDecimal estimatedCost;

	@Column(name = "latency_ms")
	private Long latencyMs;

	@Column(name = "review_required", nullable = false)
	private boolean reviewRequired = true;

	@Version
	@Column(nullable = false)
	private long version;

	protected AiRun() {
	}

	public static AiRun requested(
			AiTask operation,
			User requestedBy,
			String entityType,
			UUID entityId,
			String provider,
			String model,
			String modelVersion,
			String promptVersion,
			String schemaVersion,
			String inputHash,
			Map<String, Object> inputReference,
			Map<String, Object> configuration) {
		AiRun run = new AiRun();
		run.operation = ValidationRules.required(operation, "AI operation");
		run.requestedBy = ValidationRules.required(requestedBy, "AI requester");
		run.entityType = boundedText(entityType, "AI entity type", 100).toUpperCase();
		run.entityId = ValidationRules.required(entityId, "AI entity id");
		run.provider = boundedText(provider, "AI provider", 100);
		run.model = boundedText(model, "AI model", 150);
		run.modelVersion = boundedText(modelVersion, "AI model version", 100);
		run.promptVersion = boundedText(promptVersion, "AI prompt version", 50);
		run.schemaVersion = boundedText(schemaVersion, "AI schema version", 50);
		run.inputHash = boundedText(inputHash, "AI input hash", 64);
		run.inputReference = new LinkedHashMap<>(ValidationRules.required(
				inputReference, "AI input reference"));
		run.configuration = new LinkedHashMap<>(ValidationRules.required(
				configuration, "AI configuration"));
		run.status = Status.REQUESTED;
		return run;
	}

	public void markRunning() {
		assertStatus(Status.REQUESTED);
		status = Status.RUNNING;
		startedAt = Instant.now();
	}

	public void complete(AiResponse response, boolean reviewRequired, long latencyMs) {
		assertStatus(Status.RUNNING);
		AiResponse validated = ValidationRules.required(response, "AI response");
		this.output = new LinkedHashMap<>(validated.structuredOutput());
		this.confidence = validated.confidence();
		this.inputTokens = nonNegative(validated.inputTokens(), "AI input tokens");
		this.outputTokens = nonNegative(validated.outputTokens(), "AI output tokens");
		this.estimatedCost = nonNegative(validated.estimatedCost(), "AI estimated cost");
		this.latencyMs = nonNegative(latencyMs, "AI latency");
		this.reviewRequired = reviewRequired;
		this.status = Status.COMPLETED;
		this.completedAt = Instant.now();
	}

	public void fail(String errorMessage, long latencyMs) {
		assertStatus(Status.RUNNING);
		this.errorMessage = boundedText(errorMessage, "AI failure", 2000);
		this.latencyMs = nonNegative(latencyMs, "AI latency");
		this.reviewRequired = true;
		this.status = Status.FAILED;
		this.completedAt = Instant.now();
	}

	private void assertStatus(Status expected) {
		if (status != expected) {
			throw new DomainConflictException("AI run is not in " + expected + " state.");
		}
	}

	private static String boundedText(String value, String fieldName, int maximumLength) {
		String text = ValidationRules.requiredText(value, fieldName);
		if (text.length() > maximumLength) {
			throw new DomainValidationException(
					fieldName + " must not exceed " + maximumLength + " characters.");
		}
		return text;
	}

	private static long nonNegative(long value, String fieldName) {
		if (value < 0) {
			throw new DomainValidationException(fieldName + " cannot be negative.");
		}
		return value;
	}

	private static BigDecimal nonNegative(BigDecimal value, String fieldName) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
			throw new DomainValidationException(fieldName + " cannot be negative.");
		}
		return value;
	}

	public AiTask getOperation() { return operation; }
	public User getRequestedBy() { return requestedBy; }
	public String getEntityType() { return entityType; }
	public UUID getEntityId() { return entityId; }
	public String getProvider() { return provider; }
	public String getModel() { return model; }
	public String getModelVersion() { return modelVersion; }
	public String getPromptVersion() { return promptVersion; }
	public String getSchemaVersion() { return schemaVersion; }
	public String getInputHash() { return inputHash; }
	public Map<String, Object> getConfiguration() { return Map.copyOf(configuration); }
	public Map<String, Object> getInputReference() { return Map.copyOf(inputReference); }
	public Map<String, Object> getOutput() { return output == null ? null : Map.copyOf(output); }
	public BigDecimal getConfidence() { return confidence; }
	public Status getStatus() { return status; }
	public String getErrorMessage() { return errorMessage; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public Long getInputTokens() { return inputTokens; }
	public Long getOutputTokens() { return outputTokens; }
	public BigDecimal getEstimatedCost() { return estimatedCost; }
	public Long getLatencyMs() { return latencyMs; }
	public boolean isReviewRequired() { return reviewRequired; }
	public long getVersion() { return version; }
}
