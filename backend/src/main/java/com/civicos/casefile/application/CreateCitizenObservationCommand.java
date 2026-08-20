package com.civicos.casefile.application;

import java.util.UUID;

public record CreateCitizenObservationCommand(
		String category,
		String description,
		double latitude,
		double longitude,
		UUID roadSegmentId) {
}
