package com.civicos.ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.civicos.ai.domain.AiRun;

public interface AiRunRepository extends JpaRepository<AiRun, UUID> {
	List<AiRun> findByStatusOrderByCreatedAtAsc(AiRun.Status status);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	java.util.Optional<AiRun> findForUpdateById(UUID id);
}
