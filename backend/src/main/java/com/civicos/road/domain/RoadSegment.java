package com.civicos.road.domain;

import java.math.BigDecimal;

import org.locationtech.jts.geom.LineString;

import com.civicos.common.persistence.AbstractAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "road_segments")
public class RoadSegment extends AbstractAuditableEntity {

	public enum Classification { ARTERIAL, SUB_ARTERIAL, COLLECTOR, LOCAL, OTHER }
	public enum SurfaceType { BITUMINOUS, CONCRETE, PAVER_BLOCK, GRAVEL, EARTH, OTHER }

	@ManyToOne
	@JoinColumn(name = "road_id")
	private Road road;

	@Column(name = "external_reference", nullable = false, unique = true, length = 100)
	private String externalReference;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private Classification classification;

	@Enumerated(EnumType.STRING)
	@Column(name = "surface_type", nullable = false, length = 50)
	private SurfaceType surfaceType;

	@Column(name = "length_meters", nullable = false, precision = 12, scale = 2)
	private BigDecimal length;

	@Column(nullable = false, columnDefinition = "geometry(LineString,4326)")
	private LineString geometry;

	@Column(nullable = false)
	private boolean active = true;

	public Road getRoad() { return road; }
	public String getExternalReference() { return externalReference; }
	public String getName() { return name; }
	public Classification getClassification() { return classification; }
	public SurfaceType getSurfaceType() { return surfaceType; }
	public BigDecimal getLength() { return length; }
	public LineString getGeometry() { return geometry; }
	public boolean isActive() { return active; }
}
