package com.civicos.dependency.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.dependency.domain.Dependency;

public interface DependencyRepository extends JpaRepository<Dependency, UUID> {
	List<Dependency> findBySourceInterventionId(UUID sourceInterventionId);
	List<Dependency> findByTargetInterventionId(UUID targetInterventionId);
	boolean existsBySourceInterventionIdAndTargetInterventionIdAndType(
			UUID sourceInterventionId, UUID targetInterventionId, Dependency.Type type);
}
