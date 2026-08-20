package com.civicos.verification.domain;

import java.util.UUID;

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

@Entity
@Table(name = "verifications")
public class Verification extends AbstractCreatedEntity {

	public enum Source { FIELD_INSPECTOR, CITIZEN, SYSTEM }
	public enum Result { PASSED, FAILED, CONDITIONAL, CONFIRMED, DISPUTED, CANNOT_VERIFY }

	@Column(name = "target_type", nullable = false, length = 60)
	private String targetType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Source source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Result result;

	@ManyToOne
	@JoinColumn(name = "submitted_by")
	private User submittedBy;

	@Column(columnDefinition = "text")
	private String reason;

	protected Verification() {
	}

	public static Verification fieldInspector(
			UUID interventionId,
			Result result,
			User inspector,
			String reason) {
		Verification verification = new Verification();
		verification.targetType = "INTERVENTION";
		verification.targetId = ValidationRules.required(interventionId, "Intervention");
		verification.source = Source.FIELD_INSPECTOR;
		verification.result = ValidationRules.required(result, "Verification result");
		verification.submittedBy = ValidationRules.required(inspector, "Inspector");
		verification.reason = reason == null || reason.isBlank() ? null : reason.strip();
		return verification;
	}

	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public Source getSource() { return source; }
	public Result getResult() { return result; }
	public User getSubmittedBy() { return submittedBy; }
	public String getReason() { return reason; }
}
