package com.civicos.dependency.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.dependency.api.DependencyResponse;
import com.civicos.dependency.repository.DependencyRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;

@Service
public class DependencyQueryService {

	private final DependencyRepository dependencyRepository;
	private final InterventionRepository interventionRepository;
	private final AuthorizationService authorizationService;

	public DependencyQueryService(
			DependencyRepository dependencyRepository,
			InterventionRepository interventionRepository,
			AuthorizationService authorizationService) {
		this.dependencyRepository = dependencyRepository;
		this.interventionRepository = interventionRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public List<DependencyResponse> forIntervention(UUID interventionId) {
		authorizationService.authorize(PermissionCode.INTERVENTION_VIEW);
		Intervention intervention = interventionRepository.findById(interventionId)
				.orElseThrow(() -> new NoSuchElementException(
						"Intervention not found: " + interventionId));
		if (!authorizationService.canAccessAgency(intervention.getAgency().getId())) {
			throw new AccessDeniedException("The intervention is outside the actor's agency scope.");
		}
		return dependencyRepository.findAllConnectedTo(List.of(interventionId)).stream()
				.map(DependencyResponse::from)
				.toList();
	}
}
