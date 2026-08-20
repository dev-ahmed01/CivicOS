package com.civicos.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.civicos.casefile.domain.CivicCase;
import com.civicos.intervention.domain.Intervention;

class CanonicalWorkflowTest {

	private static final Instant OCCURRED_AT = Instant.parse("2026-08-20T08:00:00Z");

	@Test
	void caseFollowsTheCanonicalLifecycleAndSetsClosureTime() {
		CivicCase civicCase = new CivicCase();

		civicCase.transition(CivicCase.WorkflowAction.BEGIN_REVIEW, OCCURRED_AT);
		civicCase.transition(CivicCase.WorkflowAction.START_PROGRESS, OCCURRED_AT);
		civicCase.transition(CivicCase.WorkflowAction.REQUEST_VERIFICATION, OCCURRED_AT);
		civicCase.transition(CivicCase.WorkflowAction.VERIFY, OCCURRED_AT);
		civicCase.transition(CivicCase.WorkflowAction.CLOSE, OCCURRED_AT);

		assertThat(civicCase.getStatus()).isEqualTo(CivicCase.Status.CLOSED);
		assertThat(civicCase.getClosedAt()).isEqualTo(OCCURRED_AT);
	}

	@Test
	void interventionSupportsTheCanonicalCorrectionLoop() {
		Intervention intervention = completedIntervention();

		intervention.transition(Intervention.WorkflowAction.FAIL_VERIFICATION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.BEGIN_CORRECTIVE_ACTION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.RESUME_CORRECTIVE_WORK, OCCURRED_AT);

		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.IN_PROGRESS);
		assertThat(intervention.getActualStart()).isEqualTo(OCCURRED_AT);
		assertThat(intervention.getActualEnd()).isNull();

		intervention.transition(Intervention.WorkflowAction.COMPLETE, OCCURRED_AT.plusSeconds(60));
		intervention.transition(Intervention.WorkflowAction.VERIFY, OCCURRED_AT.plusSeconds(120));
		intervention.transition(Intervention.WorkflowAction.CLOSE, OCCURRED_AT.plusSeconds(180));

		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.CLOSED);
	}

	@Test
	void invalidTransitionLeavesTheAggregateUnchanged() {
		Intervention intervention = new Intervention();

		assertThatThrownBy(() -> intervention.transition(
				Intervention.WorkflowAction.START, OCCURRED_AT))
				.isInstanceOf(WorkflowActionNotAllowedException.class)
				.hasMessageContaining("DRAFT");
		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.DRAFT);
		assertThat(intervention.getActualStart()).isNull();
	}

	private Intervention completedIntervention() {
		Intervention intervention = new Intervention();
		intervention.transition(Intervention.WorkflowAction.SUBMIT, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.BEGIN_REVIEW, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.REQUIRE_COORDINATION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.APPROVE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.SCHEDULE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.START, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.COMPLETE, OCCURRED_AT);
		return intervention;
	}
}
