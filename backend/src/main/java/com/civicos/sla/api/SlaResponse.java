package com.civicos.sla.api;

import java.time.Instant;
import java.util.UUID;

import com.civicos.sla.domain.Sla;

public record SlaResponse(
		UUID id,
		String targetType,
		UUID targetId,
		Sla.Type slaType,
		Instant startAt,
		Instant deadline,
		Sla.Status status,
		Instant pausedAt,
		Instant completedAt,
		long version) {

	public static SlaResponse from(Sla sla) {
		return new SlaResponse(
				sla.getId(), sla.getTargetType(), sla.getTargetId(), sla.getSlaType(),
				sla.getStartAt(), sla.getDeadline(), sla.getStatus(), sla.getPausedAt(),
				sla.getCompletedAt(), sla.getVersion());
	}
}
