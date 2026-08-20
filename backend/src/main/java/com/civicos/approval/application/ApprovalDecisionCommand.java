package com.civicos.approval.application;

import java.util.List;
import java.util.Map;

import com.civicos.approval.domain.Approval;

public record ApprovalDecisionCommand(
		Approval.Decision decision,
		String reason,
		List<Map<String, Object>> conditions,
		long expectedVersion) {

	public ApprovalDecisionCommand {
		conditions = conditions == null ? List.of() : conditions.stream().map(Map::copyOf).toList();
	}
}
