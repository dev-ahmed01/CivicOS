package com.civicos.dependency.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.civicos.common.domain.DomainConflictException;
import com.civicos.dependency.domain.Dependency;
import com.civicos.dependency.repository.DependencyRepository;
import com.civicos.intervention.domain.Intervention;

@Component
public class DependencyScheduleValidator {

	private final DependencyRepository dependencyRepository;

	public DependencyScheduleValidator(DependencyRepository dependencyRepository) {
		this.dependencyRepository = dependencyRepository;
	}

	public void validateProposedSchedule(
			UUID interventionId,
			Instant proposedStart,
			Instant proposedEnd) {
		List<Dependency> outgoing = dependencyRepository.findBySourceInterventionId(interventionId);
		for (Dependency dependency : outgoing) {
			if (isEnforced(dependency)
					&& violates(proposedEnd, dependency.getTargetIntervention().getPlannedStart())) {
				throw invalidSchedule(dependency);
			}
		}

		List<Dependency> incoming = dependencyRepository.findByTargetInterventionId(interventionId);
		for (Dependency dependency : incoming) {
			if (isEnforced(dependency)
					&& violates(effectiveCompletion(dependency.getSourceIntervention()), proposedStart)) {
				throw invalidSchedule(dependency);
			}
		}
	}

	public boolean scheduleIsBlocked(Intervention source, Intervention target, boolean required) {
		return required && violates(effectiveCompletion(source), target.getPlannedStart());
	}

	private boolean isEnforced(Dependency dependency) {
		return dependency.isRequired() && dependency.getStatus() != Dependency.Status.CANCELLED;
	}

	private Instant effectiveCompletion(Intervention intervention) {
		return intervention.getActualEnd() == null
				? intervention.getPlannedEnd()
				: intervention.getActualEnd();
	}

	private boolean violates(Instant sourceCompletion, Instant targetStart) {
		return sourceCompletion.isAfter(targetStart);
	}

	private DomainConflictException invalidSchedule(Dependency dependency) {
		return new DomainConflictException(
				"The proposed schedule violates required dependency " + dependency.getId() + ".");
	}
}
