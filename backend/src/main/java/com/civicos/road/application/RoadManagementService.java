package com.civicos.road.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.application.DomainMutationResult;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.road.domain.Road;
import com.civicos.road.domain.RoadSegment;
import com.civicos.road.repository.RoadRepository;
import com.civicos.road.repository.RoadSegmentRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class RoadManagementService {

	private final RoadRepository roadRepository;
	private final RoadSegmentRepository roadSegmentRepository;
	private final InterventionRepository interventionRepository;
	private final AuditEventRepository auditEventRepository;
	private final UserRepository userRepository;
	private final ScopedAuthorizationService authorizationService;
	private final Clock clock;

	public RoadManagementService(
			RoadRepository roadRepository,
			RoadSegmentRepository roadSegmentRepository,
			InterventionRepository interventionRepository,
			AuditEventRepository auditEventRepository,
			UserRepository userRepository,
			ScopedAuthorizationService authorizationService,
			Clock clock) {
		this.roadRepository = roadRepository;
		this.roadSegmentRepository = roadSegmentRepository;
		this.interventionRepository = interventionRepository;
		this.auditEventRepository = auditEventRepository;
		this.userRepository = userRepository;
		this.authorizationService = authorizationService;
		this.clock = clock;
	}

	@Transactional
	public DomainMutationResult createRoad(CreateRoadCommand command, String reason, String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.ROAD_CREATE, null, SystemRole.ADMIN);
		if (command.externalReference() != null
				&& !command.externalReference().isBlank()
				&& roadRepository.existsByExternalReference(command.externalReference().strip())) {
			throw new DomainConflictException("Road external reference already exists.");
		}

		Road road = Road.create(
				command.externalReference(), command.name(), command.classification(), command.geometry());
		roadRepository.saveAndFlush(road);
		Instant occurredAt = clock.instant();
		audit(principal, "ROAD_CREATED", "ROAD", road.getId(), null, roadState(road), reason, requestId);
		return result("ROAD", road.getId(), "CREATE", road.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult updateRoad(
			UUID roadId,
			UpdateRoadCommand command,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.ROAD_UPDATE, null, SystemRole.ADMIN);
		Road road = road(roadId);
		assertVersion("Road", command.expectedVersion(), road.getVersion());
		if (command.geometry() == null || !road.getGeometry().equalsExact(command.geometry())) {
			authorizationService.authorize(PermissionCode.ROAD_GEOMETRY_UPDATE, null, SystemRole.ADMIN);
		}
		Map<String, Object> before = roadState(road);
		road.update(command.name(), command.classification(), command.geometry());
		roadRepository.saveAndFlush(road);
		Instant occurredAt = clock.instant();
		audit(principal, "ROAD_UPDATED", "ROAD", road.getId(), before, roadState(road), reason, requestId);
		return result("ROAD", road.getId(), "UPDATE", road.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult deactivateRoad(
			UUID roadId,
			long expectedVersion,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.ROAD_UPDATE, null, SystemRole.ADMIN);
		Road road = road(roadId);
		assertVersion("Road", expectedVersion, road.getVersion());
		if (roadSegmentRepository.existsByRoadIdAndActiveTrue(roadId)) {
			throw new DomainConflictException("A road with active segments cannot be deactivated.");
		}
		Map<String, Object> before = roadState(road);
		road.deactivate();
		roadRepository.saveAndFlush(road);
		Instant occurredAt = clock.instant();
		audit(principal, "ROAD_DEACTIVATED", "ROAD", road.getId(), before, roadState(road), reason, requestId);
		return result("ROAD", road.getId(), "DEACTIVATE", road.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult createRoadSegment(
			CreateRoadSegmentCommand command,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.ROAD_CREATE, null, SystemRole.ADMIN);
		Road road = road(command.roadId());
		if (!road.isActive()) {
			throw new DomainConflictException("A segment cannot be added to an inactive road.");
		}
		if (roadSegmentRepository.existsByExternalReference(command.externalReference())) {
			throw new DomainConflictException("Road segment external reference already exists.");
		}
		RoadSegment segment = RoadSegment.create(
				road,
				command.externalReference(),
				command.name(),
				command.classification(),
				command.surfaceType(),
				command.length(),
				command.geometry());
		roadSegmentRepository.saveAndFlush(segment);
		Instant occurredAt = clock.instant();
		audit(principal, "ROAD_SEGMENT_CREATED", "ROAD_SEGMENT", segment.getId(),
				null, segmentState(segment), reason, requestId);
		return result("ROAD_SEGMENT", segment.getId(), "CREATE", segment.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult updateRoadSegment(
			UUID segmentId,
			UpdateRoadSegmentCommand command,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.ROAD_SEGMENT_UPDATE, null, SystemRole.ADMIN);
		RoadSegment segment = segment(segmentId);
		assertVersion("Road segment", command.expectedVersion(), segment.getVersion());
		if (command.geometry() == null || !segment.getGeometry().equalsExact(command.geometry())) {
			authorizationService.authorize(PermissionCode.ROAD_GEOMETRY_UPDATE, null, SystemRole.ADMIN);
		}
		Map<String, Object> before = segmentState(segment);
		segment.update(
				command.name(), command.classification(), command.surfaceType(),
				command.length(), command.geometry());
		roadSegmentRepository.saveAndFlush(segment);
		Instant occurredAt = clock.instant();
		audit(principal, "ROAD_SEGMENT_UPDATED", "ROAD_SEGMENT", segment.getId(),
				before, segmentState(segment), reason, requestId);
		return result("ROAD_SEGMENT", segment.getId(), "UPDATE", segment.getVersion(), occurredAt);
	}

	@Transactional
	public DomainMutationResult deactivateRoadSegment(
			UUID segmentId,
			long expectedVersion,
			String reason,
			String requestId) {
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.ROAD_SEGMENT_UPDATE, null, SystemRole.ADMIN);
		RoadSegment segment = segment(segmentId);
		assertVersion("Road segment", expectedVersion, segment.getVersion());
		if (interventionRepository.existsByRoadSegmentIdAndStatusNot(segmentId, Intervention.Status.CLOSED)) {
			throw new DomainConflictException(
					"A road segment with non-closed interventions cannot be deactivated.");
		}
		Map<String, Object> before = segmentState(segment);
		segment.deactivate();
		roadSegmentRepository.saveAndFlush(segment);
		Instant occurredAt = clock.instant();
		audit(principal, "ROAD_SEGMENT_DEACTIVATED", "ROAD_SEGMENT", segment.getId(),
				before, segmentState(segment), reason, requestId);
		return result("ROAD_SEGMENT", segment.getId(), "DEACTIVATE", segment.getVersion(), occurredAt);
	}

	private Road road(UUID id) {
		return roadRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Road not found: " + id));
	}

	private RoadSegment segment(UUID id) {
		return roadSegmentRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Road segment not found: " + id));
	}

	private void assertVersion(String entityType, long expected, long actual) {
		if (expected != actual) {
			throw new StaleEntityVersionException(entityType, expected, actual);
		}
	}

	private void audit(
			CivicPrincipal principal,
			String action,
			String entityType,
			UUID entityId,
			Map<String, Object> before,
			Map<String, Object> after,
			String reason,
			String requestId) {
		User actor = userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + principal.userId()));
		auditEventRepository.save(AuditEvent.domainMutation(
				actor, action, entityType, entityId, before, after, reason, requestId));
	}

	private Map<String, Object> roadState(Road road) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("externalReference", road.getExternalReference() == null ? "" : road.getExternalReference());
		state.put("name", road.getName());
		state.put("classification", road.getClassification().name());
		state.put("geometry", road.getGeometry().toText());
		state.put("active", road.isActive());
		state.put("version", road.getVersion());
		return state;
	}

	private Map<String, Object> segmentState(RoadSegment segment) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("roadId", segment.getRoad().getId().toString());
		state.put("externalReference", segment.getExternalReference());
		state.put("name", segment.getName());
		state.put("classification", segment.getClassification().name());
		state.put("surfaceType", segment.getSurfaceType().name());
		state.put("lengthMeters", segment.getLength());
		state.put("geometry", segment.getGeometry().toText());
		state.put("active", segment.isActive());
		state.put("version", segment.getVersion());
		return state;
	}

	private DomainMutationResult result(
			String entityType, UUID entityId, String action, long version, Instant occurredAt) {
		return new DomainMutationResult(entityType, entityId, action, version, occurredAt);
	}
}
