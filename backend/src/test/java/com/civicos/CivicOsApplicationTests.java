package com.civicos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.road.repository.RoadSegmentRepository;

import jakarta.persistence.EntityManagerFactory;

@Testcontainers
@SpringBootTest
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
			"ai_recommendations");

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
				"AiRun", "AiRecommendation");
		assertThat(applicationContext.getBeansOfType(Repository.class)).hasSize(22);
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
}
