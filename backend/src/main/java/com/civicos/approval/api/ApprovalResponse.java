package com.civicos.approval.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.civicos.approval.domain.Approval;

public record ApprovalResponse(
		UUID id,
		UUID interventionId,
		String interventionNumber,
		UUID agencyId,
		Approval.Status status,
		Approval.Decision decision,
		UUID actorId,
		String reason,
		List<Map<String, Object>> conditions,
		Instant decidedAt,
		long version,
		Instant createdAt) {

	public static ApprovalResponse from(Approval approval) {
		return new ApprovalResponse(
				approval.getId(), approval.getIntervention().getId(),
				approval.getIntervention().getInterventionNumber(),
				approval.getIntervention().getAgency().getId(), approval.getStatus(),
				approval.getDecision(), approval.getActor() == null ? null : approval.getActor().getId(),
				approval.getReason(), approval.getConditions(), approval.getDecidedAt(),
				approval.getVersion(), approval.getCreatedAt());
	}
}
