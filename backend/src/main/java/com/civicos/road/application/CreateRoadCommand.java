package com.civicos.road.application;

import org.locationtech.jts.geom.MultiLineString;

import com.civicos.road.domain.Road;

public record CreateRoadCommand(
		String externalReference,
		String name,
		Road.Classification classification,
		MultiLineString geometry) {
}
