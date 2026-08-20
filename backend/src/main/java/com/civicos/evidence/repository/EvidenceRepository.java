package com.civicos.evidence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.evidence.domain.Evidence;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
	Optional<Evidence> findByFileReference(String fileReference);
	List<Evidence> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, UUID targetId);
}
