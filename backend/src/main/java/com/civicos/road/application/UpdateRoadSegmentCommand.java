package com.civicos.road.application;

import java.math.BigDecimal;

import org.locationtech.jts.geom.LineString;

import com.civicos.road.domain.RoadSegment;

public record UpdateRoadSegmentCommand(
		String name,
		RoadSegment.Classification classification,
		RoadSegment.SurfaceType surfaceType,
		BigDecimal length,
		LineString geometry,
		long expectedVersion) {
}
