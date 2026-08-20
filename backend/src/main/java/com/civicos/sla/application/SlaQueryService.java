package com.civicos.sla.application;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
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
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.sla.api.SlaResponse;
import com.civicos.sla.domain.Sla;
import com.civicos.sla.repository.SlaRepository;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class SlaQueryService {

	private final SlaRepository slaRepository;
	private final InterventionRepository interventionRepository;
	private final AuthorizationService authorizationService;

	public SlaQueryService(
			SlaRepository slaRepository,
			InterventionRepository interventionRepository,
			AuthorizationService authorizationService) {
		this.slaRepository = slaRepository;
		this.interventionRepository = interventionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<SlaResponse> list(
			Set<Sla.Status> statuses,
			Sla.Type slaType,
			String targetType,
			Set<UUID> targetIds,
			Pageable pageable) {
		CivicPrincipal principal = principal();
		String normalizedType = targetType == null ? null : targetType.strip().toUpperCase(Locale.ROOT);
		Specification<Sla> specification = scope(principal);
		if (statuses != null && !statuses.isEmpty()) {
			specification = specification.and((root, query, builder) -> root.get("status").in(statuses));
		}
		if (slaType != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("slaType"), slaType));
		}
		if (normalizedType != null && !normalizedType.isEmpty()) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("targetType"), normalizedType));
		}
		if (targetIds != null && !targetIds.isEmpty()) {
			specification = specification.and((root, query, builder) ->
					root.get("targetId").in(targetIds));
		}
		return slaRepository.findAll(specification, pageable).map(SlaResponse::from);
	}

	@Transactional(readOnly = true)
	public SlaResponse byId(UUID slaId) {
		CivicPrincipal principal = principal();
		Sla sla = slaRepository.findById(slaId)
				.orElseThrow(() -> new NoSuchElementException("SLA not found: " + slaId));
		if (!visible(sla, principal)) {
			throw new AccessDeniedException("The SLA is outside the actor's agency scope.");
		}
		return SlaResponse.from(sla);
	}

	private CivicPrincipal principal() {
		authorizationService.authorize(PermissionCode.SLA_VIEW);
		return authorizationService.currentPrincipal();
	}

	private Specification<Sla> scope(CivicPrincipal principal) {
		if (global(principal)) {
			return Specification.allOf();
		}
		if (principal.agencyId() == null) {
			throw new AccessDeniedException("An agency scope is required to list SLAs.");
		}
		return (root, query, builder) -> {
			Subquery<UUID> agencyInterventions = query.subquery(UUID.class);
			Root<Intervention> intervention = agencyInterventions.from(Intervention.class);
			agencyInterventions.select(intervention.get("id")).where(
					builder.equal(intervention.get("agency").get("id"), principal.agencyId()));
			return builder.and(
					builder.equal(root.get("targetType"), "INTERVENTION"),
					root.<UUID>get("targetId").in(agencyInterventions));
		};
	}

	private boolean visible(Sla sla, CivicPrincipal principal) {
		if (global(principal)) {
			return true;
		}
		if (!"INTERVENTION".equals(sla.getTargetType())) {
			return false;
		}
		return interventionRepository.findById(sla.getTargetId())
				.map(intervention -> authorizationService.canAccessAgency(intervention.getAgency().getId()))
				.orElse(false);
	}

	private boolean global(CivicPrincipal principal) {
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name());
	}
}
