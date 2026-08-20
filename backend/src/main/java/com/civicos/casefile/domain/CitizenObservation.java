package com.civicos.casefile.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import com.civicos.common.persistence.AbstractAuditableEntity;
import com.civicos.common.validation.GeometryRules;
import com.civicos.common.validation.ValidationRules;
import com.civicos.road.domain.RoadSegment;
import com.civicos.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "citizen_observations")
public class CitizenObservation extends AbstractAuditableEntity {

	public enum Status { SUBMITTED, TRIAGED, MATCHED, FLAGGED, FORWARDED, RESOLVED, DISMISSED, DUPLICATE }

	@ManyToOne(optional = false)
	@JoinColumn(name = "case_id", nullable = false)
	private CivicCase civicCase;

	@ManyToOne(optional = false)
	@JoinColumn(name = "submitted_by", nullable = false)
	private User submittedBy;

	@Column(nullable = false, length = 60)
	private String category;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(nullable = false, columnDefinition = "geometry(Point,4326)")
	private Point location;

	@ManyToOne(optional = false)
	@JoinColumn(name = "road_segment_id", nullable = false)
	private RoadSegment roadSegment;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private Instant submittedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private Status status = Status.SUBMITTED;

	@Column(name = "ai_suggested_category", length = 60)
	private String aiSuggestedCategory;

	@Column(name = "ai_confidence", precision = 5, scale = 4)
	private BigDecimal aiConfidence;

	protected CitizenObservation() {
	}

	public static CitizenObservation create(
			CivicCase civicCase,
			User submittedBy,
			String category,
			String description,
			Point location,
			RoadSegment roadSegment) {
		CitizenObservation observation = new CitizenObservation();
		observation.civicCase = ValidationRules.required(civicCase, "Case");
		observation.submittedBy = ValidationRules.required(submittedBy, "Submitting citizen");
		observation.category = ValidationRules.requiredText(category, "Observation category");
		observation.description = ValidationRules.requiredText(description, "Observation description");
		observation.location = GeometryRules.validWgs84(location, "Observation location");
		observation.roadSegment = ValidationRules.required(roadSegment, "Road segment");
		return observation;
	}

	public CivicCase getCivicCase() { return civicCase; }
	public User getSubmittedBy() { return submittedBy; }
	public String getCategory() { return category; }
	public String getDescription() { return description; }
	public Point getLocation() { return location; }
	public RoadSegment getRoadSegment() { return roadSegment; }
	public Instant getSubmittedAt() { return submittedAt; }
	public Status getStatus() { return status; }
	public String getAiSuggestedCategory() { return aiSuggestedCategory; }
	public BigDecimal getAiConfidence() { return aiConfidence; }
}
