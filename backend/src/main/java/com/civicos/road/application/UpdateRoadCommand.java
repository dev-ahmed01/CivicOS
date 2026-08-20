package com.civicos.road.application;

import org.locationtech.jts.geom.MultiLineString;

import com.civicos.road.domain.Road;

public record UpdateRoadCommand(
		String name,
		Road.Classification classification,
		MultiLineString geometry,
		long expectedVersion) {
}
