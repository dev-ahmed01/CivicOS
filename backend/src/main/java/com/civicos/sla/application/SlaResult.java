package com.civicos.sla.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.sla.domain.Sla;

public record SlaResult(
		UUID slaId,
		String targetType,
		UUID targetId,
		Sla.Type slaType,
		Sla.Status status,
		Instant deadline,
		long version) {
}
