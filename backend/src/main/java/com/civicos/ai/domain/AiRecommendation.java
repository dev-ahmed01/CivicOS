package com.civicos.ai.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.validation.ValidationRules;
import com.civicos.conflict.domain.Conflict;
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
@Table(name = "ai_recommendations")
public class AiRecommendation extends AbstractCreatedEntity {

	public enum Status { PENDING_REVIEW, ACCEPTED, REJECTED, SUPERSEDED }
	public enum ReviewDecision { ACCEPT, REJECT }

	@ManyToOne(optional = false)
	@JoinColumn(name = "ai_run_id", nullable = false, updatable = false)
	private AiRun aiRun;

	@ManyToOne(optional = false)
	@JoinColumn(name = "conflict_id", nullable = false, updatable = false)
	private Conflict conflict;

	@Column(name = "recommendation_type", nullable = false, updatable = false, length = 60)
	private String type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> recommendation = new LinkedHashMap<>();

	@Column(nullable = false, updatable = false, precision = 5, scale = 4)
	private BigDecimal confidence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.PENDING_REVIEW;

	@ManyToOne
	@JoinColumn(name = "reviewed_by")
	private User reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_reason", columnDefinition = "text")
	private String reviewReason;

	@Version
	@Column(nullable = false)
	private long version;

	protected AiRecommendation() {
	}

	public static AiRecommendation create(
			AiRun aiRun,
			Conflict conflict,
			String type,
			Map<String, Object> recommendation,
			BigDecimal confidence) {
		AiRecommendation entity = new AiRecommendation();
		entity.aiRun = ValidationRules.required(aiRun, "AI run");
		if (aiRun.getStatus() != AiRun.Status.COMPLETED
				|| aiRun.getOperation() != AiTask.RECOMMENDATION_GENERATION) {
			throw new DomainConflictException("Only a completed recommendation AI run can create a recommendation.");
		}
		entity.conflict = ValidationRules.required(conflict, "Conflict");
		entity.type = ValidationRules.requiredText(type, "AI recommendation type");
		if (entity.type.length() > 60) {
			throw new DomainValidationException("AI recommendation type must not exceed 60 characters.");
		}
		entity.recommendation = new LinkedHashMap<>(ValidationRules.required(
				recommendation, "AI recommendation"));
		entity.confidence = ValidationRules.required(confidence, "AI recommendation confidence");
		AiConfidenceBand.from(confidence);
		return entity;
	}

	public void review(ReviewDecision decision, User reviewer, String reason) {
		if (status != Status.PENDING_REVIEW) {
			throw new DomainConflictException("AI recommendation has already been reviewed.");
		}
		ReviewDecision validatedDecision = ValidationRules.required(decision, "AI review decision");
		this.reviewedBy = ValidationRules.required(reviewer, "AI recommendation reviewer");
		this.reviewReason = ValidationRules.optionalText(reason);
		if (validatedDecision == ReviewDecision.REJECT && reviewReason == null) {
			throw new DomainValidationException("AI recommendation rejection reason is required.");
		}
		this.status = validatedDecision == ReviewDecision.ACCEPT ? Status.ACCEPTED : Status.REJECTED;
		this.reviewedAt = Instant.now();
	}

	public AiRun getAiRun() { return aiRun; }
	public Conflict getConflict() { return conflict; }
	public String getType() { return type; }
	public Map<String, Object> getRecommendation() { return Map.copyOf(recommendation); }
	public BigDecimal getConfidence() { return confidence; }
	public Status getStatus() { return status; }
	public User getReviewedBy() { return reviewedBy; }
	public Instant getReviewedAt() { return reviewedAt; }
	public String getReviewReason() { return reviewReason; }
	public long getVersion() { return version; }
}
