package com.civicos.road.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.road.domain.RoadSegment;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, UUID> {

	Optional<RoadSegment> findByExternalReference(String externalReference);

	List<RoadSegment> findByActiveTrueOrderByNameAsc();

	@Query(value = """
			select rs.*
			from road_segments rs
			where rs.active = true
			  and ST_Intersects(rs.geometry, :geometry)
			order by rs.external_reference
			""", nativeQuery = true)
	List<RoadSegment> findActiveIntersecting(@Param("geometry") Geometry geometry);

	@Query(value = """
			select rs.*
			from road_segments rs
			where rs.active = true
			  and ST_DWithin(
			      rs.geometry::geography,
			      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
			      :radiusMeters)
			order by ST_Distance(
			    rs.geometry::geography,
			    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)
			""", nativeQuery = true)
	List<RoadSegment> findActiveWithinRadius(
			@Param("longitude") double longitude,
			@Param("latitude") double latitude,
			@Param("radiusMeters") double radiusMeters);
}
