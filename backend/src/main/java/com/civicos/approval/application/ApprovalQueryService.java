package com.civicos.approval.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.approval.api.ApprovalResponse;
import com.civicos.approval.domain.Approval;
import com.civicos.approval.repository.ApprovalRepository;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;

@Service
public class ApprovalQueryService {

	private final ApprovalRepository approvalRepository;
	private final InterventionRepository interventionRepository;
	private final AuthorizationService authorizationService;

	public ApprovalQueryService(
			ApprovalRepository approvalRepository,
			InterventionRepository interventionRepository,
			AuthorizationService authorizationService) {
		this.approvalRepository = approvalRepository;
		this.interventionRepository = interventionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<ApprovalResponse> list(
			Approval.Status status, UUID interventionId, Pageable pageable) {
		CivicPrincipal principal = principal();
		UUID agencyId = scopedAgency(principal);
		Specification<Approval> specification = Specification.allOf();
		if (status != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("status"), status));
		}
		if (interventionId != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("intervention").get("id"), interventionId));
		}
		if (agencyId != null) {
			specification = specification.and((root, query, builder) ->
					builder.equal(root.get("intervention").get("agency").get("id"), agencyId));
		}
		return approvalRepository.findAll(specification, pageable).map(ApprovalResponse::from);
	}

	@Transactional(readOnly = true)
	public List<ApprovalResponse> forIntervention(UUID interventionId) {
		Intervention intervention = intervention(interventionId);
		assertVisible(intervention);
		return approvalRepository.findByInterventionIdOrderByCreatedAtAsc(interventionId).stream()
				.map(ApprovalResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ApprovalResponse byId(UUID approvalId) {
		principal();
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new NoSuchElementException("Approval not found: " + approvalId));
		assertVisible(approval.getIntervention());
		return ApprovalResponse.from(approval);
	}

	private CivicPrincipal principal() {
		authorizationService.authorize(PermissionCode.APPROVAL_VIEW);
		return authorizationService.currentPrincipal();
	}

	private UUID scopedAgency(CivicPrincipal principal) {
		if (principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())) {
			return null;
		}
		if (principal.agencyId() == null) {
			throw new AccessDeniedException("An agency scope is required to view approvals.");
		}
		return principal.agencyId();
	}

	private Intervention intervention(UUID interventionId) {
		principal();
		return interventionRepository.findById(interventionId)
				.orElseThrow(() -> new NoSuchElementException(
						"Intervention not found: " + interventionId));
	}

	private void assertVisible(Intervention intervention) {
		if (!authorizationService.canAccessAgency(intervention.getAgency().getId())) {
			throw new AccessDeniedException("The approval is outside the actor's agency scope.");
		}
	}
}
