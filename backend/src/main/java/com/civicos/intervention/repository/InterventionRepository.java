package com.civicos.intervention.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.intervention.domain.Intervention;

public interface InterventionRepository extends JpaRepository<Intervention, UUID> {

	Optional<Intervention> findByInterventionNumber(String interventionNumber);
	boolean existsByInterventionNumber(String interventionNumber);

	List<Intervention> findByRoadSegmentIdAndStatusIn(UUID roadSegmentId, List<Intervention.Status> statuses);
	boolean existsByRoadSegmentIdAndStatusNot(UUID roadSegmentId, Intervention.Status status);

	@Query(value = """
			select i.*
			from interventions i
			where (:excludedId is null or i.id <> :excludedId)
			  and i.status <> 'CLOSED'
			  and i.planned_start < :plannedEnd
			  and i.planned_end > :plannedStart
			  and ST_Intersects(i.geometry, :geometry)
			order by i.planned_start, i.intervention_number
			""", nativeQuery = true)
	List<Intervention> findSpatialTemporalCandidates(
			@Param("excludedId") UUID excludedId,
			@Param("geometry") Geometry geometry,
			@Param("plannedStart") Instant plannedStart,
			@Param("plannedEnd") Instant plannedEnd);
}
