package com.civicos.evidence.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.civicos.evidence.domain.Evidence;

public record EvidenceResponse(
		UUID id,
		String targetType,
		UUID targetId,
		UUID uploadedBy,
		Evidence.Type type,
		String originalFilename,
		String mimeType,
		Long fileSizeBytes,
		String checksum,
		Instant capturedAt,
		BigDecimal latitude,
		BigDecimal longitude,
		Map<String, Object> metadata,
		Evidence.Status status,
		long version,
		Instant createdAt) {

	public static EvidenceResponse from(Evidence evidence) {
		return new EvidenceResponse(
				evidence.getId(), evidence.getTargetType(), evidence.getTargetId(),
				evidence.getUploadedBy().getId(), evidence.getType(), evidence.getOriginalFilename(),
				evidence.getMimeType(), evidence.getFileSizeBytes(), evidence.getChecksum(),
				evidence.getCapturedAt(), evidence.getLatitude(), evidence.getLongitude(),
				evidence.getMetadata(), evidence.getStatus(), evidence.getVersion(), evidence.getCreatedAt());
	}
}
