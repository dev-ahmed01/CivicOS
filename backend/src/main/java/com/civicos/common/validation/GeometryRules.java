package com.civicos.common.validation;

import org.locationtech.jts.geom.Geometry;

import com.civicos.common.domain.DomainValidationException;

public final class GeometryRules {

	public static final int CIVICOS_SRID = 4326;

	private GeometryRules() {
	}

	public static <T extends Geometry> T validWgs84(T geometry, String fieldName) {
		if (geometry == null) {
			throw new DomainValidationException(fieldName + " is required.");
		}
		if (geometry.isEmpty() || !geometry.isValid()) {
			throw new DomainValidationException(fieldName + " must be a valid, non-empty geometry.");
		}
		if (geometry.getSRID() != CIVICOS_SRID) {
			throw new DomainValidationException(fieldName + " must use EPSG:4326.");
		}
		@SuppressWarnings("unchecked")
		T copy = (T) geometry.copy();
		copy.setSRID(CIVICOS_SRID);
		return copy;
	}
}
