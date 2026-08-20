package com.civicos.approval.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.approval.domain.Approval;

public record ApprovalResult(
		UUID approvalId,
		UUID interventionId,
		Approval.Status status,
		Approval.Decision decision,
		long version,
		Instant occurredAt) {
}
