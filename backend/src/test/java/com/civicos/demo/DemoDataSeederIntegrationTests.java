package com.civicos.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class DemoDataSeederIntegrationTests {

	private static final String DEMO_PASSWORD = "demo-test-password";
	private static final DockerImageName POSTGIS_IMAGE = DockerImageName
			.parse("postgis/postgis:17-3.5")
			.asCompatibleSubstituteFor("postgres");

	@Container
	static final PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE)
			.withDatabaseName("civicos_demo_seed_test")
			.withUsername("civicos_demo_seed_test")
			.withPassword("civicos_demo_seed_test");

	@Test
	void loadsTheControlledDatasetIdempotentlyWithUsableHashedCredentials() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				postgis.getJdbcUrl(), postgis.getUsername(), postgis.getPassword());
		Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
		DemoDataSeeder seeder = new DemoDataSeeder(dataSource, jdbcTemplate, passwordEncoder, DEMO_PASSWORD);

		seeder.run(new DefaultApplicationArguments(new String[0]));
		seeder.run(new DefaultApplicationArguments(new String[0]));

		assertThat(count(jdbcTemplate, "agencies")).isEqualTo(7);
		assertThat(count(jdbcTemplate, "users")).isEqualTo(8);
		assertThat(count(jdbcTemplate, "roads")).isEqualTo(3);
		assertThat(count(jdbcTemplate, "road_segments")).isEqualTo(3);
		assertThat(count(jdbcTemplate, "civic_cases")).isEqualTo(3);
		assertThat(count(jdbcTemplate, "citizen_observations")).isEqualTo(5);
		assertThat(count(jdbcTemplate, "interventions")).isEqualTo(7);
		assertThat(count(jdbcTemplate, "conflicts")).isEqualTo(3);
		assertThat(count(jdbcTemplate, "dependencies")).isEqualTo(5);
		assertThat(count(jdbcTemplate, "approvals")).isEqualTo(6);
		assertThat(count(jdbcTemplate, "sla_instances")).isEqualTo(6);
		assertThat(count(jdbcTemplate, "escalations")).isEqualTo(1);
		assertThat(count(jdbcTemplate, "evidence")).isEqualTo(12);
		assertThat(count(jdbcTemplate, "inspections")).isEqualTo(2);
		assertThat(count(jdbcTemplate, "verifications")).isEqualTo(2);
		assertThat(count(jdbcTemplate, "ai_runs")).isEqualTo(6);
		assertThat(count(jdbcTemplate, "ai_recommendations")).isEqualTo(2);
		assertThat(count(jdbcTemplate, "notifications")).isEqualTo(10);
		assertThat(count(jdbcTemplate, "audit_events")).isEqualTo(30);
		assertThat(queryCount(jdbcTemplate,
				"SELECT COUNT(*) FROM conflicts WHERE status IN ('OPEN','UNDER_REVIEW')")).isEqualTo(3);
		assertThat(queryCount(jdbcTemplate,
				"SELECT COUNT(*) FROM sla_instances WHERE status = 'AT_RISK'")).isEqualTo(1);
		assertThat(queryCount(jdbcTemplate,
				"SELECT COUNT(*) FROM approvals WHERE status = 'PENDING'")).isEqualTo(1);
		assertThat(queryCount(jdbcTemplate,
				"SELECT COUNT(*) FROM interventions WHERE status IN ('APPROVED','SCHEDULED')")).isEqualTo(5);
		assertThat(queryCount(jdbcTemplate,
				"SELECT COUNT(*) FROM interventions WHERE status = 'VERIFICATION_PENDING'")).isEqualTo(1);
		assertThat(queryCount(jdbcTemplate, """
				SELECT COUNT(*) FROM conflicts
				WHERE road_segment_id = md5('demo-road-segment-2')::uuid
				""")).as("INT-004 is the explicit no-conflict control").isZero();

		String hash = jdbcTemplate.queryForObject(
				"SELECT password_hash FROM users WHERE external_reference = 'DEMO-ADMIN-001'", String.class);
		assertThat(hash).isNotEqualTo(DEMO_PASSWORD);
		assertThat(passwordEncoder.matches(DEMO_PASSWORD, hash)).isTrue();
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM interventions
				WHERE status NOT IN (
				    'DRAFT','SUBMITTED','UNDER_REVIEW','ANALYSIS','COORDINATION_REQUIRED',
				    'COORDINATION_COMPLETE','APPROVAL_PENDING','APPROVED','SCHEDULED','IN_PROGRESS',
				    'RESTORATION','EVIDENCE_PENDING','VERIFICATION_PENDING','VERIFIED','CLOSED',
				    'REJECTED','CANCELLED','ON_HOLD','REOPENED'
				)
				""", Integer.class)).isZero();
	}

	private int count(JdbcTemplate jdbcTemplate, String table) {
		return queryCount(jdbcTemplate, "SELECT COUNT(*) FROM " + table);
	}

	private int queryCount(JdbcTemplate jdbcTemplate, String query) {
		Integer count = jdbcTemplate.queryForObject(query, Integer.class);
		return count == null ? 0 : count;
	}
}
