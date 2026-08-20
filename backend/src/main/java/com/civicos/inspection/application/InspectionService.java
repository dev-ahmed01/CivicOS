package com.civicos.inspection.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

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
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.inspection.domain.Inspection;
import com.civicos.inspection.repository.InspectionRepository;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;
import com.civicos.workflow.application.SeparationOfDutiesException;

@Service
public class InspectionService {

	private static final List<Inspection.Status> OPEN_STATUSES = List.of(
			Inspection.Status.SCHEDULED, Inspection.Status.IN_PROGRESS);

	private final InspectionRepository inspectionRepository;
	private final InterventionRepository interventionRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService scopedAuthorizationService;
	private final AuthorizationService authorizationService;
	private final Clock clock;

	public InspectionService(
			InspectionRepository inspectionRepository,
			InterventionRepository interventionRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService scopedAuthorizationService,
			AuthorizationService authorizationService,
			Clock clock) {
		this.inspectionRepository = inspectionRepository;
		this.interventionRepository = interventionRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.scopedAuthorizationService = scopedAuthorizationService;
		this.authorizationService = authorizationService;
		this.clock = clock;
	}

	@Transactional
	public InspectionResult schedule(UUID interventionId, String reason, String requestId) {
		Intervention intervention = intervention(interventionId);
		CivicPrincipal principal = scopedAuthorizationService.authorize(
				PermissionCode.INSPECTION_CREATE,
				intervention.getAgency().getId(),
				SystemRole.INSPECTOR);
		return schedule(intervention, actor(principal), reason, requestId, "INSPECTION_SCHEDULED");
	}

	@Transactional
	public InspectionResult requestReinspection(UUID interventionId, String reason, String requestId) {
		Intervention intervention = intervention(interventionId);
		CivicPrincipal principal = scopedAuthorizationService.authorize(
				PermissionCode.REINSPECTION_REQUEST,
				intervention.getAgency().getId(),
				SystemRole.INSPECTOR);
		Inspection previous = inspectionRepository
				.findFirstByInterventionIdAndStatusOrderByCreatedAtDesc(
						interventionId, Inspection.Status.COMPLETED)
				.orElseThrow(() -> new DomainConflictException(
						"A completed inspection is required before reinspection."));
		if (previous.getResult() == Inspection.Result.PASSED) {
			throw new DomainConflictException("A passed inspection does not require reinspection.");
		}
		return schedule(intervention, actor(principal), reason, requestId, "REINSPECTION_REQUESTED");
	}

	@Transactional
	public InspectionResult start(UUID inspectionId, long expectedVersion, String requestId) {
		Inspection inspection = inspectionForUpdate(inspectionId);
		CivicPrincipal principal = assignedInspector(inspection, PermissionCode.INSPECTION_CREATE);
		assertVersion(inspection, expectedVersion);
		Map<String, Object> before = state(inspection);
		Instant occurredAt = clock.instant();
		inspection.start(occurredAt);
		inspectionRepository.saveAndFlush(inspection);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "INSPECTION_STARTED", "INSPECTION", inspection.getId(),
				before, state(inspection), null, requestId));
		return result(inspection, occurredAt);
	}

	@Transactional
	public InspectionResult complete(
			UUID inspectionId, CompleteInspectionCommand command, String requestId) {
		if (command == null) {
			throw new com.civicos.common.domain.DomainValidationException(
					"Inspection completion command is required.");
		}
		Inspection inspection = inspectionForUpdate(inspectionId);
		CivicPrincipal principal = assignedInspector(inspection, PermissionCode.INSPECTION_COMPLETE);
		assertVersion(inspection, command.expectedVersion());
		Map<String, Object> before = state(inspection);
		Instant occurredAt = clock.instant();
		inspection.complete(command.result(), command.notes(), occurredAt);
		inspectionRepository.saveAndFlush(inspection);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "INSPECTION_COMPLETED", "INSPECTION", inspection.getId(),
				before, state(inspection), command.notes(), requestId));
		return result(inspection, occurredAt);
	}

	private InspectionResult schedule(
			Intervention intervention,
			User inspector,
			String reason,
			String requestId,
			String auditAction) {
		if (intervention.getStatus() != Intervention.Status.COMPLETED_PENDING_VERIFICATION) {
			throw new DomainConflictException(
					"Inspection requires an intervention pending verification.");
		}
		if (intervention.getCreatedBy().getId().equals(inspector.getId())) {
			throw new SeparationOfDutiesException();
		}
		if (inspectionRepository.existsByInterventionIdAndStatusIn(intervention.getId(), OPEN_STATUSES)) {
			throw new DomainConflictException("An open inspection already exists for the intervention.");
		}
		Inspection inspection = Inspection.schedule(intervention, inspector);
		inspectionRepository.saveAndFlush(inspection);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				inspector, auditAction, "INSPECTION", inspection.getId(),
				null, state(inspection), reason, requestId));
		return result(inspection, occurredAt);
	}

	private CivicPrincipal assignedInspector(Inspection inspection, PermissionCode permission) {
		authorizationService.authorize(permission);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		boolean inspector = principal.roles().contains(SystemRole.INSPECTOR.name());
		boolean adminOverride = principal.roles().contains(SystemRole.ADMIN.name())
				&& principal.permissions().contains(PermissionCode.ADMIN_OVERRIDE.name());
		if (!inspector && !adminOverride) {
			throw new AccessDeniedException("Only an inspector can perform this action.");
		}
		if (!inspection.getInspector().getId().equals(principal.userId()) && !adminOverride) {
			throw new AccessDeniedException("Only the assigned inspector can perform this action.");
		}
		return principal;
	}

	private Intervention intervention(UUID id) {
		return interventionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + id));
	}

	private Inspection inspectionForUpdate(UUID id) {
		return inspectionRepository.findForUpdate(id)
				.orElseThrow(() -> new NoSuchElementException("Inspection not found: " + id));
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
	}

	private void assertVersion(Inspection inspection, long expectedVersion) {
		if (inspection.getVersion() != expectedVersion) {
			throw new StaleEntityVersionException(
					"Inspection", expectedVersion, inspection.getVersion());
		}
	}

	private Map<String, Object> state(Inspection inspection) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("interventionId", inspection.getIntervention().getId().toString());
		state.put("inspectorId", inspection.getInspector().getId().toString());
		state.put("status", inspection.getStatus().name());
		if (inspection.getResult() != null) {
			state.put("result", inspection.getResult().name());
		}
		state.put("version", inspection.getVersion());
		return state;
	}

	private InspectionResult result(Inspection inspection, Instant occurredAt) {
		return new InspectionResult(
				inspection.getId(), inspection.getIntervention().getId(), inspection.getInspector().getId(),
				inspection.getStatus(), inspection.getResult(), inspection.getVersion(), occurredAt);
	}
}
