package com.civicos.evidence.application;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.Set;

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
import com.civicos.evidence.api.EvidenceResponse;
import com.civicos.evidence.domain.Evidence;
import com.civicos.evidence.repository.EvidenceRepository;
import com.civicos.inspection.domain.Inspection;
import com.civicos.inspection.repository.InspectionRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class EvidenceQueryService {

	private final EvidenceRepository evidenceRepository;
	private final InterventionRepository interventionRepository;
	private final InspectionRepository inspectionRepository;
	private final AuthorizationService authorizationService;

	public EvidenceQueryService(
			EvidenceRepository evidenceRepository,
			InterventionRepository interventionRepository,
			InspectionRepository inspectionRepository,
			AuthorizationService authorizationService) {
		this.evidenceRepository = evidenceRepository;
		this.interventionRepository = interventionRepository;
		this.inspectionRepository = inspectionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<EvidenceResponse> list(
			String targetType,
			Set<UUID> targetIds,
			Evidence.Status status,
			Pageable pageable) {
		CivicPrincipal principal = principal();
		String normalizedType = targetType == null ? null : targetType.strip().toUpperCase(Locale.ROOT);
		Specification<Evidence> specification = scope(principal);
		if (normalizedType != null && !normalizedType.isEmpty()) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("targetType"), normalizedType));
		}
		if (targetIds != null && !targetIds.isEmpty()) {
			specification = specification.and((root, query, builder) ->
					root.get("targetId").in(targetIds));
		}
		if (status != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("status"), status));
		}
		return evidenceRepository.findAll(specification, pageable).map(EvidenceResponse::from);
	}

	@Transactional(readOnly = true)
	public EvidenceResponse byId(UUID evidenceId) {
		CivicPrincipal principal = principal();
		Evidence evidence = evidenceRepository.findById(evidenceId)
				.orElseThrow(() -> new NoSuchElementException("Evidence not found: " + evidenceId));
		if (!visible(evidence, principal)) {
			throw new AccessDeniedException("The evidence is outside the actor's agency scope.");
		}
		return EvidenceResponse.from(evidence);
	}

	private CivicPrincipal principal() {
		authorizationService.authorize(PermissionCode.EVIDENCE_VIEW);
		return authorizationService.currentPrincipal();
	}

	private Specification<Evidence> scope(CivicPrincipal principal) {
		if (global(principal)) {
			return Specification.allOf();
		}
		if (principal.roles().contains(SystemRole.INSPECTOR.name())) {
			return (root, query, builder) -> {
				Subquery<UUID> assignedInterventions = query.subquery(UUID.class);
				Root<Inspection> inspection = assignedInterventions.from(Inspection.class);
				assignedInterventions.select(inspection.get("intervention").get("id")).where(
						builder.equal(inspection.get("inspector").get("id"), principal.userId()));
				return builder.and(
						builder.equal(root.get("targetType"), "INTERVENTION"),
						root.<UUID>get("targetId").in(assignedInterventions));
			};
		}
		if (principal.agencyId() == null) {
			throw new AccessDeniedException("An agency scope is required to list evidence.");
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

	private boolean visible(Evidence evidence, CivicPrincipal principal) {
		if (global(principal)) {
			return true;
		}
		if (!"INTERVENTION".equals(evidence.getTargetType())) {
			return false;
		}
		if (principal.roles().contains(SystemRole.INSPECTOR.name())) {
			return inspectionRepository.existsByInterventionIdAndInspectorId(
					evidence.getTargetId(), principal.userId());
		}
		return interventionRepository.findById(evidence.getTargetId())
				.map(intervention -> authorizationService.canAccessAgency(intervention.getAgency().getId()))
				.orElse(false);
	}

	private boolean global(CivicPrincipal principal) {
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name());
	}
}
