package com.civicos.coordination.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.coordination.repository.CoordinationDecisionRepository;

@Service
public class CoordinationDecisionQueryService {

	private final CoordinationDecisionRepository decisionRepository;
	private final ConflictRepository conflictRepository;
	private final ScopedAuthorizationService authorizationService;

	public CoordinationDecisionQueryService(
			CoordinationDecisionRepository decisionRepository,
			ConflictRepository conflictRepository,
			ScopedAuthorizationService authorizationService) {
		this.decisionRepository = decisionRepository;
		this.conflictRepository = conflictRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public List<CoordinationDecisionResult> forConflict(UUID conflictId) {
		authorizationService.authorize(
				PermissionCode.COORDINATION_VIEW, null, SystemRole.COORDINATOR);
		if (!conflictRepository.existsById(conflictId)) {
			throw new NoSuchElementException("Conflict not found: " + conflictId);
		}
		return decisionRepository.findByConflictIdOrderByCreatedAtAsc(conflictId).stream()
				.map(CoordinationDecisionResult::from)
				.toList();
	}
}
