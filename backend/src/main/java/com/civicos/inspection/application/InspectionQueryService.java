package com.civicos.inspection.application;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.inspection.api.InspectionResponse;
import com.civicos.inspection.domain.Inspection;
import com.civicos.inspection.repository.InspectionRepository;

@Service
public class InspectionQueryService {

	private final InspectionRepository inspectionRepository;
	private final AuthorizationService authorizationService;

	public InspectionQueryService(
			InspectionRepository inspectionRepository,
			AuthorizationService authorizationService) {
		this.inspectionRepository = inspectionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<InspectionResponse> list(Pageable pageable) {
		CivicPrincipal principal = principal();
		if (global(principal)) {
			return inspectionRepository.findAll(pageable).map(InspectionResponse::from);
		}
		assertInspector(principal);
		return inspectionRepository.findByInspectorId(principal.userId(), pageable)
				.map(InspectionResponse::from);
	}

	@Transactional(readOnly = true)
	public InspectionResponse byId(UUID inspectionId) {
		CivicPrincipal principal = principal();
		Inspection inspection = inspectionRepository.findById(inspectionId)
				.orElseThrow(() -> new NoSuchElementException("Inspection not found: " + inspectionId));
		if (!global(principal) && (!principal.roles().contains(SystemRole.INSPECTOR.name())
				|| !inspection.getInspector().getId().equals(principal.userId()))) {
			throw new AccessDeniedException("Only the assigned inspector can view this inspection.");
		}
		return InspectionResponse.from(inspection);
	}

	private CivicPrincipal principal() {
		authorizationService.authorize(PermissionCode.INSPECTION_VIEW);
		return authorizationService.currentPrincipal();
	}

	private void assertInspector(CivicPrincipal principal) {
		if (!principal.roles().contains(SystemRole.INSPECTOR.name())) {
			throw new AccessDeniedException("Only inspectors can view assigned inspections.");
		}
	}

	private boolean global(CivicPrincipal principal) {
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name());
	}
}
