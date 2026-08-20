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

@Entity
@Table(name = "evidence")
public class Evidence extends AbstractCreatedEntity {

	public enum Status { UPLOADED, UNDER_REVIEW, ACCEPTED, REJECTED, SUPERSEDED }

	@Column(name = "target_type", nullable = false, length = 60)
	private String targetType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "uploaded_by", nullable = false)
	private User uploadedBy;

	@Column(name = "evidence_type", nullable = false, length = 60)
	private String type;

	@Column(name = "file_reference", nullable = false, unique = true, length = 500)
	private String fileReference;

	@Column(name = "original_filename")
	private String originalFilename;

	@Column(name = "mime_type", length = 100)
	private String mimeType;

	@Column(name = "file_size_bytes")
	private Long fileSizeBytes;

	@Column(length = 128)
	private String checksum;

	@Column(name = "captured_at")
	private Instant capturedAt;

	@Column(precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "captured_location", columnDefinition = "geometry(Point,4326)")
	private Point capturedLocation;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata = new LinkedHashMap<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status = Status.UPLOADED;

	public String getTargetType() { return targetType; }
	public UUID getTargetId() { return targetId; }
	public User getUploadedBy() { return uploadedBy; }
	public String getType() { return type; }
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
}
