package com.civicos.coordination.domain;

import com.civicos.ai.domain.AiRecommendation;
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
@Table(name = "coordination_decisions")
public class CoordinationDecision extends AbstractCreatedEntity {

	public enum Type { ACCEPT, ACCEPT_WITH_MODIFICATION, REJECT, REQUEST_INFORMATION }

	@ManyToOne(optional = false)
	@JoinColumn(name = "conflict_id", nullable = false)
	private Conflict conflict;

	@ManyToOne(optional = false)
	@JoinColumn(name = "coordinator_id", nullable = false)
	private User coordinator;

	@Enumerated(EnumType.STRING)
	@Column(name = "decision_type", nullable = false, length = 40)
	private Type decisionType;

	@Column(name = "decision_text", nullable = false, columnDefinition = "text")
	private String decisionText;

	@ManyToOne
	@JoinColumn(name = "accepted_recommendation_id")
	private AiRecommendation acceptedRecommendation;

	public Conflict getConflict() { return conflict; }
	public User getCoordinator() { return coordinator; }
	public Type getDecisionType() { return decisionType; }
	public String getDecisionText() { return decisionText; }
	public AiRecommendation getAcceptedRecommendation() { return acceptedRecommendation; }
}
