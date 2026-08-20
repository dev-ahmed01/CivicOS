package com.civicos.evidence.application;

import com.civicos.evidence.domain.Evidence;

public record EvidenceReviewCommand(Evidence.Status decision, String reason, long expectedVersion) {
}
