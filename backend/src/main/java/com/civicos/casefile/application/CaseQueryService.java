package com.civicos.casefile.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.api.CaseResponse;
import com.civicos.casefile.domain.CitizenObservation;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.casefile.repository.CitizenObservationRepository;
import com.civicos.casefile.repository.CivicCaseRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;

@Service
public class CaseQueryService {

	private final CivicCaseRepository caseRepository;
	private final CitizenObservationRepository observationRepository;
	private final InterventionRepository interventionRepository;
	private final AuthorizationService authorizationService;

	public CaseQueryService(
			CivicCaseRepository caseRepository,
			CitizenObservationRepository observationRepository,
			InterventionRepository interventionRepository,
			AuthorizationService authorizationService) {
		this.caseRepository = caseRepository;
		this.observationRepository = observationRepository;
		this.interventionRepository = interventionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public CaseResponse byId(UUID caseId) {
		CivicPrincipal principal = authorizationService.currentPrincipal();
		assertViewPermission(principal);
		CivicCase civicCase = caseRepository.findById(caseId)
				.orElseThrow(() -> new NoSuchElementException("Civic case not found: " + caseId));
		if (!visible(caseId, principal)) {
			throw new AccessDeniedException("The civic case is outside the actor's permitted scope.");
		}
		List<CitizenObservation> observations =
				observationRepository.findByCivicCaseIdOrderBySubmittedAtAsc(caseId);
		List<Intervention> interventions = interventionRepository.findByCivicCaseIdOrderByCreatedAtAsc(caseId);
		return CaseResponse.from(civicCase, observations, interventions);
	}

	private void assertViewPermission(CivicPrincipal principal) {
		if (!principal.permissions().contains(PermissionCode.OBSERVATION_VIEW_OWN.name())
				&& !principal.permissions().contains(PermissionCode.OBSERVATION_VIEW_RELEVANT.name())
				&& !principal.permissions().contains(PermissionCode.ADMIN_OVERRIDE.name())) {
			throw new AccessDeniedException("A case-view permission is required.");
		}
	}

	private boolean visible(UUID caseId, CivicPrincipal principal) {
		return principal.roles().contains(SystemRole.ADMIN.name())
				|| principal.roles().contains(SystemRole.COORDINATOR.name())
				|| principal.agencyId() != null
				&& interventionRepository.existsByCivicCaseIdAndAgencyId(caseId, principal.agencyId())
				|| observationRepository.existsByCivicCaseIdAndSubmittedById(caseId, principal.userId());
	}
}
