package com.civicos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.Repository;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.approval.application.ApprovalDecisionCommand;
import com.civicos.approval.application.ApprovalService;
import com.civicos.approval.domain.Approval;
import com.civicos.audit.application.AuditQueryService;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.conflict.application.ConflictAnalysisResult;
import com.civicos.conflict.application.ConflictDetectionService;
import com.civicos.conflict.domain.Conflict;
import com.civicos.dependency.application.CreateDependencyCommand;
import com.civicos.dependency.application.DependencyManagementService;
import com.civicos.dependency.domain.Dependency;
import com.civicos.evidence.application.EvidenceReviewCommand;
import com.civicos.evidence.application.EvidenceService;
import com.civicos.evidence.application.EvidenceUploadCommand;
import com.civicos.evidence.domain.Evidence;
import com.civicos.evidence.storage.FileStorageService;
import com.civicos.inspection.application.CompleteInspectionCommand;
import com.civicos.inspection.application.InspectionService;
import com.civicos.inspection.domain.Inspection;
import com.civicos.intervention.application.CreateInterventionCommand;
import com.civicos.intervention.application.InterventionManagementService;
import com.civicos.intervention.application.UpdateDraftInterventionCommand;
import com.civicos.intervention.domain.Intervention;
import com.civicos.notification.application.NotificationOutboxProcessor;
import com.civicos.notification.application.NotificationOutboxService;
import com.civicos.notification.application.NotificationRequest;
import com.civicos.notification.application.NotificationService;
import com.civicos.notification.domain.Notification;
import com.civicos.road.application.CreateRoadCommand;
import com.civicos.road.application.CreateRoadSegmentCommand;
import com.civicos.road.application.RoadManagementService;
import com.civicos.road.application.UpdateRoadCommand;
import com.civicos.road.domain.Road;
import com.civicos.road.domain.RoadSegment;
import com.civicos.road.repository.RoadSegmentRepository;
import com.civicos.sla.application.CreateSlaCommand;
import com.civicos.sla.application.SlaService;
import com.civicos.sla.application.SlaUrgency;
import com.civicos.sla.domain.Sla;
import com.civicos.workflow.application.CaseWorkflowService;
import com.civicos.workflow.application.InterventionWorkflowService;
import com.civicos.workflow.application.SeparationOfDutiesException;
import com.civicos.workflow.application.StaleWorkflowVersionException;
import com.civicos.workflow.application.WorkflowTransitionResult;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;
import com.civicos.verification.application.VerificationCommand;
import com.civicos.verification.application.VerificationService;
import com.civicos.verification.domain.Verification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CivicOsApplicationTests {

	private static final DockerImageName POSTGIS_IMAGE = DockerImageName
			.parse("postgis/postgis:17-3.5")
			.asCompatibleSubstituteFor("postgres");
	private static final Path EVIDENCE_STORAGE_PATH = Path.of(
			System.getProperty("java.io.tmpdir"), "civicos-phase-9-" + UUID.randomUUID());

	private static final List<String> REQUIRED_TABLES = List.of(
			"users",
			"roles",
			"permissions",
			"user_roles",
			"role_permissions",
			"agencies",
			"roads",
			"road_segments",
			"civic_cases",
			"citizen_observations",
			"interventions",
			"dependencies",
			"conflicts",
			"conflict_interventions",
			"coordination_decisions",
			"approvals",
			"sla_instances",
			"escalations",
			"evidence",
			"inspections",
			"verifications",
			"notifications",
			"notification_outbox",
			"audit_events",
			"ai_runs",
			"ai_recommendations",
			"refresh_tokens");

	@Container
	static final PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE)
			.withDatabaseName("civicos_test")
			.withUsername("civicos_test")
			.withPassword("civicos_test");

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgis::getJdbcUrl);
		registry.add("spring.datasource.username", postgis::getUsername);
		registry.add("spring.datasource.password", postgis::getPassword);
		registry.add("civicos.security.jwt-secret", () -> "phase-4-test-secret-that-is-at-least-32-bytes-long");
		registry.add("civicos.sla.monitor-enabled", () -> "false");
		registry.add("civicos.notification.dispatcher-enabled", () -> "false");
		registry.add("civicos.file-storage.path", () -> EVIDENCE_STORAGE_PATH.toString());
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RoadSegmentRepository roadSegmentRepository;

	@Autowired
	private InterventionRepository interventionRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private InterventionWorkflowService interventionWorkflowService;

	@Autowired
	private CaseWorkflowService caseWorkflowService;

	@Autowired
	private RoadManagementService roadManagementService;

	@Autowired
	private InterventionManagementService interventionManagementService;

	@Autowired
	private DependencyManagementService dependencyManagementService;

	@Autowired
	private ConflictDetectionService conflictDetectionService;

	@Autowired
	private ApprovalService approvalService;

	@Autowired
	private SlaService slaService;

	@Autowired
	private EvidenceService evidenceService;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private InspectionService inspectionService;

	@Autowired
	private VerificationService verificationService;

	@Autowired
	private NotificationOutboxService notificationOutboxService;

	@Autowired
	private NotificationOutboxProcessor notificationOutboxProcessor;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private AuditQueryService auditQueryService;

	@Test
	void contextLoadsWithFlywayManagedSchema() {
		for (String table : REQUIRED_TABLES) {
			Boolean exists = jdbcTemplate.queryForObject(
					"select to_regclass(?) is not null",
					Boolean.class,
					"public." + table);

			assertThat(exists).as("table %s", table).isTrue();
		}
	}

	@Test
	void postgisPerformsAuthoritativeSpatialOperations() {
		String postgisVersion = jdbcTemplate.queryForObject(
				"select PostGIS_Version()",
				String.class);
		Boolean intersects = jdbcTemplate.queryForObject(
				"""
				select ST_Intersects(
				    ST_GeomFromText('LINESTRING(77.60 12.90, 77.61 12.91)', 4326),
				    ST_GeomFromText('LINESTRING(77.60 12.91, 77.61 12.90)', 4326)
				)
				""",
				Boolean.class);

		assertThat(postgisVersion).isNotBlank();
		assertThat(intersects).isTrue();
	}

	@Test
	void auditEventsAreAppendOnly() {
		UUID auditId = jdbcTemplate.queryForObject(
				"""
				insert into audit_events (action, entity_type, entity_id)
				values ('PHASE_2_TEST', 'DATABASE', ?)
				returning id
				""",
				UUID.class,
				UUID.randomUUID());

		assertThat(auditId).isNotNull();
		assertThatThrownBy(() -> jdbcTemplate.update(
				"update audit_events set action = 'MUTATED' where id = ?",
				auditId))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("audit_events are append-only");
	}

	@Test
	void canonicalDomainEntitiesAndModuleRepositoriesAreRegistered() {
		Set<String> entityNames = entityManagerFactory.getMetamodel().getEntities().stream()
				.map(entityType -> entityType.getJavaType().getSimpleName())
				.collect(java.util.stream.Collectors.toSet());

		assertThat(entityNames).containsExactlyInAnyOrder(
				"Agency", "User", "Role", "Permission", "Road", "RoadSegment",
				"CivicCase", "CitizenObservation", "Intervention", "Dependency",
				"Conflict", "CoordinationDecision", "Approval", "Sla", "Escalation",
				"Evidence", "Inspection", "Verification", "Notification", "NotificationOutboxEvent", "AuditEvent",
				"AiRun", "AiRecommendation", "RefreshToken");
		assertThat(applicationContext.getBeansOfType(Repository.class)).hasSize(24);
	}

	@Test
	void nativeSpatialRepositoriesExecuteInPostgis() {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		LineString geometry = geometryFactory.createLineString(new Coordinate[] {
				new Coordinate(77.60, 12.90),
				new Coordinate(77.61, 12.91)
		});
		Instant plannedStart = Instant.parse("2026-08-20T06:30:00Z");

		assertThat(roadSegmentRepository.findActiveIntersecting(geometry)).isEmpty();
		assertThat(roadSegmentRepository.findActiveWithinRadius(77.60, 12.90, 100)).isEmpty();
		assertThat(interventionRepository.findSpatialTemporalCandidates(
				null, geometry, plannedStart, plannedStart.plusSeconds(3600))).isEmpty();
	}

	@Test
	void protectedEndpointRejectsUnauthenticatedRequestsWithCorrelationId() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").header("X-Request-Id", "phase-4-unauthenticated"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("X-Request-Id", "phase-4-unauthenticated"))
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
				.andExpect(jsonPath("$.requestId").value("phase-4-unauthenticated"));
	}

	@Test
	void loginRefreshAndLogoutEnforceRotatingServerControlledSessions() throws Exception {
		TestUser citizen = createUser("CITIZEN", "ACTIVE", "OBSERVATION_CREATE");
		JsonNode login = login(citizen);
		String accessToken = login.get("accessToken").asText();
		String refreshToken = login.get("refreshToken").asText();

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(citizen.id().toString()))
				.andExpect(jsonPath("$.roles[0]").value("CITIZEN"))
				.andExpect(jsonPath("$.permissions[0]").value("OBSERVATION_CREATE"));

		String refreshBody = objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken));
		String rotatedJson = mockMvc.perform(post("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(refreshBody))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode rotated = objectMapper.readTree(rotatedJson);
		String rotatedRefreshToken = rotated.get("refreshToken").asText();
		assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

		mockMvc.perform(post("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(refreshBody))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		mockMvc.perform(post("/api/v1/auth/logout")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(
							java.util.Map.of("refreshToken", rotatedRefreshToken))))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(
							java.util.Map.of("refreshToken", rotatedRefreshToken))))
				.andExpect(status().isUnauthorized());

		Integer securityAuditCount = jdbcTemplate.queryForObject(
				"select count(*) from audit_events where actor_id = ? and action like 'AUTH_%'",
				Integer.class,
				citizen.id());
		assertThat(securityAuditCount).isEqualTo(3);
	}

	@Test
	void permissionsAreEnforcedAndReResolvedFromTheDatabase() throws Exception {
		TestUser citizen = createUser("CITIZEN", "ACTIVE", "OBSERVATION_CREATE");
		TestUser admin = createUser("ADMIN", "ACTIVE", "USER_VIEW");

		String citizenAccessToken = login(citizen).get("accessToken").asText();
		mockMvc.perform(get("/api/v1/users/{id}", admin.id())
					.header("Authorization", "Bearer " + citizenAccessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		String adminAccessToken = login(admin).get("accessToken").asText();
		mockMvc.perform(get("/api/v1/users/{id}", citizen.id())
					.header("Authorization", "Bearer " + adminAccessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(citizen.id().toString()));

		jdbcTemplate.update(
				"""
				delete from role_permissions
				where role_id = (select id from roles where code = 'ADMIN')
				  and permission_id = (select id from permissions where code = 'USER_VIEW')
				""");

		mockMvc.perform(get("/api/v1/users/{id}", citizen.id())
					.header("Authorization", "Bearer " + adminAccessToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void disabledUsersCannotAuthenticate() throws Exception {
		TestUser disabled = createUser("CITIZEN", "DISABLED", "OBSERVATION_CREATE");
		mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(java.util.Map.of(
							"email", disabled.email(),
							"password", disabled.password()))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void authorizedWorkflowTransitionIsAtomicVersionedAndAudited() {
		WorkflowFixture fixture = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_SUBMIT");
		try {
			WorkflowTransitionResult result = interventionWorkflowService.transition(
					fixture.interventionId(),
					Intervention.WorkflowAction.SUBMIT,
					0,
					"Ready for cross-agency review",
					"c7ac8339-1ab0-47d8-86d7-f6e02d60da01");

			assertThat(result.previousStatus()).isEqualTo("DRAFT");
			assertThat(result.currentStatus()).isEqualTo("SUBMITTED");
			assertThat(result.version()).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select status from interventions where id = ?",
					String.class,
					fixture.interventionId())).isEqualTo("SUBMITTED");
			assertThat(jdbcTemplate.queryForObject(
					"""
					select count(*) from audit_events
					where entity_id = ?
					  and action = 'INTERVENTION_WORKFLOW_TRANSITION'
					  and before_state ->> 'status' = 'DRAFT'
					  and after_state ->> 'status' = 'SUBMITTED'
					""",
					Integer.class,
					fixture.interventionId())).isEqualTo(1);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void caseWorkflowUsesTheSameAuthorizationConcurrencyAndAuditBoundary() {
		WorkflowFixture fixture = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "COORDINATOR", "OBSERVATION_TRIAGE");
		try {
			WorkflowTransitionResult result = caseWorkflowService.transition(
					fixture.caseId(),
					CivicCase.WorkflowAction.BEGIN_REVIEW,
					0,
					"Coordinator accepted the case for review",
					"phase-5-case-review");

			assertThat(result.previousStatus()).isEqualTo("OPEN");
			assertThat(result.currentStatus()).isEqualTo("UNDER_REVIEW");
			assertThat(result.version()).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select status from civic_cases where id = ?",
					String.class,
					fixture.caseId())).isEqualTo("UNDER_REVIEW");
			assertThat(jdbcTemplate.queryForObject(
					"""
					select count(*) from audit_events
					where entity_id = ? and action = 'CIVIC_CASE_WORKFLOW_TRANSITION'
					""",
					Integer.class,
					fixture.caseId())).isEqualTo(1);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void invalidWorkflowTransitionDoesNotMutateOrAudit() {
		WorkflowFixture fixture = createWorkflowFixture("CLOSED", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_SUBMIT");
		try {
			assertThatThrownBy(() -> interventionWorkflowService.transition(
					fixture.interventionId(),
					Intervention.WorkflowAction.SUBMIT,
					0,
					null,
					"phase-5-invalid-transition"))
					.isInstanceOf(WorkflowActionNotAllowedException.class);

			assertThat(jdbcTemplate.queryForObject(
					"select status from interventions where id = ?",
					String.class,
					fixture.interventionId())).isEqualTo("CLOSED");
			assertThat(auditCount(fixture.interventionId())).isZero();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void creatorCannotApproveOwnIntervention() {
		WorkflowFixture fixture = createWorkflowFixture("COORDINATION_REQUIRED", true);
		insertApproval(fixture.interventionId(), fixture.actorId());
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "APPROVAL_APPROVE");
		try {
			assertThatThrownBy(() -> interventionWorkflowService.transition(
					fixture.interventionId(),
					Intervention.WorkflowAction.APPROVE,
					0,
					"Self approval attempt",
					"phase-5-sod"))
					.isInstanceOf(SeparationOfDutiesException.class);

			assertThat(jdbcTemplate.queryForObject(
					"select status from interventions where id = ?",
					String.class,
					fixture.interventionId())).isEqualTo("COORDINATION_REQUIRED");
			assertThat(auditCount(fixture.interventionId())).isZero();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void duplicateApprovalTransitionAllowsOnlyOneAuthoritativeMutation() {
		WorkflowFixture fixture = createWorkflowFixture("COORDINATION_REQUIRED", false);
		UUID secondActorId = createWorkflowUser(fixture.agencyId());
		insertApproval(fixture.interventionId(), fixture.actorId());
		insertApproval(fixture.interventionId(), secondActorId);

		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "APPROVAL_APPROVE");
		interventionWorkflowService.transition(
				fixture.interventionId(),
				Intervention.WorkflowAction.APPROVE,
				0,
				"Authority one approved",
				"phase-5-approval-one");

		authenticateWorkflowActor(
				secondActorId, fixture.agencyId(), "AGENCY_OFFICER", "APPROVAL_APPROVE");
		try {
			assertThatThrownBy(() -> interventionWorkflowService.transition(
					fixture.interventionId(),
					Intervention.WorkflowAction.APPROVE,
					0,
					"Authority two approved stale state",
					"phase-5-approval-two"))
					.isInstanceOf(StaleWorkflowVersionException.class);

			assertThat(jdbcTemplate.queryForObject(
					"select status from interventions where id = ?",
					String.class,
					fixture.interventionId())).isEqualTo("APPROVED");
			assertThat(auditCount(fixture.interventionId())).isEqualTo(1);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void roadAndSegmentCommandsPersistValidPostgisGeometryAndAuditEvents() {
		WorkflowFixture actorFixture = createWorkflowFixture("DRAFT", false);
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		LineString segmentGeometry = geometryFactory.createLineString(new Coordinate[] {
				new Coordinate(79.00, 14.00),
				new Coordinate(79.01, 14.01)
		});
		MultiLineString roadGeometry = geometryFactory.createMultiLineString(
				new LineString[] {(LineString) segmentGeometry.copy()});
		roadGeometry.setSRID(4326);

		authenticateWorkflowActor(
				actorFixture.actorId(), actorFixture.agencyId(), "ADMIN", "ROAD_CREATE");
		try {
			var roadResult = roadManagementService.createRoad(
					new CreateRoadCommand(
							"ROAD-PHASE-6-" + UUID.randomUUID(),
							"Phase 6 Road",
							Road.Classification.LOCAL,
							roadGeometry),
					"Authoritative road import",
					"phase-6-road-create");
			var segmentResult = roadManagementService.createRoadSegment(
					new CreateRoadSegmentCommand(
							roadResult.entityId(),
							"SEG-PHASE-6-" + UUID.randomUUID(),
							"Phase 6 Segment",
							RoadSegment.Classification.LOCAL,
							RoadSegment.SurfaceType.BITUMINOUS,
							new BigDecimal("150.00"),
							segmentGeometry),
					"Operational segment creation",
					"phase-6-segment-create");
			authenticateWorkflowActor(
					actorFixture.actorId(), actorFixture.agencyId(), "ADMIN", "ROAD_UPDATE");
			var updatedRoad = roadManagementService.updateRoad(
					roadResult.entityId(),
					new UpdateRoadCommand(
							"Phase 6 Road Updated", Road.Classification.LOCAL, roadGeometry, 0),
					"Corrected authoritative road name",
					"phase-6-road-update");

			assertThat(jdbcTemplate.queryForObject(
					"select ST_IsValid(geometry) from road_segments where id = ?",
					Boolean.class,
					segmentResult.entityId())).isTrue();
			assertThat(updatedRoad.version()).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from audit_events where entity_id in (?, ?)",
					Integer.class,
					roadResult.entityId(), segmentResult.entityId())).isEqualTo(3);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void interventionCreationRequiresAgencyScopeCaseSegmentAndSpatialIntersection() {
		WorkflowFixture fixture = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_CREATE");
		try {
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			LineString validGeometry = geometryFactory.createLineString(new Coordinate[] {
					new Coordinate(78.00, 13.50),
					new Coordinate(78.01, 13.51)
			});
			var result = interventionManagementService.create(
					new CreateInterventionCommand(
							"INT-PHASE-6-" + UUID.randomUUID(),
							fixture.caseId(),
							fixture.agencyId(),
							Intervention.Type.UTILITY_EXCAVATION,
							"Validated utility excavation",
							fixture.roadSegmentId(),
							validGeometry,
							Instant.parse("2026-09-01T08:00:00Z"),
							Instant.parse("2026-09-02T08:00:00Z"),
							Intervention.Priority.HIGH),
					"Agency work plan",
					"phase-6-intervention-create");

			assertThat(jdbcTemplate.queryForObject(
					"select status from interventions where id = ?",
					String.class,
					result.entityId())).isEqualTo("DRAFT");
			assertThat(auditCount(result.entityId())).isEqualTo(1);
			authenticateWorkflowActor(
					fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_UPDATE");
			var updated = interventionManagementService.updateDraft(
					result.entityId(),
					new UpdateDraftInterventionCommand(
							fixture.caseId(), fixture.agencyId(), Intervention.Type.UTILITY_EXCAVATION,
							"Validated utility excavation with updated schedule",
							fixture.roadSegmentId(), validGeometry,
							Instant.parse("2026-09-03T08:00:00Z"),
							Instant.parse("2026-09-04T08:00:00Z"),
							Intervention.Priority.HIGH, 0),
					"Agency schedule correction",
					"phase-6-intervention-update");
			assertThat(updated.version()).isEqualTo(1);
			assertThat(auditCount(result.entityId())).isEqualTo(2);

			LineString outsideSegment = geometryFactory.createLineString(new Coordinate[] {
					new Coordinate(81.00, 18.00),
					new Coordinate(81.01, 18.01)
			});
			authenticateWorkflowActor(
					fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_CREATE");
			assertThatThrownBy(() -> interventionManagementService.create(
					new CreateInterventionCommand(
							"INT-PHASE-6-OUTSIDE-" + UUID.randomUUID(),
							fixture.caseId(), fixture.agencyId(), Intervention.Type.ROADWORK,
							"Outside segment", fixture.roadSegmentId(), outsideSegment,
							Instant.parse("2026-09-01T08:00:00Z"),
							Instant.parse("2026-09-02T08:00:00Z"),
							Intervention.Priority.NORMAL),
					null,
					"phase-6-invalid-geometry"))
					.isInstanceOf(DomainConflictException.class)
					.hasMessageContaining("intersect");
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void requiredDependenciesAreBlockedByInvalidSchedulesAndRejectCycles() {
		WorkflowFixture first = createWorkflowFixture("DRAFT", false);
		WorkflowFixture second = createWorkflowFixture("DRAFT", false);
		WorkflowFixture third = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				first.actorId(), first.agencyId(), "COORDINATOR", "COORDINATION_UPDATE");
		try {
			var firstDependency = dependencyManagementService.create(
					new CreateDependencyCommand(
							first.interventionId(), second.interventionId(),
							Dependency.Type.MUST_COMPLETE_BEFORE, true,
							"First intervention must complete before second"),
					"phase-6-dependency-one");
			dependencyManagementService.create(
					new CreateDependencyCommand(
							second.interventionId(), third.interventionId(),
							Dependency.Type.MUST_VERIFY_BEFORE, true,
							"Second intervention must be verified before third"),
					"phase-6-dependency-two");

			assertThat(jdbcTemplate.queryForObject(
					"select status from dependencies where id = ?",
					String.class,
					firstDependency.entityId())).isEqualTo("BLOCKED");
			assertThatThrownBy(() -> dependencyManagementService.create(
					new CreateDependencyCommand(
							third.interventionId(), first.interventionId(),
							Dependency.Type.RESTORATION_DEPENDS_ON, true,
							"This edge would close the cycle"),
					"phase-6-dependency-cycle"))
					.isInstanceOf(DomainConflictException.class)
					.hasMessageContaining("cycle");
			assertThat(jdbcTemplate.queryForObject(
					"""
					select count(*) from dependencies
					where required = true
					  and source_intervention_id in (?, ?, ?)
					""",
					Integer.class,
					first.interventionId(), second.interventionId(), third.interventionId()))
					.isEqualTo(2);
			var cancelled = dependencyManagementService.cancel(
					firstDependency.entityId(),
					0,
					"Coordination plan superseded",
					"phase-6-dependency-cancel");
			assertThat(cancelled.version()).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select status from dependencies where id = ?",
					String.class,
					firstDependency.entityId())).isEqualTo("CANCELLED");
			assertThat(auditCount(firstDependency.entityId())).isEqualTo(2);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void deterministicConflictAnalysisFindsCanonicalRisksAndIsIdempotent() {
		ConflictScenario scenario = createConflictScenario();
		authenticateWorkflowActor(
				scenario.actorId(), scenario.actorAgencyId(), "COORDINATOR", "CONFLICT_ANALYSE");
		try {
			ConflictAnalysisResult first = conflictDetectionService.analyse(
					scenario.targetId(), "phase-7-primary-analysis-one");
			Set<Conflict.Type> types = first.conflicts().stream()
					.map(ConflictAnalysisResult.DetectedConflict::type)
					.collect(java.util.stream.Collectors.toSet());

			assertThat(first.outcome()).isEqualTo(ConflictAnalysisResult.Outcome.CONFLICTS_DETECTED);
			assertThat(types).contains(
					Conflict.Type.SAME_ROAD_OVERLAP,
					Conflict.Type.SPATIAL_OVERLAP,
					Conflict.Type.TEMPORAL_OVERLAP,
					Conflict.Type.UNSAFE_SEQUENCING,
					Conflict.Type.RESTORATION_BEFORE_EXCAVATION_COMPLETION,
					Conflict.Type.REPEAT_DIGGING_RISK,
					Conflict.Type.MULTI_AGENCY_COORDINATION);
			assertThat(first.conflicts()).filteredOn(result ->
					result.type() == Conflict.Type.RESTORATION_BEFORE_EXCAVATION_COMPLETION)
					.allMatch(result -> result.severity() == Conflict.Severity.HIGH);
			assertThat(first.conflicts()).filteredOn(result ->
					result.type() == Conflict.Type.REPEAT_DIGGING_RISK)
					.allMatch(result -> result.severity() == Conflict.Severity.MEDIUM);
			assertThat(first.conflicts()).filteredOn(result ->
					result.type() == Conflict.Type.MULTI_AGENCY_COORDINATION)
					.allMatch(result -> result.severity() == Conflict.Severity.HIGH);

			int persistedAfterFirstRun = conflictCountFor(scenario.targetId());
			UUID humanResolvedConflictId = first.conflicts().getFirst().conflictId();
			jdbcTemplate.update(
					"update conflicts set status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP where id = ?",
					humanResolvedConflictId);
			ConflictAnalysisResult second = conflictDetectionService.analyse(
					scenario.targetId(), "phase-7-primary-analysis-two");

			assertThat(second.conflicts())
					.extracting(ConflictAnalysisResult.DetectedConflict::conflictNumber)
					.containsExactlyInAnyOrderElementsOf(first.conflicts().stream()
							.map(ConflictAnalysisResult.DetectedConflict::conflictNumber)
							.toList());
			assertThat(second.conflicts())
					.anyMatch(result -> result.persistenceAction()
							== ConflictAnalysisResult.PersistenceAction.HUMAN_DECISION_PRESERVED)
					.allMatch(result -> result.persistenceAction()
							== ConflictAnalysisResult.PersistenceAction.UNCHANGED
							|| result.persistenceAction()
							== ConflictAnalysisResult.PersistenceAction.HUMAN_DECISION_PRESERVED);
			assertThat(conflictCountFor(scenario.targetId())).isEqualTo(persistedAfterFirstRun);
			assertThat(jdbcTemplate.queryForObject(
					"select status from conflicts where id = ?",
					String.class,
					humanResolvedConflictId)).isEqualTo("RESOLVED");
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from audit_events where entity_id = ? and action = 'CONFLICT_ANALYSIS_COMPLETED'",
					Integer.class,
					scenario.targetId())).isEqualTo(2);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void isolatedInterventionProducesAuditedNoConflictOutcome() {
		WorkflowFixture isolated = createWorkflowFixtureAt(
				"DRAFT", "LINESTRING(83.00 22.00, 83.01 22.01)",
				"2029-01-01T08:00:00Z", "2029-01-02T08:00:00Z", "ROADWORK");
		authenticateWorkflowActor(
				isolated.actorId(), isolated.agencyId(), "COORDINATOR", "CONFLICT_ANALYSE");
		try {
			ConflictAnalysisResult result = conflictDetectionService.analyse(
					isolated.interventionId(), "phase-7-no-conflict");

			assertThat(result.outcome()).isEqualTo(ConflictAnalysisResult.Outcome.NO_CONFLICT);
			assertThat(result.conflicts()).isEmpty();
			assertThat(conflictCountFor(isolated.interventionId())).isZero();
			assertThat(jdbcTemplate.queryForObject(
					"select after_state ->> 'outcome' from audit_events where entity_id = ? and action = 'CONFLICT_ANALYSIS_COMPLETED'",
					String.class,
					isolated.interventionId())).isEqualTo("NO_CONFLICT");
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void approvalDecisionIsAuthoritativeAuditedAndConcurrencySafe() {
		WorkflowFixture fixture = createWorkflowFixture("COORDINATION_REQUIRED", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "APPROVAL_REQUEST");
		try {
			var requested = approvalService.request(
					fixture.interventionId(), "Coordination package complete", "phase-8-approval-request");
			assertThat(requested.status()).isEqualTo(Approval.Status.PENDING);

			authenticateWorkflowActor(
					fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "APPROVAL_APPROVE");
			var approved = approvalService.decide(
					requested.approvalId(),
					new ApprovalDecisionCommand(Approval.Decision.APPROVE, "Approved", List.of(), 0),
					"phase-8-approval-decide");

			assertThat(approved.status()).isEqualTo(Approval.Status.APPROVED);
			assertThat(approved.version()).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from audit_events where entity_id = ?",
					Integer.class,
					requested.approvalId())).isEqualTo(2);
			assertThatThrownBy(() -> approvalService.decide(
					requested.approvalId(),
					new ApprovalDecisionCommand(Approval.Decision.APPROVE, "Duplicate", List.of(), 0),
					"phase-8-approval-duplicate"))
					.isInstanceOf(StaleEntityVersionException.class);
			assertThat(jdbcTemplate.queryForObject(
					"select status from approvals where id = ?",
					String.class,
					requested.approvalId())).isEqualTo("APPROVED");
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void rejectedApprovalRequiresReasonAndRollsBackInvalidDecision() {
		WorkflowFixture fixture = createWorkflowFixture("COORDINATION_REQUIRED", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "COORDINATOR", "APPROVAL_REQUEST");
		try {
			var requested = approvalService.request(
					fixture.interventionId(), null, "phase-8-rejection-request");
			authenticateWorkflowActor(
					fixture.actorId(), fixture.agencyId(), "COORDINATOR", "APPROVAL_REJECT");

			assertThatThrownBy(() -> approvalService.decide(
					requested.approvalId(),
					new ApprovalDecisionCommand(Approval.Decision.REJECT, null, List.of(), 0),
					"phase-8-invalid-rejection"))
					.isInstanceOf(DomainValidationException.class)
					.hasMessageContaining("reason");
			assertThat(jdbcTemplate.queryForObject(
					"select status from approvals where id = ?",
					String.class,
					requested.approvalId())).isEqualTo("PENDING");
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from audit_events where entity_id = ?",
					Integer.class,
					requested.approvalId())).isEqualTo(1);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void approvalGrantIsBlockedByDependenciesAndHighConflicts() {
		WorkflowFixture target = createWorkflowFixture("COORDINATION_REQUIRED", false);
		WorkflowFixture prerequisite = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				target.actorId(), target.agencyId(), "COORDINATOR", "APPROVAL_REQUEST");
		try {
			var requested = approvalService.request(
					target.interventionId(), null, "phase-8-policy-request");
			UUID dependencyId = UUID.randomUUID();
			jdbcTemplate.update(
					"""
					insert into dependencies (
					    id, source_intervention_id, target_intervention_id,
					    dependency_type, required, status, reason)
					values (?, ?, ?, 'MUST_COMPLETE_BEFORE', true, 'BLOCKED', 'Approval policy test')
					""",
					dependencyId, prerequisite.interventionId(), target.interventionId());
			authenticateWorkflowActor(
					target.actorId(), target.agencyId(), "COORDINATOR", "APPROVAL_APPROVE");
			ApprovalDecisionCommand approve = new ApprovalDecisionCommand(
					Approval.Decision.APPROVE, "Policy checks passed", List.of(), 0);
			assertThatThrownBy(() -> approvalService.decide(
					requested.approvalId(), approve, "phase-8-blocked-dependency"))
					.isInstanceOf(DomainConflictException.class)
					.hasMessageContaining("dependency");

			jdbcTemplate.update("update dependencies set status = 'CANCELLED' where id = ?", dependencyId);
			UUID conflictId = UUID.randomUUID();
			jdbcTemplate.update(
					"""
					insert into conflicts (
					    id, conflict_number, road_segment_id, conflict_type,
					    severity, status, explanation)
					values (?, ?, ?, 'SAME_ROAD_OVERLAP', 'HIGH', 'OPEN', 'Approval policy test')
					""",
					conflictId, "CF-PHASE-8-" + conflictId, target.roadSegmentId());
			jdbcTemplate.update(
					"insert into conflict_interventions (conflict_id, intervention_id) values (?, ?), (?, ?)",
					conflictId, target.interventionId(), conflictId, prerequisite.interventionId());
			assertThatThrownBy(() -> approvalService.decide(
					requested.approvalId(), approve, "phase-8-blocking-conflict"))
					.isInstanceOf(DomainConflictException.class)
					.hasMessageContaining("HIGH");

			assertThat(jdbcTemplate.queryForObject(
					"select status from approvals where id = ?",
					String.class,
					requested.approvalId())).isEqualTo("PENDING");
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void slaUsesBusinessHoursAndBreachesEscalateIdempotently() {
		UUID targetId = UUID.randomUUID();
		CreateSlaCommand command = new CreateSlaCommand(
				"INTERVENTION", targetId, Sla.Type.APPROVAL, SlaUrgency.STANDARD,
				Instant.parse("2026-08-21T10:30:00Z"));

		var created = slaService.createSystem(command, "Approval obligation", "phase-8-sla-create");
		var duplicate = slaService.createSystem(command, "Duplicate trigger", "phase-8-sla-duplicate");
		assertThat(created.slaId()).isEqualTo(duplicate.slaId());
		assertThat(created.deadline()).isEqualTo(Instant.parse("2026-08-24T10:30:00Z"));

		var atRisk = slaService.monitorOne(
				created.slaId(), created.deadline().minusSeconds(3 * 3600L), "phase-8-sla-risk");
		assertThat(atRisk.status()).isEqualTo(Sla.Status.AT_RISK);
		var breached = slaService.monitorOne(
				created.slaId(), created.deadline(), "phase-8-sla-breach");
		assertThat(breached.status()).isEqualTo(Sla.Status.BREACHED);
		slaService.monitorOne(
				created.slaId(), created.deadline().plusSeconds(3600), "phase-8-sla-recheck");

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from escalations where sla_id = ? and level = 1",
				Integer.class,
				created.slaId())).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from audit_events where entity_id = ? and action in ('SLA_AT_RISK', 'SLA_BREACHED')",
				Integer.class,
				created.slaId())).isEqualTo(2);
	}

	@Test
	void slaPauseResumeAndCompletionAreVersionedAndAudited() {
		WorkflowFixture admin = createWorkflowFixture("DRAFT", false);
		var created = slaService.createSystem(
				new CreateSlaCommand(
						"INTERVENTION", admin.interventionId(), Sla.Type.REVIEW,
						SlaUrgency.STANDARD, null),
				"Initial review obligation",
				"phase-8-sla-lifecycle-create");
		authenticateWorkflowActor(
				admin.actorId(), admin.agencyId(), "ADMIN", "SLA_MANAGE");
		try {
			var paused = slaService.pause(
					created.slaId(), 0, "Awaiting external information", "phase-8-sla-pause");
			assertThat(paused.status()).isEqualTo(Sla.Status.PAUSED);
			var resumed = slaService.resume(
					created.slaId(), 1, "Information received", "phase-8-sla-resume");
			assertThat(resumed.status()).isEqualTo(Sla.Status.NORMAL);
			assertThat(resumed.deadline()).isAfterOrEqualTo(created.deadline());
			var completed = slaService.complete(
					created.slaId(), 2, "Review completed", "phase-8-sla-complete");
			assertThat(completed.status()).isEqualTo(Sla.Status.COMPLETED);
			assertThat(completed.version()).isEqualTo(3);
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from audit_events where entity_id = ?",
					Integer.class,
					created.slaId())).isEqualTo(4);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void evidenceUploadValidatesContentPreservesProvenanceAndRequiresIndependentReview() throws Exception {
		WorkflowFixture fixture = createWorkflowFixture("COMPLETED_PENDING_VERIFICATION", false);
		byte[] png = pngEvidence();
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "EVIDENCE_UPLOAD");
		try {
			assertThatThrownBy(() -> evidenceService.upload(
					new EvidenceUploadCommand(
							"INTERVENTION", fixture.interventionId(), Evidence.Type.COMPLETION,
							"spoofed.png", "image/png", Instant.parse("2026-08-19T08:00:00Z"),
							null, null, Map.of()),
					"not-a-png".getBytes(java.nio.charset.StandardCharsets.UTF_8),
					"phase-9-invalid-signature"))
					.isInstanceOf(DomainValidationException.class)
					.hasMessageContaining("declared MIME type");

			var uploaded = evidenceService.upload(
					new EvidenceUploadCommand(
							"intervention", fixture.interventionId(), Evidence.Type.COMPLETION,
							"../../completion.png", "image/png",
							Instant.parse("2026-08-19T08:00:00Z"),
							new BigDecimal("12.971599"), new BigDecimal("77.594566"),
							Map.of("caption", "Restoration completion")),
					png,
					"phase-9-evidence-upload");

			assertThat(uploaded.status()).isEqualTo(Evidence.Status.UPLOADED);
			assertThat(uploaded.checksum()).hasSize(64);
			assertThat(fileStorageService.open(uploaded.fileReference()).readAllBytes()).isEqualTo(png);
			assertThat(jdbcTemplate.queryForObject(
					"select original_filename from evidence where id = ?",
					String.class,
					uploaded.evidenceId())).isEqualTo("completion.png");
			assertThat(jdbcTemplate.queryForObject(
					"select metadata ->> 'provenance' from evidence where id = ?",
					String.class,
					uploaded.evidenceId())).isEqualTo("AGENCY_SUBMITTED");

			var underReview = evidenceService.submitForReview(
					uploaded.evidenceId(), 0, "Ready for independent review",
					"phase-9-evidence-submit");
			assertThat(underReview.status()).isEqualTo(Evidence.Status.UNDER_REVIEW);

			UUID reviewerId = createWorkflowUser(fixture.agencyId());
			authenticateWorkflowActor(
					reviewerId, fixture.agencyId(), "INSPECTOR", "EVIDENCE_ACCEPT");
			var accepted = evidenceService.review(
					uploaded.evidenceId(),
					new EvidenceReviewCommand(Evidence.Status.ACCEPTED, "Evidence is valid", 1),
					"phase-9-evidence-review");
			assertThat(accepted.status()).isEqualTo(Evidence.Status.ACCEPTED);
			assertThat(accepted.version()).isEqualTo(2);
			assertThat(auditCount(uploaded.evidenceId())).isEqualTo(3);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void inspectionsAreAssignedVersionedAndImmutableAfterCompletion() {
		WorkflowFixture fixture = createWorkflowFixture("COMPLETED_PENDING_VERIFICATION", false);
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "INSPECTOR", "INSPECTION_CREATE");
		try {
			var scheduled = inspectionService.schedule(
					fixture.interventionId(), "Final field inspection", "phase-9-inspection-schedule");
			var started = inspectionService.start(
					scheduled.inspectionId(), 0, "phase-9-inspection-start");
			assertThat(started.status()).isEqualTo(Inspection.Status.IN_PROGRESS);

			authenticateWorkflowActor(
					fixture.actorId(), fixture.agencyId(), "INSPECTOR", "INSPECTION_COMPLETE");
			var completed = inspectionService.complete(
					scheduled.inspectionId(),
					new CompleteInspectionCommand(Inspection.Result.PASSED, "Site is compliant", 1),
					"phase-9-inspection-complete");
			assertThat(completed.status()).isEqualTo(Inspection.Status.COMPLETED);
			assertThat(completed.version()).isEqualTo(2);
			assertThatThrownBy(() -> inspectionService.complete(
					scheduled.inspectionId(),
					new CompleteInspectionCommand(Inspection.Result.FAILED, "Mutation attempt", 2),
					"phase-9-inspection-mutate"))
					.isInstanceOf(DomainConflictException.class);
			assertThatThrownBy(() -> jdbcTemplate.update(
					"update inspections set notes = 'tampered' where id = ?",
					scheduled.inspectionId()))
					.isInstanceOf(DataAccessException.class)
					.hasMessageContaining("completed inspections are immutable");
			assertThat(auditCount(scheduled.inspectionId())).isEqualTo(3);
		} finally {
			SecurityContextHolder.clearContext();
		}

		WorkflowFixture selfInspection = createWorkflowFixture("COMPLETED_PENDING_VERIFICATION", true);
		authenticateWorkflowActor(
				selfInspection.actorId(), selfInspection.agencyId(), "INSPECTOR", "INSPECTION_CREATE");
		try {
			assertThatThrownBy(() -> inspectionService.schedule(
					selfInspection.interventionId(), "Self inspection", "phase-9-self-inspection"))
					.isInstanceOf(SeparationOfDutiesException.class);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void authoritativeVerificationRequiresAcceptedEvidenceAndTransitionsWorkflowAtomically() {
		WorkflowFixture fixture = createWorkflowFixture("COMPLETED_PENDING_VERIFICATION", false);
		UUID inspectorId = createWorkflowUser(fixture.agencyId());
		authenticateWorkflowActor(
				inspectorId, fixture.agencyId(), "INSPECTOR", "INSPECTION_CREATE");
		var scheduled = inspectionService.schedule(
				fixture.interventionId(), "Final verification inspection", "phase-9-final-schedule");
		inspectionService.start(scheduled.inspectionId(), 0, "phase-9-final-start");
		authenticateWorkflowActor(
				inspectorId, fixture.agencyId(), "INSPECTOR", "INSPECTION_COMPLETE");
		inspectionService.complete(
				scheduled.inspectionId(),
				new CompleteInspectionCommand(Inspection.Result.PASSED, "All checks passed", 1),
				"phase-9-final-complete");

		authenticateWorkflowActor(
				inspectorId, fixture.agencyId(), "INSPECTOR", "VERIFICATION_PASS");
		try {
			assertThatThrownBy(() -> verificationService.verify(
					fixture.interventionId(),
					new VerificationCommand(
							scheduled.inspectionId(), Verification.Result.PASSED,
							"Official verification passed", 0),
					"phase-9-missing-evidence"))
					.isInstanceOf(DomainConflictException.class)
					.hasMessageContaining("Required accepted evidence");
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from verifications where target_id = ?",
					Integer.class,
					fixture.interventionId())).isZero();
		} finally {
			SecurityContextHolder.clearContext();
		}

		UUID reviewerId = createWorkflowUser(fixture.agencyId());
		uploadAndAcceptEvidence(fixture, fixture.actorId(), reviewerId, Evidence.Type.COMPLETION);
		uploadAndAcceptEvidence(fixture, fixture.actorId(), reviewerId, Evidence.Type.RESTORATION);

		authenticateWorkflowActor(
				inspectorId, fixture.agencyId(), "INSPECTOR", "VERIFICATION_PASS");
		try {
			var verified = verificationService.verify(
					fixture.interventionId(),
					new VerificationCommand(
							scheduled.inspectionId(), Verification.Result.PASSED,
							"Official verification passed", 0),
					"phase-9-verification-pass");
			assertThat(verified.interventionStatus()).isEqualTo("VERIFIED");
			assertThat(verified.interventionVersion()).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select status from interventions where id = ?",
					String.class,
					fixture.interventionId())).isEqualTo("VERIFIED");
			assertThatThrownBy(() -> jdbcTemplate.update(
					"update verifications set reason = 'tampered' where id = ?",
					verified.verificationId()))
					.isInstanceOf(DataAccessException.class)
					.hasMessageContaining("verifications are append-only");
			assertThat(auditCount(verified.verificationId())).isEqualTo(1);
			assertThat(auditCount(fixture.interventionId())).isEqualTo(1);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void notificationOutboxIsIdempotentAndOnlyRecipientsCanReadDeliveredNotifications() {
		WorkflowFixture fixture = createWorkflowFixture("DRAFT", false);
		NotificationRequest request = new NotificationRequest(
				"phase-10-idempotent-" + fixture.interventionId(),
				Notification.Type.TASK_ASSIGNED,
				fixture.actorId(),
				"Coordination task assigned",
				"A coordination task is ready for review.",
				"INTERVENTION",
				fixture.interventionId());

		UUID outboxId = notificationOutboxService.enqueue(request);
		assertThat(notificationOutboxService.enqueue(request)).isEqualTo(outboxId);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from notification_outbox where event_key = ?",
				Integer.class,
				request.eventKey())).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from notifications where recipient_id = ?",
				Integer.class,
				fixture.actorId())).isZero();

		notificationOutboxProcessor.process(outboxId);
		assertThat(jdbcTemplate.queryForObject(
				"select status from notification_outbox where id = ?",
				String.class,
				outboxId)).isEqualTo("DELIVERED");

		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_VIEW");
		UUID notificationId;
		try {
			assertThat(notificationService.unreadCount()).isEqualTo(1);
			var unread = notificationService.listForCurrentUser(true);
			assertThat(unread).hasSize(1);
			notificationId = unread.getFirst().notificationId();
			var read = notificationService.markRead(notificationId);
			assertThat(read.readAt()).isNotNull();
			assertThat(read.version()).isEqualTo(1);
			assertThat(notificationService.unreadCount()).isZero();
		} finally {
			SecurityContextHolder.clearContext();
		}

		WorkflowFixture unrelated = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				unrelated.actorId(), unrelated.agencyId(), "AGENCY_OFFICER", "INTERVENTION_VIEW");
		try {
			assertThatThrownBy(() -> notificationService.markRead(notificationId))
					.isInstanceOf(AccessDeniedException.class);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void workflowQueuesNotificationsAndAuditVisibilityIsResourceScoped() {
		WorkflowFixture fixture = createWorkflowFixture("DRAFT", false);
		assignSystemRole(fixture.actorId(), "AGENCY_OFFICER");
		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "INTERVENTION_SUBMIT");
		try {
			interventionWorkflowService.transition(
					fixture.interventionId(), Intervention.WorkflowAction.SUBMIT, 0,
					"Ready for review", "phase-10-workflow-audit");
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from notification_outbox where target_id = ? and status = 'PENDING'",
					Integer.class,
					fixture.interventionId())).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from notifications where target_id = ?",
					Integer.class,
					fixture.interventionId())).isZero();
		} finally {
			SecurityContextHolder.clearContext();
		}

		authenticateWorkflowActor(
				fixture.actorId(), fixture.agencyId(), "AGENCY_OFFICER", "AUDIT_VIEW");
		UUID eventId;
		try {
			var trail = auditQueryService.entityTrail("intervention", fixture.interventionId());
			assertThat(trail).hasSize(1);
			assertThat(trail.getFirst().metadata())
					.containsEntry("agencyId", fixture.agencyId().toString())
					.containsEntry("correlationId", "phase-10-workflow-audit");
			assertThat(trail.getFirst().metadata().get("actorRoles"))
					.isEqualTo(List.of("AGENCY_OFFICER"));
			eventId = trail.getFirst().eventId();
			assertThat(auditQueryService.byEventId(eventId).entityId())
					.isEqualTo(fixture.interventionId());
		} finally {
			SecurityContextHolder.clearContext();
		}

		WorkflowFixture unrelated = createWorkflowFixture("DRAFT", false);
		authenticateWorkflowActor(
				unrelated.actorId(), unrelated.agencyId(), "AGENCY_OFFICER", "AUDIT_VIEW");
		try {
			assertThatThrownBy(() -> auditQueryService.byEventId(eventId))
					.isInstanceOf(AccessDeniedException.class);
		} finally {
			SecurityContextHolder.clearContext();
		}

		authenticateWorkflowActor(
				unrelated.actorId(), unrelated.agencyId(), "ADMIN", "AUDIT_VIEW");
		try {
			assertThat(auditQueryService.byEventId(eventId).entityId())
					.isEqualTo(fixture.interventionId());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private void uploadAndAcceptEvidence(
			WorkflowFixture fixture,
			UUID uploaderId,
			UUID reviewerId,
			Evidence.Type type) {
		authenticateWorkflowActor(
				uploaderId, fixture.agencyId(), "AGENCY_OFFICER", "EVIDENCE_UPLOAD");
		var uploaded = evidenceService.upload(
				new EvidenceUploadCommand(
						"INTERVENTION", fixture.interventionId(), type,
						type.name().toLowerCase() + ".png", "image/png",
						Instant.parse("2026-08-19T08:00:00Z"), null, null, Map.of()),
				pngEvidence(),
				"phase-9-" + type.name().toLowerCase() + "-upload");
		evidenceService.submitForReview(
				uploaded.evidenceId(), 0, "Required evidence ready",
				"phase-9-" + type.name().toLowerCase() + "-submit");
		authenticateWorkflowActor(
				reviewerId, fixture.agencyId(), "INSPECTOR", "EVIDENCE_ACCEPT");
		evidenceService.review(
				uploaded.evidenceId(),
				new EvidenceReviewCommand(Evidence.Status.ACCEPTED, "Required evidence accepted", 1),
				"phase-9-" + type.name().toLowerCase() + "-accept");
	}

	private byte[] pngEvidence() {
		return new byte[] {
				(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
				0x00, 0x00, 0x00, 0x00
		};
	}

	private JsonNode login(TestUser user) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(java.util.Map.of(
							"email", user.email(),
							"password", user.password()))))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response);
	}

	private TestUser createUser(String roleCode, String status, String... permissions) {
		UUID roleId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into roles (id, code, name, system_role) values (?, ?, ?, true) on conflict (code) do nothing",
				roleId, roleCode, roleCode);
		roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", UUID.class, roleCode);

		for (String permissionCode : permissions) {
			UUID permissionId = UUID.randomUUID();
			jdbcTemplate.update(
					"insert into permissions (id, code) values (?, ?) on conflict (code) do nothing",
					permissionId, permissionCode);
			permissionId = jdbcTemplate.queryForObject(
					"select id from permissions where code = ?", UUID.class, permissionCode);
			jdbcTemplate.update(
					"insert into role_permissions (role_id, permission_id) values (?, ?) on conflict do nothing",
					roleId, permissionId);
		}

		UUID userId = UUID.randomUUID();
		String email = "phase4-" + userId + "@civicos.test";
		String password = "Phase4-Test-Password!";
		jdbcTemplate.update(
				"insert into users (id, full_name, email, password_hash, status) values (?, ?, ?, ?, ?)",
				userId, "Phase 4 Test User", email, passwordEncoder.encode(password), status);
		jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", userId, roleId);
		return new TestUser(userId, email, password);
	}

	private WorkflowFixture createWorkflowFixture(String status, boolean actorIsCreator) {
		return createWorkflowFixtureAt(
				status,
				"LINESTRING(78.00 13.50, 78.01 13.51)",
				"2026-08-20T08:00:00Z",
				"2026-08-21T08:00:00Z",
				"ROADWORK",
				actorIsCreator);
	}

	private WorkflowFixture createWorkflowFixtureAt(
			String status,
			String geometryWkt,
			String plannedStart,
			String plannedEnd,
			String interventionType) {
		return createWorkflowFixtureAt(
				status, geometryWkt, plannedStart, plannedEnd, interventionType, false);
	}

	private WorkflowFixture createWorkflowFixtureAt(
			String status,
			String geometryWkt,
			String plannedStart,
			String plannedEnd,
			String interventionType,
			boolean actorIsCreator) {
		UUID agencyId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into agencies (id, code, name, agency_type) values (?, ?, ?, 'GOVERNMENT')",
				agencyId, "AG-" + agencyId, "Workflow Agency " + agencyId);
		UUID actorId = createWorkflowUser(agencyId);
		UUID creatorId = actorIsCreator ? actorId : createWorkflowUser(agencyId);

		UUID roadId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into roads (id, external_reference, name, classification, geometry)
				values (?, ?, ?, 'LOCAL', ST_Multi(ST_GeomFromText(?, 4326)))
				""",
				roadId, "ROAD-" + roadId, "Workflow Road " + roadId, geometryWkt);
		UUID roadSegmentId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into road_segments (
				    id, road_id, external_reference, name, classification,
				    surface_type, length_meters, geometry)
				values (?, ?, ?, ?, 'LOCAL', 'BITUMINOUS', 100,
				        ST_GeomFromText(?, 4326))
				""",
				roadSegmentId, roadId, "SEG-" + roadSegmentId, "Workflow Segment " + roadSegmentId,
				geometryWkt);
		UUID caseId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into civic_cases (id, case_number, source, road_segment_id)
				values (?, ?, 'AGENCY', ?)
				""",
				caseId, "CASE-" + caseId, roadSegmentId);
		UUID interventionId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into interventions (
				    id, intervention_number, case_id, agency_id, intervention_type,
				    description, road_segment_id, geometry, planned_start, planned_end,
				    status, created_by)
				values (?, ?, ?, ?, ?, 'Phase 5 workflow fixture', ?,
				        ST_GeomFromText(?, 4326), ?::timestamptz, ?::timestamptz, ?, ?)
				""",
				interventionId, "INT-" + interventionId, caseId, agencyId, interventionType,
				roadSegmentId, geometryWkt, plannedStart, plannedEnd, status, creatorId);
		return new WorkflowFixture(interventionId, caseId, roadSegmentId, agencyId, actorId);
	}

	private ConflictScenario createConflictScenario() {
		WorkflowFixture restoration = createWorkflowFixtureAt(
				"DRAFT", "LINESTRING(80.00 16.00, 80.01 16.01)",
				"2027-02-05T08:00:00Z", "2027-02-06T08:00:00Z", "RESTORATION");
		insertScenarioIntervention(restoration, "UTILITY_EXCAVATION",
				"2027-02-01T08:00:00Z", "2027-02-07T08:00:00Z");
		UUID lateExcavation = insertScenarioIntervention(restoration, "WATER",
				"2027-02-08T08:00:00Z", "2027-02-09T08:00:00Z");
		insertScenarioIntervention(restoration, "TELECOM",
				"2027-02-10T08:00:00Z", "2027-02-11T08:00:00Z");
		jdbcTemplate.update(
				"""
				insert into dependencies (
				    id, source_intervention_id, target_intervention_id,
				    dependency_type, required, status, reason)
				values (?, ?, ?, 'RESTORATION_DEPENDS_ON', true, 'BLOCKED',
				        'Restoration is blocked until excavation completes')
				""",
				UUID.randomUUID(), lateExcavation, restoration.interventionId());
		return new ConflictScenario(
				restoration.interventionId(), restoration.actorId(), restoration.agencyId());
	}

	private UUID insertScenarioIntervention(
			WorkflowFixture corridor,
			String interventionType,
			String plannedStart,
			String plannedEnd) {
		UUID agencyId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into agencies (id, code, name, agency_type) values (?, ?, ?, 'GOVERNMENT')",
				agencyId, "AG-" + agencyId, "Conflict Agency " + agencyId);
		UUID creatorId = createWorkflowUser(agencyId);
		UUID caseId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into civic_cases (id, case_number, source, road_segment_id) values (?, ?, 'AGENCY', ?)",
				caseId, "CASE-" + caseId, corridor.roadSegmentId());
		UUID interventionId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into interventions (
				    id, intervention_number, case_id, agency_id, intervention_type,
				    description, road_segment_id, geometry, planned_start, planned_end,
				    status, created_by)
				values (?, ?, ?, ?, ?, 'Phase 7 synthetic conflict scenario', ?,
				        ST_GeomFromText('LINESTRING(80.00 16.00, 80.01 16.01)', 4326),
				        ?::timestamptz, ?::timestamptz, 'DRAFT', ?)
				""",
				interventionId, "INT-" + interventionId, caseId, agencyId, interventionType,
				corridor.roadSegmentId(), plannedStart, plannedEnd, creatorId);
		return interventionId;
	}

	private int conflictCountFor(UUID interventionId) {
		return jdbcTemplate.queryForObject(
				"select count(*) from conflict_interventions where intervention_id = ?",
				Integer.class,
				interventionId);
	}

	private UUID createWorkflowUser(UUID agencyId) {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into users (id, agency_id, full_name, email, status)
				values (?, ?, 'Phase 5 Workflow Actor', ?, 'ACTIVE')
				""",
				userId, agencyId, "phase5-" + userId + "@civicos.test");
		return userId;
	}

	private void assignSystemRole(UUID userId, String roleCode) {
		UUID roleId = UUID.randomUUID();
		jdbcTemplate.update(
				"insert into roles (id, code, name, system_role) values (?, ?, ?, true) on conflict (code) do nothing",
				roleId, roleCode, roleCode);
		roleId = jdbcTemplate.queryForObject(
				"select id from roles where code = ?", UUID.class, roleCode);
		jdbcTemplate.update(
				"insert into user_roles (user_id, role_id) values (?, ?) on conflict do nothing",
				userId, roleId);
	}

	private void insertApproval(UUID interventionId, UUID actorId) {
		jdbcTemplate.update(
				"""
				insert into approvals (
				    id, intervention_id, actor_id, status, decision, reason, decided_at)
				values (?, ?, ?, 'APPROVED', 'APPROVE', 'Approved for Phase 5 test', CURRENT_TIMESTAMP)
				""",
				UUID.randomUUID(), interventionId, actorId);
	}

	private void authenticateWorkflowActor(
			UUID userId,
			UUID agencyId,
			String role,
			String permission) {
		CivicPrincipal principal = new CivicPrincipal(
				userId,
				agencyId,
				"Phase 5 Workflow Actor",
				"phase5-" + userId + "@civicos.test",
				"",
				true,
				Set.of(role),
				Set.of(permission),
				List.of());
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
	}

	private int auditCount(UUID interventionId) {
		return jdbcTemplate.queryForObject(
				"select count(*) from audit_events where entity_id = ?",
				Integer.class,
				interventionId);
	}

	private record TestUser(UUID id, String email, String password) {
	}

	private record WorkflowFixture(
			UUID interventionId,
			UUID caseId,
			UUID roadSegmentId,
			UUID agencyId,
			UUID actorId) {
	}

	private record ConflictScenario(UUID targetId, UUID actorId, UUID actorAgencyId) {
	}
}
