package com.civicos.evidence.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.evidence.config.FileStorageProperties;

@Service
public class EvidenceValidator {

	private final FileStorageProperties properties;
	private final Clock clock;

	public EvidenceValidator(FileStorageProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public String validate(EvidenceUploadCommand command, byte[] content) {
		ValidationRules.required(command, "Evidence upload command");
		ValidationRules.required(command.targetId(), "Evidence target");
		ValidationRules.required(command.type(), "Evidence type");
		String targetType = ValidationRules.requiredText(command.targetType(), "Evidence target type")
				.toUpperCase(Locale.ROOT);
		String mimeType = ValidationRules.requiredText(command.mimeType(), "Evidence MIME type")
				.toLowerCase(Locale.ROOT);
		if (content == null || content.length == 0) {
			throw new DomainValidationException("Evidence file content is required.");
		}
		if (content.length > properties.getMaxUploadSize().toBytes()) {
			throw new DomainValidationException("Evidence file exceeds the configured upload limit.");
		}
		Set<String> allowed = properties.getAllowedContentTypes();
		if (!allowed.contains(mimeType)) {
			throw new DomainValidationException("Evidence MIME type is not allowed: " + mimeType);
		}
		if (!matchesSignature(mimeType, content)) {
			throw new DomainValidationException("Evidence content does not match its declared MIME type.");
		}
		if (command.originalFilename() != null && command.originalFilename().length() > 255) {
			throw new DomainValidationException("Evidence original filename cannot exceed 255 characters.");
		}
		if (command.capturedAt() != null && command.capturedAt().isAfter(clock.instant())) {
			throw new DomainValidationException("Evidence capture time cannot be in the future.");
		}
		validateCoordinates(command.latitude(), command.longitude());
		return targetType;
	}

	private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
		if ((latitude == null) != (longitude == null)) {
			throw new DomainValidationException("Evidence latitude and longitude must be provided together.");
		}
		if (latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
				|| latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
			throw new DomainValidationException("Evidence latitude must be between -90 and 90.");
		}
		if (longitude != null && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
				|| longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
			throw new DomainValidationException("Evidence longitude must be between -180 and 180.");
		}
	}

	private boolean matchesSignature(String mimeType, byte[] content) {
		return switch (mimeType) {
			case "image/jpeg" -> startsWith(content, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
			case "image/png" -> startsWith(content, new byte[] {
					(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
			case "image/webp" -> asciiAt(content, 0, "RIFF") && asciiAt(content, 8, "WEBP");
			case "application/pdf" -> asciiAt(content, 0, "%PDF-");
			case "video/mp4" -> asciiAt(content, 4, "ftyp");
			default -> false;
		};
	}

	private boolean startsWith(byte[] content, byte[] prefix) {
		return content.length >= prefix.length
				&& Arrays.equals(Arrays.copyOf(content, prefix.length), prefix);
	}

	private boolean asciiAt(byte[] content, int offset, String signature) {
		byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
		if (content.length < offset + expected.length) {
			return false;
		}
		for (int index = 0; index < expected.length; index++) {
			if (content[offset + index] != expected[index]) {
				return false;
			}
		}
		return true;
	}
}
