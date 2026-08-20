package com.civicos.evidence.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import com.civicos.common.persistence.AbstractCreatedEntity;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "evidence")
public class Evidence extends AbstractCreatedEntity {

	public enum Type {
		BEFORE_WORK, DURING_WORK, COMPLETION, RESTORATION,
		INSPECTION, CITIZEN_VALIDATION, DOCUMENT
	}
	public enum Status { UPLOADED, UNDER_REVIEW, ACCEPTED, REJECTED, SUPERSEDED }

	@Column(name = "target_type", nullable = false, updatable = false, length = 60)
	private String targetType;

	@Column(name = "target_id", nullable = false, updatable = false)
	private UUID targetId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
	private User uploadedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "evidence_type", nullable = false, updatable = false, length = 60)
	private Type type;

	@Column(name = "file_reference", nullable = false, unique = true, updatable = false, length = 500)
	private String fileReference;

	@Column(name = "original_filename", updatable = false)
	private String originalFilename;

	@Column(name = "mime_type", updatable = false, length = 100)
	private String mimeType;

	@Column(name = "file_size_bytes", updatable = false)
	private Long fileSizeBytes;

	@Column(updatable = false, length = 128)
	private String checksum;

	@Column(name = "captured_at", updatable = false)
	private Instant capturedAt;

	@Column(updatable = false, precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(updatable = false, precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "captured_location", updatable = false, columnDefinition = "geometry(Point,4326)")
	private Point capturedLocation;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata = new LinkedHashMap<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.UPLOADED;

	@Version
	@Column(nullable = false)
	private long version;

	protected Evidence() {
	}

	public static Evidence uploaded(
			String targetType,
			UUID targetId,
			User uploadedBy,
			Type type,
			String fileReference,
			String originalFilename,
			String mimeType,
			long fileSizeBytes,
			String checksum,
			Instant capturedAt,
			BigDecimal latitude,
			BigDecimal longitude,
			Point capturedLocation,
			Map<String, Object> metadata) {
		Evidence evidence = new Evidence();
		evidence.targetType = targetType;
		evidence.targetId = targetId;
		evidence.uploadedBy = uploadedBy;
		evidence.type = type;
		evidence.fileReference = fileReference;
		evidence.originalFilename = originalFilename;
		evidence.mimeType = mimeType;
		evidence.fileSizeBytes = fileSizeBytes;
		evidence.checksum = checksum;
		evidence.capturedAt = capturedAt;
		evidence.latitude = latitude;
		evidence.longitude = longitude;
		evidence.capturedLocation = capturedLocation;
		evidence.metadata = new LinkedHashMap<>(metadata);
		return evidence;
	}

	public void submitForReview() {
		if (status != Status.UPLOADED) {
			throw new com.civicos.common.domain.DomainConflictException(
					"Only uploaded evidence can be submitted for review.");
		}
		status = Status.UNDER_REVIEW;
	}

	public void review(Status decision) {
		if (status != Status.UNDER_REVIEW) {
			throw new com.civicos.common.domain.DomainConflictException(
					"Only evidence under review can receive a decision.");
		}
		if (decision != Status.ACCEPTED && decision != Status.REJECTED) {
			throw new com.civicos.common.domain.DomainValidationException(
					"Evidence review decision must be ACCEPTED or REJECTED.");
		}
		status = decision;
	}

	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public User getUploadedBy() { return uploadedBy; }
	public Type getType() { return type; }
	public String getFileReference() { return fileReference; }
	public String getOriginalFilename() { return originalFilename; }
	public String getMimeType() { return mimeType; }
	public Long getFileSizeBytes() { return fileSizeBytes; }
	public String getChecksum() { return checksum; }
	public Instant getCapturedAt() { return capturedAt; }
	public BigDecimal getLatitude() { return latitude; }
	public BigDecimal getLongitude() { return longitude; }
	public Point getCapturedLocation() { return capturedLocation; }
	public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
	public Status getStatus() { return status; }
	public long getVersion() { return version; }
}
