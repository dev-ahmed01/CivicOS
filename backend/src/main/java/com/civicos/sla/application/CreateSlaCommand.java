package com.civicos.sla.application;

import java.time.Instant;
import java.util.UUID;

import com.civicos.sla.domain.Sla;

public record CreateSlaCommand(
		String targetType,
		UUID targetId,
		Sla.Type slaType,
		SlaUrgency urgency,
		Instant startAt) {
}
