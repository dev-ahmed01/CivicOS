package com.civicos.evidence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.evidence.domain.Evidence;

import jakarta.persistence.LockModeType;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
	Optional<Evidence> findByFileReference(String fileReference);
	List<Evidence> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, UUID targetId);
	boolean existsByTargetTypeAndTargetIdAndTypeAndStatus(
			String targetType, UUID targetId, Evidence.Type type, Evidence.Status status);
	boolean existsByTargetTypeAndTargetIdAndUploadedByIdAndTypeIn(
			String targetType, UUID targetId, UUID uploadedById, List<Evidence.Type> types);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select evidence from Evidence evidence where evidence.id = :id")
	Optional<Evidence> findForUpdate(@Param("id") UUID id);
}
