package com.civicos.verification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.verification.domain.Verification;

public interface VerificationRepository extends JpaRepository<Verification, UUID> {
	List<Verification> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, UUID targetId);
}
