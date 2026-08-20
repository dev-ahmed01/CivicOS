package com.civicos.ai.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.conflict.domain.Conflict;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_recommendations")
public class AiRecommendation extends AbstractCreatedEntity {

	public enum Status { PENDING_REVIEW, ACCEPTED, REJECTED, SUPERSEDED }

	@ManyToOne(optional = false)
	@JoinColumn(name = "ai_run_id", nullable = false)
	private AiRun aiRun;

	@ManyToOne(optional = false)
	@JoinColumn(name = "conflict_id", nullable = false)
	private Conflict conflict;

	@Column(name = "recommendation_type", nullable = false, length = 60)
	private String type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> recommendation = new LinkedHashMap<>();

	@Column(precision = 5, scale = 4)
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

	public AiRun getAiRun() { return aiRun; }
	public Conflict getConflict() { return conflict; }
	public String getType() { return type; }
	public Map<String, Object> getRecommendation() { return Map.copyOf(recommendation); }
	public BigDecimal getConfidence() { return confidence; }
	public Status getStatus() { return status; }
	public User getReviewedBy() { return reviewedBy; }
	public Instant getReviewedAt() { return reviewedAt; }
	public String getReviewReason() { return reviewReason; }
}
