package com.civicos.road.domain;

import org.locationtech.jts.geom.MultiLineString;

import com.civicos.common.persistence.AbstractAuditableEntity;
import com.civicos.common.validation.GeometryRules;
import com.civicos.common.validation.ValidationRules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "roads")
public class Road extends AbstractAuditableEntity {

	public enum Classification { ARTERIAL, SUB_ARTERIAL, COLLECTOR, LOCAL, OTHER }

	@Column(name = "external_reference", unique = true, length = 100)
	private String externalReference;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private Classification classification;

	@Column(nullable = false, columnDefinition = "geometry(MultiLineString,4326)")
	private MultiLineString geometry;

	@Column(nullable = false)
	private boolean active = true;

	@Version
	@Column(nullable = false)
	private long version;

	protected Road() {
	}

	public static Road create(
			String externalReference,
			String name,
			Classification classification,
			MultiLineString geometry) {
		Road road = new Road();
		road.externalReference = ValidationRules.optionalText(externalReference);
		road.update(name, classification, geometry);
		return road;
	}

	public void update(String name, Classification classification, MultiLineString geometry) {
		this.name = ValidationRules.requiredText(name, "Road name");
		this.classification = ValidationRules.required(classification, "Road classification");
		this.geometry = GeometryRules.validWgs84(geometry, "Road geometry");
	}

	public void deactivate() {
		active = false;
	}

	public String getExternalReference() { return externalReference; }
	public String getName() { return name; }
	public Classification getClassification() { return classification; }
	public MultiLineString getGeometry() { return geometry; }
	public boolean isActive() { return active; }
	public long getVersion() { return version; }

}
