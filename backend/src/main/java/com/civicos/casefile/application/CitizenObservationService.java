package com.civicos.casefile.application;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.api.CitizenObservationResponse;
import com.civicos.casefile.api.RoadCandidateResponse;
import com.civicos.casefile.domain.CitizenObservation;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.casefile.repository.CitizenObservationRepository;
import com.civicos.casefile.repository.CivicCaseRepository;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.road.domain.RoadSegment;
import com.civicos.road.repository.RoadSegmentRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;
import com.civicos.verification.domain.Verification;
import com.civicos.verification.repository.VerificationRepository;

@Service
public class CitizenObservationService {

	private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);
	private static final double ROAD_MATCH_RADIUS_METERS = 150.0;

	private final CitizenObservationRepository observationRepository;
	private final CivicCaseRepository caseRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final VerificationRepository verificationRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService scopedAuthorizationService;
	private final AuthorizationService authorizationService;
	private final Clock clock;

	public CitizenObservationService(
			CitizenObservationRepository observationRepository,
			CivicCaseRepository caseRepository,
			RoadSegmentRepository roadSegmentRepository,
			VerificationRepository verificationRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService scopedAuthorizationService,
			AuthorizationService authorizationService,
			Clock clock) {
		this.observationRepository = observationRepository;
		this.caseRepository = caseRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.verificationRepository = verificationRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.scopedAuthorizationService = scopedAuthorizationService;
		this.authorizationService = authorizationService;
		this.clock = clock;
	}

	@Transactional
	public CitizenObservationResponse create(
			CreateCitizenObservationCommand command,
			String requestId) {
		ValidationRules.required(command, "Citizen observation command");
		CivicPrincipal principal = scopedAuthorizationService.authorize(
				PermissionCode.OBSERVATION_CREATE, null, SystemRole.CITIZEN);
		User citizen = user(principal.userId());
		Point location = point(command.latitude(), command.longitude());
		RoadSegment roadSegment = roadSegment(command);
		CivicCase civicCase = CivicCase.create(
				caseNumber(), CivicCase.Source.CITIZEN, CivicCase.Priority.NORMAL, roadSegment);
		caseRepository.saveAndFlush(civicCase);
		CitizenObservation observation = CitizenObservation.create(
				civicCase, citizen, command.category(), command.description(), location, roadSegment);
		observationRepository.saveAndFlush(observation);
		auditEventRepository.save(AuditEvent.domainMutation(
				citizen, "CITIZEN_OBSERVATION_SUBMITTED", "CITIZEN_OBSERVATION",
				observation.getId(), null, state(observation), null, requestId));
		return CitizenObservationResponse.from(observation);
	}

	@Transactional(readOnly = true)
	public CitizenObservationResponse byId(UUID observationId) {
		CitizenObservation observation = observation(observationId);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		if (observation.getSubmittedBy().getId().equals(principal.userId())) {
			authorizationService.authorize(PermissionCode.OBSERVATION_VIEW_OWN);
		} else {
			authorizationService.authorize(PermissionCode.OBSERVATION_VIEW_RELEVANT);
			if (principal.roles().contains(SystemRole.CITIZEN.name())) {
				throw new AccessDeniedException("Citizens may view only their own observations.");
			}
		}
		return CitizenObservationResponse.from(observation);
	}

	@Transactional(readOnly = true)
	public Page<CitizenObservationResponse> mine(Pageable pageable) {
		CivicPrincipal principal = scopedAuthorizationService.authorize(
				PermissionCode.OBSERVATION_VIEW_OWN, null, SystemRole.CITIZEN);
		return observationRepository.findBySubmittedById(principal.userId(), pageable)
				.map(CitizenObservationResponse::from);
	}

	@Transactional(readOnly = true)
	public List<RoadCandidateResponse> roadCandidates(double latitude, double longitude) {
		scopedAuthorizationService.authorize(
				PermissionCode.OBSERVATION_CREATE, null, SystemRole.CITIZEN);
		validateCoordinates(latitude, longitude);
		return roadSegmentRepository.findActiveWithinRadius(
				longitude, latitude, ROAD_MATCH_RADIUS_METERS).stream()
				.limit(5)
				.map(RoadCandidateResponse::from)
				.toList();
	}

	@Transactional
	public CitizenValidationResult validateResolution(
			UUID observationId,
			CitizenValidationDecision decision,
			String reason,
			String requestId) {
		CivicPrincipal principal = scopedAuthorizationService.authorize(
				PermissionCode.CITIZEN_VALIDATION_CREATE, null, SystemRole.CITIZEN);
		CitizenObservation observation = observation(observationId);
		if (!observation.getSubmittedBy().getId().equals(principal.userId())) {
			throw new AccessDeniedException("Citizens may validate only their own observation.");
		}
		if (observation.getStatus() != CitizenObservation.Status.RESOLVED) {
			throw new DomainConflictException(
					"Citizen validation is available only after the observation is resolved.");
		}
		ValidationRules.required(decision, "Citizen validation decision");
		String normalizedReason = ValidationRules.optionalText(reason);
		if (decision == CitizenValidationDecision.STILL_UNRESOLVED && normalizedReason == null) {
			throw new DomainValidationException("A reason is required when the issue is still unresolved.");
		}
		if (verificationRepository.existsByTargetTypeAndTargetIdAndSubmittedByIdAndResultIn(
				"CITIZEN_OBSERVATION", observationId, principal.userId(),
				List.of(Verification.Result.CONFIRMED, Verification.Result.DISPUTED))) {
			throw new DomainConflictException("Citizen validation has already been recorded.");
		}
		Verification.Result result = decision == CitizenValidationDecision.LOOKS_RESOLVED
				? Verification.Result.CONFIRMED
				: Verification.Result.DISPUTED;
		User citizen = user(principal.userId());
		Verification verification = Verification.citizen(
				observationId, result, citizen, normalizedReason);
		verificationRepository.saveAndFlush(verification);
		auditEventRepository.save(AuditEvent.domainMutation(
				citizen, "CITIZEN_VALIDATION_RECORDED", "VERIFICATION", verification.getId(),
				null, Map.of(
						"observationId", observationId.toString(),
						"result", result.name(),
						"authoritativeWorkflowChanged", false),
				normalizedReason, requestId));
		return new CitizenValidationResult(
				verification.getId(), observationId, decision, clock.instant(), false);
	}

	private RoadSegment roadSegment(CreateCitizenObservationCommand command) {
		List<RoadSegment> candidates = roadSegmentRepository.findActiveWithinRadius(
				command.longitude(), command.latitude(), ROAD_MATCH_RADIUS_METERS);
		if (candidates.isEmpty()) {
			throw new DomainValidationException(
					"No active road segment was found within 150 metres of the selected location.");
		}
		if (command.roadSegmentId() == null) {
			return candidates.getFirst();
		}
		return candidates.stream()
				.filter(candidate -> candidate.getId().equals(command.roadSegmentId()))
				.findFirst()
				.orElseThrow(() -> new DomainValidationException(
						"The selected road segment is not near the observation location."));
	}

	private Point point(double latitude, double longitude) {
		validateCoordinates(latitude, longitude);
		Point point = WGS84.createPoint(new Coordinate(longitude, latitude));
		point.setSRID(4326);
		return point;
	}

	private void validateCoordinates(double latitude, double longitude) {
		if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
			throw new DomainValidationException("Latitude must be between -90 and 90.");
		}
		if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
			throw new DomainValidationException("Longitude must be between -180 and 180.");
		}
	}

	private CitizenObservation observation(UUID observationId) {
		return observationRepository.findById(observationId)
				.orElseThrow(() -> new NoSuchElementException(
						"Citizen observation not found: " + observationId));
	}

	private User user(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchElementException("Citizen not found: " + userId));
	}

	private String caseNumber() {
		return "CASE-CIT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
	}

	private Map<String, Object> state(CitizenObservation observation) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("caseId", observation.getCivicCase().getId().toString());
		state.put("trackingId", observation.getCivicCase().getCaseNumber());
		state.put("status", observation.getStatus().name());
		state.put("category", observation.getCategory());
		state.put("roadSegmentId", observation.getRoadSegment().getId().toString());
		return state;
	}
}
