package com.civicos.casefile.domain;

import java.time.Instant;

import com.civicos.common.persistence.AbstractAuditableEntity;
import com.civicos.road.domain.RoadSegment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "civic_cases")
public class CivicCase extends AbstractAuditableEntity {

	public enum Source { CITIZEN, AGENCY, SYSTEM, IMPORT }
	public enum Status { OPEN, UNDER_REVIEW, IN_PROGRESS, PENDING_VERIFICATION, VERIFIED, CLOSED }
	public enum Priority { LOW, NORMAL, HIGH, CRITICAL }

	@Column(name = "case_number", nullable = false, unique = true, length = 50)
	private String caseNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Source source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private Status status = Status.OPEN;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Priority priority = Priority.NORMAL;

	@ManyToOne(optional = false)
	@JoinColumn(name = "road_segment_id", nullable = false)
	private RoadSegment roadSegment;

	@Column(name = "closed_at")
	private Instant closedAt;

	@Version
	@Column(nullable = false)
	private long version;

	public String getCaseNumber() { return caseNumber; }
	public Source getSource() { return source; }
	public Status getStatus() { return status; }
	public Priority getPriority() { return priority; }
	public RoadSegment getRoadSegment() { return roadSegment; }
	public Instant getClosedAt() { return closedAt; }
	public long getVersion() { return version; }
}
