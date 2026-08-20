package com.civicos.sla.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.civicos.sla.domain.Sla;

public interface SlaRepository extends JpaRepository<Sla, UUID> {
	List<Sla> findByTargetTypeAndTargetId(String targetType, UUID targetId);
	List<Sla> findByStatusInAndDeadlineBefore(List<Sla.Status> statuses, Instant deadline);

	@Query("""
			select sla from Sla sla
			where sla.targetType = :targetType and sla.targetId = :targetId
			  and sla.slaType = :slaType
			  and sla.status in :statuses
			""")
	java.util.Optional<Sla> findActive(
			@Param("targetType") String targetType,
			@Param("targetId") UUID targetId,
			@Param("slaType") Sla.Type slaType,
			@Param("statuses") List<Sla.Status> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select sla from Sla sla where sla.id = :id")
	java.util.Optional<Sla> findForUpdate(@Param("id") UUID id);
}
