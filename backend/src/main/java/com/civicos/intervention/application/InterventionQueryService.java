package com.civicos.intervention.application;

import java.time.Instant;
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
import com.civicos.intervention.api.InterventionResponse;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;

@Service
public class InterventionQueryService {

	private final InterventionRepository interventionRepository;
	private final AuthorizationService authorizationService;

	public InterventionQueryService(
			InterventionRepository interventionRepository,
			AuthorizationService authorizationService) {
		this.interventionRepository = interventionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<InterventionResponse> list(
			Intervention.Status status,
			UUID agencyId,
			UUID roadSegmentId,
			Intervention.Priority priority,
			Instant plannedFrom,
			Instant plannedTo,
			Pageable pageable) {
		authorizationService.authorize(PermissionCode.INTERVENTION_VIEW);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		UUID effectiveAgencyId = scopedAgency(principal, agencyId);

		Specification<Intervention> specification = Specification.allOf();
		if (status != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("status"), status));
		}
		if (effectiveAgencyId != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("agency").get("id"), effectiveAgencyId));
		}
		if (roadSegmentId != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("roadSegment").get("id"), roadSegmentId));
		}
		if (priority != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("priority"), priority));
		}
		if (plannedFrom != null) {
			specification = specification.and((root, query, builder) ->
					builder.greaterThanOrEqualTo(root.get("plannedStart"), plannedFrom));
		}
		if (plannedTo != null) {
			specification = specification.and((root, query, builder) ->
					builder.lessThanOrEqualTo(root.get("plannedEnd"), plannedTo));
		}
		return interventionRepository.findAll(specification, pageable).map(InterventionResponse::from);
	}

	@Transactional(readOnly = true)
	public InterventionResponse byId(UUID interventionId) {
		authorizationService.authorize(PermissionCode.INTERVENTION_VIEW);
		Intervention intervention = interventionRepository.findById(interventionId)
				.orElseThrow(() -> new NoSuchElementException(
						"Intervention not found: " + interventionId));
		if (!authorizationService.canAccessAgency(intervention.getAgency().getId())) {
			throw new AccessDeniedException("The intervention is outside the actor's agency scope.");
		}
		return InterventionResponse.from(intervention);
	}

	private UUID scopedAgency(CivicPrincipal principal, UUID requestedAgencyId) {
		boolean global = principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name());
		if (global) {
			return requestedAgencyId;
		}
		if (principal.agencyId() == null) {
			throw new AccessDeniedException("An agency scope is required to list interventions.");
		}
		if (requestedAgencyId != null && !requestedAgencyId.equals(principal.agencyId())) {
			throw new AccessDeniedException("The requested agency is outside the actor's scope.");
		}
		return principal.agencyId();
	}
}
