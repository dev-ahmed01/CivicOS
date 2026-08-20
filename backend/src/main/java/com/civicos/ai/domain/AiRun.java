package com.civicos.ai.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.civicos.common.persistence.AbstractCreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_runs")
public class AiRun extends AbstractCreatedEntity {

	public enum Status { REQUESTED, RUNNING, COMPLETED, FAILED, CANCELLED }

	@Column(nullable = false, length = 60)
	private String operation;

	@Column(nullable = false, length = 100)
	private String provider;

	@Column(nullable = false, length = 150)
	private String model;

	@Column(name = "prompt_version", nullable = false, length = 50)
	private String promptVersion;

	@Column(name = "schema_version", nullable = false, length = 50)
	private String schemaVersion;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "input_reference", nullable = false, columnDefinition = "jsonb")
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

	@Column(name = "completed_at")
	private Instant completedAt;

	public String getOperation() { return operation; }
	public String getProvider() { return provider; }
	public String getModel() { return model; }
	public String getPromptVersion() { return promptVersion; }
	public String getSchemaVersion() { return schemaVersion; }
	public Map<String, Object> getInputReference() { return Map.copyOf(inputReference); }
	public Map<String, Object> getOutput() { return output == null ? null : Map.copyOf(output); }
	public BigDecimal getConfidence() { return confidence; }
	public Status getStatus() { return status; }
	public String getErrorMessage() { return errorMessage; }
	public Instant getCompletedAt() { return completedAt; }
}
