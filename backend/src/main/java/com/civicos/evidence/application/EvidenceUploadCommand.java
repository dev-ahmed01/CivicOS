package com.civicos.evidence.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.civicos.evidence.domain.Evidence;

public record EvidenceUploadCommand(
		String targetType,
		UUID targetId,
		Evidence.Type type,
		String originalFilename,
		String mimeType,
		Instant capturedAt,
		BigDecimal latitude,
		BigDecimal longitude,
		Map<String, Object> metadata) {
}
