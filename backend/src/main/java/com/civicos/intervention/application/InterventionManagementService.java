package com.civicos.intervention.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.agency.domain.Agency;
import com.civicos.agency.repository.AgencyRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.casefile.repository.CivicCaseRepository;
import com.civicos.common.application.DomainMutationResult;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.validation.GeometryRules;
import com.civicos.dependency.application.DependencyScheduleValidator;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.road.domain.RoadSegment;
import com.civicos.road.repository.RoadSegmentRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class InterventionManagementService {

	private final InterventionRepository interventionRepository;
	private final CivicCaseRepository caseRepository;
	private final AgencyRepository agencyRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService authorizationService;
	private final DependencyScheduleValidator dependencyScheduleValidator;
	private final Clock clock;

	public InterventionManagementService(
			InterventionRepository interventionRepository,
			CivicCaseRepository caseRepository,
			AgencyRepository agencyRepository,
			RoadSegmentRepository roadSegmentRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService authorizationService,
			DependencyScheduleValidator dependencyScheduleValidator,
			Clock clock) {
		this.interventionRepository = interventionRepository;
		this.caseRepository = caseRepository;
		this.agencyRepository = agencyRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
		this.dependencyScheduleValidator = dependencyScheduleValidator;
		this.clock = clock;
	}

	@Transactional
	public DomainMutationResult create(
			CreateInterventionCommand command,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.INTERVENTION_CREATE,
				command.agencyId(),
				SystemRole.AGENCY_OFFICER);
		String number = command.interventionNumber() == null ? null : command.interventionNumber().strip();
		if (number != null && interventionRepository.existsByInterventionNumber(number)) {
			throw new DomainConflictException("Intervention number already exists.");
		}

		CivicCase civicCase = civicCase(command.caseId());
		Agency agency = agency(command.agencyId());
		RoadSegment segment = roadSegment(command.roadSegmentId());
		org.locationtech.jts.geom.Geometry geometry = GeometryRules.validWgs84(
				command.geometry(), "Intervention geometry");
		validateReferences(civicCase, agency, segment, geometry);
		User actor = actor(principal);
		Intervention intervention = Intervention.create(
				number,
				civicCase,
				agency,
				command.type(),
				command.description(),
				segment,
				geometry,
				command.plannedStart(),
				command.plannedEnd(),
				command.priority(),
				actor);
		interventionRepository.saveAndFlush(intervention);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor, "INTERVENTION_CREATED", "INTERVENTION", intervention.getId(),
				null, state(intervention), reason, requestId));
		return new DomainMutationResult(
				"INTERVENTION", intervention.getId(), "CREATE", intervention.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult updateDraft(
			UUID interventionId,
			UpdateDraftInterventionCommand command,
			String reason,
			String requestId) {
		Intervention intervention = intervention(interventionId);
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.INTERVENTION_UPDATE,
				intervention.getAgency().getId(),
				SystemRole.AGENCY_OFFICER);
		authorizationService.authorize(
				PermissionCode.INTERVENTION_UPDATE,
				command.agencyId(),
				SystemRole.AGENCY_OFFICER);
		if (intervention.getVersion() != command.expectedVersion()) {
			throw new StaleEntityVersionException(
					"Intervention", command.expectedVersion(), intervention.getVersion());
		}
		if (intervention.getStatus() != Intervention.Status.DRAFT) {
			throw new DomainConflictException("Only a DRAFT intervention can be edited directly.");
		}
		validateDates(command.plannedStart(), command.plannedEnd());

		CivicCase civicCase = civicCase(command.caseId());
		Agency agency = agency(command.agencyId());
		RoadSegment segment = roadSegment(command.roadSegmentId());
		org.locationtech.jts.geom.Geometry geometry = GeometryRules.validWgs84(
				command.geometry(), "Intervention geometry");
		validateReferences(civicCase, agency, segment, geometry);
		dependencyScheduleValidator.validateProposedSchedule(
				interventionId, command.plannedStart(), command.plannedEnd());
		Map<String, Object> before = state(intervention);
		intervention.updateDraft(
				civicCase,
				agency,
				command.type(),
				command.description(),
				segment,
				geometry,
				command.plannedStart(),
				command.plannedEnd(),
				command.priority());
		interventionRepository.saveAndFlush(intervention);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "INTERVENTION_UPDATED", "INTERVENTION", intervention.getId(),
				before, state(intervention), reason, requestId));
		return new DomainMutationResult(
				"INTERVENTION", intervention.getId(), "UPDATE", intervention.getVersion(), occurredAt);
	}

	private void validateReferences(
			CivicCase civicCase,
			Agency agency,
			RoadSegment segment,
			org.locationtech.jts.geom.Geometry geometry) {
		if (civicCase.getStatus() == CivicCase.Status.CLOSED) {
			throw new DomainConflictException("An intervention cannot be attached to a closed case.");
		}
		if (!agency.isActive()) {
			throw new DomainConflictException("The owning agency is inactive.");
		}
		if (!segment.isActive()) {
			throw new DomainConflictException("The selected road segment is inactive.");
		}
		if (!civicCase.getRoadSegment().getId().equals(segment.getId())) {
			throw new DomainConflictException("The intervention road segment must match its civic case.");
		}
		if (!roadSegmentRepository.activeSegmentIntersects(segment.getId(), geometry)) {
			throw new DomainConflictException("Intervention geometry must intersect the selected road segment.");
		}
	}

	private void validateDates(Instant plannedStart, Instant plannedEnd) {
		if (plannedStart == null || plannedEnd == null || !plannedEnd.isAfter(plannedStart)) {
			throw new DomainValidationException("Planned end must be after planned start.");
		}
	}

	private CivicCase civicCase(UUID id) {
		return caseRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Civic case not found: " + id));
	}

	private Agency agency(UUID id) {
		return agencyRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Agency not found: " + id));
	}

	private RoadSegment roadSegment(UUID id) {
		return roadSegmentRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Road segment not found: " + id));
	}

	private Intervention intervention(UUID id) {
		return interventionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + id));
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + principal.userId()));
	}

	private Map<String, Object> state(Intervention intervention) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("interventionNumber", intervention.getInterventionNumber());
		state.put("caseId", intervention.getCivicCase().getId().toString());
		state.put("agencyId", intervention.getAgency().getId().toString());
		state.put("type", intervention.getType().name());
		state.put("description", intervention.getDescription());
		state.put("roadSegmentId", intervention.getRoadSegment().getId().toString());
		state.put("geometry", intervention.getGeometry().toText());
		state.put("plannedStart", intervention.getPlannedStart().toString());
		state.put("plannedEnd", intervention.getPlannedEnd().toString());
		state.put("status", intervention.getStatus().name());
		state.put("priority", intervention.getPriority().name());
		state.put("version", intervention.getVersion());
		return state;
	}
}
