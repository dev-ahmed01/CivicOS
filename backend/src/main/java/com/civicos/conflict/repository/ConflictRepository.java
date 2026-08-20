package com.civicos.conflict.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.conflict.domain.Conflict;

public interface ConflictRepository extends JpaRepository<Conflict, UUID> {
	Optional<Conflict> findByConflictNumber(String conflictNumber);
	List<Conflict> findByStatusOrderByDetectedAtAsc(Conflict.Status status);
	List<Conflict> findBySeverityAndStatus(Conflict.Severity severity, Conflict.Status status);

	@Query("select distinct c from Conflict c join c.interventions i where i.id = :interventionId")
	List<Conflict> findAllByInterventionId(@Param("interventionId") UUID interventionId);

	@Query("""
			select (count(c) > 0)
			from Conflict c join c.interventions i
			where i.id = :interventionId
			  and c.severity = :severity
			  and c.status in :statuses
			""")
	boolean existsBlockingConflict(
			@Param("interventionId") UUID interventionId,
			@Param("severity") Conflict.Severity severity,
			@Param("statuses") List<Conflict.Status> statuses);

	@Query("""
			select (count(c) > 0)
			from Conflict c join c.interventions i
			where c.id = :conflictId and i.agency.id = :agencyId
			""")
	boolean existsByIdAndInterventionAgencyId(
			@Param("conflictId") UUID conflictId,
			@Param("agencyId") UUID agencyId);

	@Query("""
			select (count(c) > 0)
			from Conflict c join c.interventions i, Inspection inspection
			where c.id = :conflictId
			  and inspection.intervention.id = i.id
			  and inspection.inspector.id = :inspectorId
			""")
	boolean existsByIdAndAssignedInspectorId(
			@Param("conflictId") UUID conflictId,
			@Param("inspectorId") UUID inspectorId);
}
