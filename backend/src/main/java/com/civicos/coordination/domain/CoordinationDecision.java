package com.civicos.coordination.domain;

import com.civicos.ai.domain.AiRecommendation;
import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
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

	protected CoordinationDecision() {
	}

	public static CoordinationDecision record(
			Conflict conflict,
			User coordinator,
			Type decisionType,
			String decisionText,
			AiRecommendation acceptedRecommendation) {
		CoordinationDecision decision = new CoordinationDecision();
		decision.conflict = ValidationRules.required(conflict, "Coordination conflict");
		if (conflict.getStatus() == Conflict.Status.RESOLVED
				|| conflict.getStatus() == Conflict.Status.DISMISSED) {
			throw new DomainConflictException(
					"A final conflict cannot receive a new coordination decision.");
		}
		decision.coordinator = ValidationRules.required(coordinator, "Coordinator");
		decision.decisionType = ValidationRules.required(decisionType, "Coordination decision type");
		decision.decisionText = ValidationRules.requiredText(decisionText, "Coordination decision text");
		if (acceptedRecommendation != null) {
			if (decisionType != Type.ACCEPT) {
				throw new DomainValidationException(
						"Only an accepted coordination decision can link an accepted recommendation.");
			}
			if (!acceptedRecommendation.getConflict().getId().equals(conflict.getId())) {
				throw new DomainValidationException(
						"The accepted recommendation belongs to a different conflict.");
			}
			if (acceptedRecommendation.getStatus() != AiRecommendation.Status.ACCEPTED) {
				throw new DomainValidationException(
						"The recommendation must receive human acceptance before it can support a coordination decision.");
			}
		}
		decision.acceptedRecommendation = acceptedRecommendation;
		return decision;
	}

	public Conflict getConflict() { return conflict; }
	public User getCoordinator() { return coordinator; }
	public Type getDecisionType() { return decisionType; }
	public String getDecisionText() { return decisionText; }
	public AiRecommendation getAcceptedRecommendation() { return acceptedRecommendation; }
}
