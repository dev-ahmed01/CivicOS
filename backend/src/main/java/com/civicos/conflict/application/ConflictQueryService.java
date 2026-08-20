package com.civicos.conflict.application;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.conflict.api.ConflictResponse;
import com.civicos.conflict.domain.Conflict;
import com.civicos.conflict.repository.ConflictRepository;
import com.civicos.inspection.domain.Inspection;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class ConflictQueryService {

	private final ConflictRepository conflictRepository;
	private final AuthorizationService authorizationService;

	public ConflictQueryService(
			ConflictRepository conflictRepository,
			AuthorizationService authorizationService) {
		this.conflictRepository = conflictRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<ConflictResponse> list(
			Conflict.Status status,
			Conflict.Severity severity,
			UUID roadSegmentId,
			Pageable pageable) {
		authorizationService.authorize(PermissionCode.CONFLICT_VIEW);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		Specification<Conflict> specification = scope(principal);
		if (status != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("status"), status));
		}
		if (severity != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("severity"), severity));
		}
		if (roadSegmentId != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("roadSegment").get("id"), roadSegmentId));
		}
		return conflictRepository.findAll(specification, pageable).map(ConflictResponse::from);
	}

	@Transactional(readOnly = true)
	public ConflictResponse byId(UUID conflictId) {
		authorizationService.authorize(PermissionCode.CONFLICT_VIEW);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		Conflict conflict = conflictRepository.findById(conflictId)
				.orElseThrow(() -> new NoSuchElementException("Conflict not found: " + conflictId));
		if (!visible(conflictId, principal)) {
			throw new AccessDeniedException("The conflict is outside the actor's permitted scope.");
		}
		return ConflictResponse.from(conflict);
	}

	private Specification<Conflict> scope(CivicPrincipal principal) {
		if (principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())) {
			return Specification.allOf();
		}
		if (principal.agencyId() != null) {
			return (root, query, builder) -> {
				query.distinct(true);
				return builder.equal(
						root.join("interventions", JoinType.INNER).get("agency").get("id"),
						principal.agencyId());
			};
		}
		if (principal.roles().contains(SystemRole.INSPECTOR.name())) {
			return (root, query, builder) -> {
				query.distinct(true);
				var affectedIntervention = root.join("interventions", JoinType.INNER);
				Subquery<Integer> assigned = query.subquery(Integer.class);
				Root<Inspection> inspection = assigned.from(Inspection.class);
				assigned.select(builder.literal(1)).where(
						builder.equal(inspection.get("inspector").get("id"), principal.userId()),
						builder.equal(inspection.get("intervention").get("id"),
								affectedIntervention.get("id")));
				return builder.exists(assigned);
			};
		}
		throw new AccessDeniedException("The actor has no conflict resource scope.");
	}

	private boolean visible(UUID conflictId, CivicPrincipal principal) {
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())
				|| principal.agencyId() != null
				&& conflictRepository.existsByIdAndInterventionAgencyId(conflictId, principal.agencyId())
				|| principal.roles().contains(SystemRole.INSPECTOR.name())
				&& conflictRepository.existsByIdAndAssignedInspectorId(conflictId, principal.userId());
	}
}
