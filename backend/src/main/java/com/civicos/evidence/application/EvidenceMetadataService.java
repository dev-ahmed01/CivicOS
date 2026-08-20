package com.civicos.evidence.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.civicos.common.domain.DomainValidationException;
import com.civicos.evidence.domain.Evidence;
import com.civicos.user.domain.User;

@Service
public class EvidenceMetadataService {

	private static final Set<String> SYSTEM_KEYS = Set.of(
			"targetType", "targetId", "uploaderId", "provenance", "evidenceType",
			"mimeType", "fileSizeBytes", "checksum", "capturedAt");

	public Map<String, Object> trustedMetadata(
			Map<String, Object> clientMetadata,
			String targetType,
			UUID targetId,
			User uploader,
			String provenance,
			Evidence.Type evidenceType,
			String mimeType,
			long fileSizeBytes,
			String checksum,
			Instant capturedAt) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		if (clientMetadata != null) {
			if (clientMetadata.size() > 25) {
				throw new DomainValidationException("Evidence metadata cannot contain more than 25 entries.");
			}
			clientMetadata.forEach((key, value) -> addClientValue(metadata, key, value));
		}
		metadata.put("targetType", targetType);
		metadata.put("targetId", targetId.toString());
		metadata.put("uploaderId", uploader.getId().toString());
		metadata.put("provenance", provenance);
		metadata.put("evidenceType", evidenceType.name());
		metadata.put("mimeType", mimeType);
		metadata.put("fileSizeBytes", fileSizeBytes);
		metadata.put("checksum", checksum);
		if (capturedAt != null) {
			metadata.put("capturedAt", capturedAt.toString());
		}
		return metadata;
	}

	private void addClientValue(Map<String, Object> target, String key, Object value) {
		String normalizedKey = key == null ? null : key.strip();
		if (normalizedKey == null || normalizedKey.isBlank()
				|| normalizedKey.length() > 80 || SYSTEM_KEYS.contains(normalizedKey)) {
			throw new DomainValidationException("Evidence metadata contains an invalid or system-owned key.");
		}
		if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
			throw new DomainValidationException("Evidence metadata values must be scalar.");
		}
		if (value instanceof String text && text.length() > 500) {
			throw new DomainValidationException("Evidence metadata text cannot exceed 500 characters.");
		}
		target.put(normalizedKey, value);
	}
}
