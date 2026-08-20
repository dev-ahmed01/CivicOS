package com.civicos.demo;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("demo")
@ConditionalOnProperty(prefix = "civicos.features", name = "demo-mode", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
	private static final int MINIMUM_DEMO_PASSWORD_LENGTH = 12;

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	private final String demoPassword;

	public DemoDataSeeder(DataSource dataSource, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
			@Value("${civicos.demo.password:}") String demoPassword) {
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
		this.demoPassword = demoPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(demoPassword) || demoPassword.length() < MINIMUM_DEMO_PASSWORD_LENGTH) {
			throw new IllegalStateException(
					"DEMO_PASSWORD must contain at least 12 characters when the demo profile is active");
		}
		ensureControlledDatabase();

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
		populator.addScript(new ClassPathResource("db/demo/seed_demo.sql"));
		populator.execute(dataSource);

		String encodedPassword = passwordEncoder.encode(demoPassword);
		int credentialCount = jdbcTemplate.update("""
				UPDATE users
				SET password_hash = ?, updated_at = TIMESTAMPTZ '2026-08-20 09:00:00+05:30'
				WHERE external_reference LIKE 'DEMO-%'
				  AND password_hash IS NULL
				""", encodedPassword);

		Map<String, Integer> counts = verifyMinimumDataset();
		log.info("Loaded controlled DEMO / SYNTHETIC CivicOS dataset: {} demo credentials, verified counts {}",
				credentialCount, counts);
	}

	private void ensureControlledDatabase() {
		Boolean markerPresent = jdbcTemplate.queryForObject("""
				SELECT EXISTS (
				    SELECT 1 FROM audit_events
				    WHERE action = 'DEMO_DATASET_LOADED'
				      AND metadata @> '{"dataset":"DEMO / SYNTHETIC"}'::jsonb
				)
				""", Boolean.class);
		if (Boolean.TRUE.equals(markerPresent)) {
			return;
		}

		Integer operationalRecords = jdbcTemplate.queryForObject("""
				SELECT
				    (SELECT COUNT(*) FROM users)
				  + (SELECT COUNT(*) FROM roads)
				  + (SELECT COUNT(*) FROM civic_cases)
				  + (SELECT COUNT(*) FROM interventions)
				  + (SELECT COUNT(*) FROM evidence)
				""", Integer.class);
		if (operationalRecords != null && operationalRecords > 0) {
			throw new IllegalStateException(
					"Refusing to overlay the DEMO / SYNTHETIC dataset on a database containing operational records");
		}
	}

	private Map<String, Integer> verifyMinimumDataset() {
		Map<String, CountRequirement> requirements = new LinkedHashMap<>();
		requirements.put("agencies", new CountRequirement(
				"SELECT COUNT(*) FROM agencies WHERE code IN ('BBMP','BESCOM','BWSSB','BMRCL','KPTCL','GAIL','TSP')", 7));
		requirements.put("users", new CountRequirement(
				"SELECT COUNT(*) FROM users WHERE external_reference LIKE 'DEMO-%'", 7));
		requirements.put("roads", new CountRequirement(
				"SELECT COUNT(*) FROM roads WHERE external_reference LIKE 'DEMO-R%'", 3));
		requirements.put("roadSegments", new CountRequirement(
				"SELECT COUNT(*) FROM road_segments WHERE external_reference LIKE 'DEMO-RS%'", 3));
		requirements.put("cases", new CountRequirement(
				"SELECT COUNT(*) FROM civic_cases WHERE case_number LIKE 'CASE-00%'", 3));
		requirements.put("observations", new CountRequirement(
				"SELECT COUNT(*) FROM citizen_observations WHERE id IN (SELECT md5('demo-observation-' || n)::uuid FROM generate_series(1, 5) n)", 5));
		requirements.put("interventions", new CountRequirement(
				"SELECT COUNT(*) FROM interventions WHERE intervention_number LIKE 'INT-00%'", 7));
		requirements.put("conflicts", new CountRequirement(
				"SELECT COUNT(*) FROM conflicts WHERE conflict_number IN ('C001','C002','C003')", 3));
		requirements.put("dependencies", new CountRequirement(
				"SELECT COUNT(*) FROM dependencies WHERE id IN (SELECT md5('demo-dependency-' || n)::uuid FROM generate_series(1, 5) n)", 5));
		requirements.put("approvals", new CountRequirement(
				"SELECT COUNT(*) FROM approvals WHERE id IN (SELECT md5('demo-approval-' || n)::uuid FROM generate_series(1, 6) n)", 5));
		requirements.put("slas", new CountRequirement(
				"SELECT COUNT(*) FROM sla_instances WHERE id IN (SELECT md5('demo-sla-' || n)::uuid FROM generate_series(1, 6) n)", 5));
		requirements.put("escalations", new CountRequirement(
				"SELECT COUNT(*) FROM escalations WHERE id = md5('demo-escalation-1')::uuid", 1));
		requirements.put("evidence", new CountRequirement(
				"SELECT COUNT(*) FROM evidence WHERE metadata @> '{\"dataset\":\"DEMO / SYNTHETIC\"}'::jsonb", 10));
		requirements.put("inspections", new CountRequirement(
				"SELECT COUNT(*) FROM inspections WHERE id IN (md5('demo-inspection-1')::uuid, md5('demo-inspection-2')::uuid)", 2));
		requirements.put("aiRuns", new CountRequirement(
				"SELECT COUNT(*) FROM ai_runs WHERE configuration @> '{\"dataset\":\"DEMO / SYNTHETIC\"}'::jsonb", 5));
		requirements.put("recommendations", new CountRequirement(
				"SELECT COUNT(*) FROM ai_recommendations WHERE id IN (md5('demo-recommendation-1')::uuid, md5('demo-recommendation-2')::uuid)", 2));
		requirements.put("notifications", new CountRequirement(
				"SELECT COUNT(*) FROM notifications WHERE id IN (SELECT md5('demo-notification-' || n)::uuid FROM generate_series(1, 10) n)", 10));
		requirements.put("auditEvents", new CountRequirement(
				"SELECT COUNT(*) FROM audit_events WHERE metadata @> '{\"dataset\":\"DEMO / SYNTHETIC\"}'::jsonb", 30));

		Map<String, Integer> actual = new LinkedHashMap<>();
		requirements.forEach((name, requirement) -> {
			Integer count = jdbcTemplate.queryForObject(requirement.query(), Integer.class);
			int safeCount = count == null ? 0 : count;
			if (safeCount < requirement.minimum()) {
				throw new IllegalStateException("Incomplete demo dataset: " + name + " expected at least "
						+ requirement.minimum() + " but found " + safeCount);
			}
			actual.put(name, safeCount);
		});
		return actual;
	}

	private record CountRequirement(String query, int minimum) {
	}
}
