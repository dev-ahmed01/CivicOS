package com.civicos.intervention.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.civicos.casefile.domain.CivicCase;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;

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
		Intervention intervention = verificationPendingIntervention();

		intervention.transition(Intervention.WorkflowAction.FAIL_VERIFICATION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.BEGIN_CORRECTIVE_ACTION, OCCURRED_AT);
		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.RESTORATION);
		assertThat(intervention.getActualEnd()).isNull();

		intervention.transition(Intervention.WorkflowAction.COMPLETE_RESTORATION, OCCURRED_AT.plusSeconds(60));
		intervention.transition(Intervention.WorkflowAction.SUBMIT_EVIDENCE, OCCURRED_AT.plusSeconds(120));
		intervention.transition(Intervention.WorkflowAction.VERIFY, OCCURRED_AT.plusSeconds(180));
		intervention.transition(Intervention.WorkflowAction.CLOSE, OCCURRED_AT.plusSeconds(240));

		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.CLOSED);
	}

	@Test
	void holdAndResumeRestoreTheExactPriorState() {
		Intervention intervention = new Intervention();
		intervention.transition(Intervention.WorkflowAction.SUBMIT, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.BEGIN_REVIEW, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.ANALYSE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.REQUIRE_COORDINATION, OCCURRED_AT);

		intervention.transition(Intervention.WorkflowAction.HOLD, OCCURRED_AT);
		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.ON_HOLD);
		assertThat(intervention.getHeldFromStatus()).isEqualTo(Intervention.Status.COORDINATION_REQUIRED);

		intervention.transition(Intervention.WorkflowAction.RESUME, OCCURRED_AT);
		assertThat(intervention.getStatus()).isEqualTo(Intervention.Status.COORDINATION_REQUIRED);
		assertThat(intervention.getHeldFromStatus()).isNull();
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

	private Intervention verificationPendingIntervention() {
		Intervention intervention = new Intervention();
		intervention.transition(Intervention.WorkflowAction.SUBMIT, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.BEGIN_REVIEW, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.ANALYSE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.REQUIRE_COORDINATION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.COMPLETE_COORDINATION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.REQUEST_APPROVAL, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.APPROVE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.SCHEDULE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.START, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.COMPLETE, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.COMPLETE_RESTORATION, OCCURRED_AT);
		intervention.transition(Intervention.WorkflowAction.SUBMIT_EVIDENCE, OCCURRED_AT);
		return intervention;
	}
}
