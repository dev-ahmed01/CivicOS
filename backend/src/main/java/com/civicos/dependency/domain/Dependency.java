package com.civicos.dependency.domain;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.intervention.domain.Intervention;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "dependencies",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_dependencies_relationship",
				columnNames = {"source_intervention_id", "target_intervention_id", "dependency_type"}))
public class Dependency extends AbstractCreatedEntity {

	public enum Type { MUST_COMPLETE_BEFORE, MUST_VERIFY_BEFORE, RESTORATION_DEPENDS_ON }
	public enum Status { ACTIVE, SATISFIED, BLOCKED, CANCELLED }

	@ManyToOne(optional = false)
	@JoinColumn(name = "source_intervention_id", nullable = false)
	private Intervention sourceIntervention;

	@ManyToOne(optional = false)
	@JoinColumn(name = "target_intervention_id", nullable = false)
	private Intervention targetIntervention;

	@Enumerated(EnumType.STRING)
	@Column(name = "dependency_type", nullable = false, length = 60)
	private Type type;

	@Column(nullable = false)
	private boolean required = true;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.ACTIVE;

	@Column(nullable = false, columnDefinition = "text")
	private String reason;

	@Version
	@Column(nullable = false)
	private long version;

	protected Dependency() {
	}

	public static Dependency create(
			Intervention source,
			Intervention target,
			Type type,
			boolean required,
			String reason,
			boolean scheduleBlocked) {
		if (source == null || target == null) {
			throw new DomainValidationException("Source and target interventions are required.");
		}
		if (source.getId().equals(target.getId())) {
			throw new DomainValidationException("A dependency cannot reference the same intervention twice.");
		}
		Dependency dependency = new Dependency();
		dependency.sourceIntervention = source;
		dependency.targetIntervention = target;
		dependency.type = ValidationRules.required(type, "Dependency type");
		dependency.required = required;
		dependency.reason = ValidationRules.requiredText(reason, "Dependency reason");
		dependency.status = scheduleBlocked ? Status.BLOCKED : Status.ACTIVE;
		return dependency;
	}

	public void cancel() {
		if (status == Status.CANCELLED) {
			throw new DomainConflictException("The dependency is already cancelled.");
		}
		status = Status.CANCELLED;
	}

	public Intervention getSourceIntervention() { return sourceIntervention; }
	public Intervention getTargetIntervention() { return targetIntervention; }
	public Type getType() { return type; }
	public boolean isRequired() { return required; }
	public Status getStatus() { return status; }
	public String getReason() { return reason; }
	public long getVersion() { return version; }

}
