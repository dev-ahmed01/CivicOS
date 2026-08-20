package com.civicos.evidence.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.evidence.application.EvidenceResult;
import com.civicos.evidence.application.EvidenceReviewCommand;
import com.civicos.evidence.application.EvidenceService;
import com.civicos.evidence.application.EvidenceUploadCommand;
import com.civicos.evidence.domain.Evidence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/evidence")
@Tag(name = "Evidence")
public class EvidenceController {

	private final EvidenceService evidenceService;
	private final ApiIdempotencyService idempotencyService;

	public EvidenceController(EvidenceService evidenceService, ApiIdempotencyService idempotencyService) {
		this.evidenceService = evidenceService;
		this.idempotencyService = idempotencyService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Upload evidence with trusted server-side checksum and metadata validation")
	public EvidenceResult upload(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestPart("metadata") UploadRequest body,
			@RequestPart("file") MultipartFile file,
			HttpServletRequest request) {
		byte[] content = content(file);
		UploadFingerprint fingerprint = new UploadFingerprint(body, sha256(content));
		return idempotencyService.execute(idempotencyKey, "EVIDENCE_UPLOAD", fingerprint,
				EvidenceResult.class, () -> evidenceService.upload(new EvidenceUploadCommand(
						body.targetType(), body.targetId(), body.type(), file.getOriginalFilename(),
						file.getContentType(), body.capturedAt(), body.latitude(), body.longitude(),
						body.metadata()), content, CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/{evidenceId}/submit")
	public EvidenceResult submit(
			@PathVariable UUID evidenceId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @org.springframework.web.bind.annotation.RequestBody VersionReasonRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "EVIDENCE_SUBMIT:" + evidenceId, body,
				EvidenceResult.class, () -> evidenceService.submitForReview(evidenceId,
						body.expectedVersion(), body.reason(), CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/{evidenceId}/review")
	public EvidenceResult review(
			@PathVariable UUID evidenceId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @org.springframework.web.bind.annotation.RequestBody ReviewRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "EVIDENCE_REVIEW:" + evidenceId, body,
				EvidenceResult.class, () -> evidenceService.review(evidenceId,
						new EvidenceReviewCommand(body.decision(), body.reason(), body.expectedVersion()),
						CorrelationIdFilter.requestId(request)));
	}

	private byte[] content(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw new DomainValidationException("Evidence file could not be read.");
		}
	}

	private String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	public record UploadRequest(
			@NotBlank String targetType,
			@NotNull UUID targetId,
			@NotNull Evidence.Type type,
			Instant capturedAt,
			BigDecimal latitude,
			BigDecimal longitude,
			Map<String, Object> metadata) {
	}

	public record VersionReasonRequest(long expectedVersion, @NotBlank String reason) {
	}

	public record ReviewRequest(
			@NotNull Evidence.Status decision,
			String reason,
			long expectedVersion) {
	}

	private record UploadFingerprint(UploadRequest metadata, String contentSha256) {
	}
}
