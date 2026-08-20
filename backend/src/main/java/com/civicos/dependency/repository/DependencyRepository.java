package com.civicos.dependency.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.civicos.dependency.domain.Dependency;

public interface DependencyRepository extends JpaRepository<Dependency, UUID> {
	List<Dependency> findBySourceInterventionId(UUID sourceInterventionId);
	List<Dependency> findByTargetInterventionId(UUID targetInterventionId);
	boolean existsBySourceInterventionIdAndTargetInterventionIdAndType(
			UUID sourceInterventionId, UUID targetInterventionId, Dependency.Type type);

	List<Dependency> findByRequiredTrueAndStatusNot(Dependency.Status status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select dependency from Dependency dependency where dependency.id = :id")
	java.util.Optional<Dependency> findForUpdate(@Param("id") UUID id);
}
