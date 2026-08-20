package com.civicos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.domain.CivicCase;
import com.civicos.intervention.domain.Intervention;
import com.civicos.road.repository.RoadSegmentRepository;
import com.civicos.workflow.application.CaseWorkflowService;
import com.civicos.workflow.application.InterventionWorkflowService;
import com.civicos.workflow.application.SeparationOfDutiesException;
import com.civicos.workflow.application.StaleWorkflowVersionException;
import com.civicos.workflow.application.WorkflowTransitionResult;
import com.civicos.workflow.domain.WorkflowActionNotAllowedException;
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
				"Evidence", "Inspection", "Verification", "Notification", "AuditEvent",
				"AiRun", "AiRecommendation", "RefreshToken");
		assertThat(applicationContext.getBeansOfType(Repository.class)).hasSize(23);
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
				values (?, ?, ?, 'LOCAL', ST_GeomFromText('MULTILINESTRING((78.00 13.50, 78.01 13.51))', 4326))
				""",
				roadId, "ROAD-" + roadId, "Workflow Road " + roadId);
		UUID roadSegmentId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				insert into road_segments (
				    id, road_id, external_reference, name, classification,
				    surface_type, length_meters, geometry)
				values (?, ?, ?, ?, 'LOCAL', 'BITUMINOUS', 100,
				        ST_GeomFromText('LINESTRING(78.00 13.50, 78.01 13.51)', 4326))
				""",
				roadSegmentId, roadId, "SEG-" + roadSegmentId, "Workflow Segment " + roadSegmentId);
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
				values (?, ?, ?, ?, 'ROADWORK', 'Phase 5 workflow fixture', ?,
				        ST_GeomFromText('LINESTRING(78.00 13.50, 78.01 13.51)', 4326),
				        '2026-08-20T08:00:00Z', '2026-08-21T08:00:00Z', ?, ?)
				""",
				interventionId, "INT-" + interventionId, caseId, agencyId,
				roadSegmentId, status, creatorId);
		return new WorkflowFixture(interventionId, caseId, agencyId, actorId);
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

	private record WorkflowFixture(UUID interventionId, UUID caseId, UUID agencyId, UUID actorId) {
	}
}
