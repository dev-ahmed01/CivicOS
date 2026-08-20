package com.civicos.road.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.locationtech.jts.geom.LineString;

import com.civicos.road.domain.RoadSegment;

public record CreateRoadSegmentCommand(
		UUID roadId,
		String externalReference,
		String name,
		RoadSegment.Classification classification,
		RoadSegment.SurfaceType surfaceType,
		BigDecimal length,
		LineString geometry) {
}
