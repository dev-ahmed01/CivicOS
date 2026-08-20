-- CivicOS controlled demonstration dataset.
-- Every operational record in this file is DEMO / SYNTHETIC. It is not a live
-- MARCS integration, an official project record, or evidence of AI accuracy.
-- This script is loaded only by DemoDataSeeder while the explicit demo profile is active.

INSERT INTO agencies (id, code, name, agency_type, jurisdiction, contact_email, active, created_at, updated_at)
VALUES
    (md5('demo-agency-BBMP')::uuid, 'BBMP', 'BBMP — DEMO / SYNTHETIC', 'GOVERNMENT', 'Bengaluru demo jurisdiction', 'demo-bbmp@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-agency-BESCOM')::uuid, 'BESCOM', 'BESCOM — DEMO / SYNTHETIC', 'UTILITY', 'Bengaluru demo jurisdiction', 'demo-bescom@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-agency-BWSSB')::uuid, 'BWSSB', 'BWSSB — DEMO / SYNTHETIC', 'UTILITY', 'Bengaluru demo jurisdiction', 'demo-bwssb@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-agency-BMRCL')::uuid, 'BMRCL', 'BMRCL — DEMO / SYNTHETIC', 'GOVERNMENT', 'Bengaluru demo jurisdiction', 'demo-bmrcl@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-agency-KPTCL')::uuid, 'KPTCL', 'KPTCL — DEMO / SYNTHETIC', 'UTILITY', 'Karnataka demo jurisdiction', 'demo-kptcl@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-agency-GAIL')::uuid, 'GAIL', 'GAIL — DEMO / SYNTHETIC', 'UTILITY', 'Bengaluru demo jurisdiction', 'demo-gail@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-agency-TSP')::uuid, 'TSP', 'Telecom Service Provider — DEMO / SYNTHETIC', 'TELECOM', 'Bengaluru demo jurisdiction', 'demo-tsp@example.invalid', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles (id, code, name, description, system_role, created_at)
VALUES
    (md5('demo-role-CITIZEN')::uuid, 'CITIZEN', 'Citizen', '[DEMO / SYNTHETIC] Citizen participation role.', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-role-AGENCY_OFFICER')::uuid, 'AGENCY_OFFICER', 'Agency Officer', '[DEMO / SYNTHETIC] Agency-scoped operational role.', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-role-COORDINATOR')::uuid, 'COORDINATOR', 'Coordinator', '[DEMO / SYNTHETIC] Cross-agency coordination role.', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-role-INSPECTOR')::uuid, 'INSPECTOR', 'Inspector', '[DEMO / SYNTHETIC] Field inspection and verification role.', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-role-ADMIN')::uuid, 'ADMIN', 'Administrator', '[DEMO / SYNTHETIC] Governance administrator, not an operational approver.', TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-role-CONTRACTOR')::uuid, 'CONTRACTOR', 'Contractor', '[DEMO / SYNTHETIC] Evidence-submission role.', FALSE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (id, code, description, created_at)
SELECT md5('demo-permission-' || code)::uuid,
       code,
       '[DEMO / SYNTHETIC] Controlled permission catalog entry.',
       TIMESTAMPTZ '2026-08-20 09:00:00+05:30'
FROM unnest(ARRAY[
    'USER_VIEW','USER_CREATE','USER_UPDATE','USER_DISABLE','ROLE_ASSIGN','AGENCY_ASSIGN',
    'ROAD_VIEW','ROAD_CREATE','ROAD_UPDATE','ROAD_GEOMETRY_UPDATE','ROAD_SEGMENT_VIEW','ROAD_SEGMENT_UPDATE',
    'INTERVENTION_VIEW','INTERVENTION_CREATE','INTERVENTION_UPDATE','INTERVENTION_SUBMIT','INTERVENTION_ASSIGN',
    'INTERVENTION_SCHEDULE','INTERVENTION_START','INTERVENTION_COMPLETE','INTERVENTION_CANCEL','INTERVENTION_HOLD',
    'INTERVENTION_RESUME','INTERVENTION_REOPEN','CONFLICT_VIEW','CONFLICT_ANALYSE','CONFLICT_ACKNOWLEDGE',
    'CONFLICT_ASSIGN','CONFLICT_RESOLVE','CONFLICT_ESCALATE','COORDINATION_VIEW','COORDINATION_TASK_CREATE',
    'COORDINATION_TASK_ASSIGN','COORDINATION_UPDATE','COORDINATION_COMPLETE','COORDINATION_ESCALATE',
    'APPROVAL_VIEW','APPROVAL_REQUEST','APPROVAL_APPROVE','APPROVAL_REJECT','APPROVAL_RETURN','APPROVAL_CONDITIONAL',
    'CLOSURE_APPROVAL','EVIDENCE_VIEW','EVIDENCE_UPLOAD','EVIDENCE_REVIEW','EVIDENCE_ACCEPT','EVIDENCE_REJECT',
    'INSPECTION_CREATE','INSPECTION_VIEW','INSPECTION_COMPLETE','VERIFICATION_PASS','VERIFICATION_FAIL',
    'REINSPECTION_REQUEST','SLA_VIEW','SLA_MANAGE','SLA_ESCALATE','SLA_OVERRIDE','OBSERVATION_CREATE',
    'OBSERVATION_VIEW_OWN','OBSERVATION_VIEW_RELEVANT','OBSERVATION_TRIAGE','OBSERVATION_MATCH',
    'CITIZEN_VALIDATION_CREATE','POLICY_VIEW','POLICY_MANAGE','AUDIT_VIEW','ADMIN_OVERRIDE','INTEGRATION_MANAGE'
]) AS permission_codes(code)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code = ANY (CASE role.code
    WHEN 'CITIZEN' THEN ARRAY['OBSERVATION_CREATE','OBSERVATION_VIEW_OWN','CITIZEN_VALIDATION_CREATE','EVIDENCE_UPLOAD']
    WHEN 'AGENCY_OFFICER' THEN ARRAY['ROAD_VIEW','ROAD_SEGMENT_VIEW','INTERVENTION_VIEW','INTERVENTION_CREATE','INTERVENTION_UPDATE','INTERVENTION_SUBMIT','INTERVENTION_START','INTERVENTION_COMPLETE','INTERVENTION_HOLD','INTERVENTION_RESUME','INTERVENTION_REOPEN','CONFLICT_VIEW','COORDINATION_VIEW','APPROVAL_VIEW','APPROVAL_REQUEST','APPROVAL_APPROVE','APPROVAL_REJECT','APPROVAL_RETURN','APPROVAL_CONDITIONAL','EVIDENCE_VIEW','EVIDENCE_UPLOAD','OBSERVATION_VIEW_RELEVANT']
    WHEN 'COORDINATOR' THEN ARRAY['ROAD_VIEW','ROAD_CREATE','ROAD_UPDATE','ROAD_GEOMETRY_UPDATE','ROAD_SEGMENT_VIEW','ROAD_SEGMENT_UPDATE','INTERVENTION_VIEW','INTERVENTION_UPDATE','INTERVENTION_ASSIGN','INTERVENTION_SCHEDULE','INTERVENTION_HOLD','INTERVENTION_RESUME','CONFLICT_VIEW','CONFLICT_ANALYSE','CONFLICT_ACKNOWLEDGE','CONFLICT_ASSIGN','CONFLICT_RESOLVE','CONFLICT_ESCALATE','COORDINATION_VIEW','COORDINATION_TASK_CREATE','COORDINATION_TASK_ASSIGN','COORDINATION_UPDATE','COORDINATION_COMPLETE','COORDINATION_ESCALATE','APPROVAL_VIEW','APPROVAL_REQUEST','EVIDENCE_VIEW','EVIDENCE_REVIEW','EVIDENCE_ACCEPT','EVIDENCE_REJECT','INSPECTION_CREATE','INSPECTION_VIEW','SLA_VIEW','SLA_ESCALATE','OBSERVATION_VIEW_RELEVANT','OBSERVATION_TRIAGE','OBSERVATION_MATCH','AUDIT_VIEW']
    WHEN 'INSPECTOR' THEN ARRAY['ROAD_VIEW','ROAD_SEGMENT_VIEW','INTERVENTION_VIEW','EVIDENCE_VIEW','EVIDENCE_UPLOAD','INSPECTION_VIEW','INSPECTION_COMPLETE','VERIFICATION_PASS','VERIFICATION_FAIL','REINSPECTION_REQUEST']
    WHEN 'ADMIN' THEN ARRAY['USER_VIEW','USER_CREATE','USER_UPDATE','USER_DISABLE','ROLE_ASSIGN','AGENCY_ASSIGN','ROAD_VIEW','ROAD_CREATE','ROAD_UPDATE','ROAD_GEOMETRY_UPDATE','ROAD_SEGMENT_VIEW','ROAD_SEGMENT_UPDATE','POLICY_VIEW','POLICY_MANAGE','SLA_VIEW','SLA_MANAGE','SLA_OVERRIDE','AUDIT_VIEW','ADMIN_OVERRIDE','INTEGRATION_MANAGE']
    WHEN 'CONTRACTOR' THEN ARRAY['ROAD_VIEW','ROAD_SEGMENT_VIEW','INTERVENTION_VIEW','INTERVENTION_START','INTERVENTION_COMPLETE','EVIDENCE_VIEW','EVIDENCE_UPLOAD']
END)
WHERE role.code IN ('CITIZEN','AGENCY_OFFICER','COORDINATOR','INSPECTOR','ADMIN','CONTRACTOR')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, agency_id, full_name, email, phone, password_hash, status, external_reference, created_at, updated_at)
VALUES
    (md5('demo-user-1')::uuid, NULL, 'Demo Citizen', 'citizen.demo@civicos.example.invalid', '+910000000001', NULL, 'ACTIVE', 'DEMO-CIT-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-2')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'Demo BBMP Engineer', 'engineer.demo@civicos.example.invalid', '+910000000002', NULL, 'ACTIVE', 'DEMO-BBMP-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-3')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'Demo City Coordinator', 'coordinator.demo@civicos.example.invalid', '+910000000003', NULL, 'ACTIVE', 'DEMO-BBMP-002', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-4')::uuid, (SELECT id FROM agencies WHERE code='BESCOM'), 'Demo BESCOM Officer', 'bescom.demo@civicos.example.invalid', '+910000000004', NULL, 'ACTIVE', 'DEMO-BESCOM-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-5')::uuid, (SELECT id FROM agencies WHERE code='BWSSB'), 'Demo BWSSB Officer', 'bwssb.demo@civicos.example.invalid', '+910000000005', NULL, 'ACTIVE', 'DEMO-BWSSB-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-6')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'Demo Field Inspector', 'inspector.demo@civicos.example.invalid', '+910000000006', NULL, 'ACTIVE', 'DEMO-INSP-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-7')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'Demo Administrator', 'admin.demo@civicos.example.invalid', '+910000000007', NULL, 'ACTIVE', 'DEMO-ADMIN-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-8')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'Demo Restoration Contractor', 'contractor.demo@civicos.example.invalid', '+910000000008', NULL, 'ACTIVE', 'DEMO-CONTRACT-001', TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id, assigned_at)
VALUES
    (md5('demo-user-1')::uuid, (SELECT id FROM roles WHERE code='CITIZEN'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-2')::uuid, (SELECT id FROM roles WHERE code='AGENCY_OFFICER'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-3')::uuid, (SELECT id FROM roles WHERE code='COORDINATOR'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-4')::uuid, (SELECT id FROM roles WHERE code='AGENCY_OFFICER'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-5')::uuid, (SELECT id FROM roles WHERE code='AGENCY_OFFICER'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-6')::uuid, (SELECT id FROM roles WHERE code='INSPECTOR'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-7')::uuid, (SELECT id FROM roles WHERE code='ADMIN'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-user-8')::uuid, (SELECT id FROM roles WHERE code='CONTRACTOR'), TIMESTAMPTZ '2026-08-20 09:00:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO roads (id, external_reference, name, classification, geometry, active, created_at, updated_at)
VALUES
    (md5('demo-road-1')::uuid, 'DEMO-R001', 'Outer Ring Road — Demo Segment', 'ARTERIAL', ST_Multi(ST_GeomFromText('LINESTRING(77.6400 12.9250,77.6480 12.9250)',4326)), TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-road-2')::uuid, 'DEMO-R002', 'Demo Link Road — Koramangala', 'COLLECTOR', ST_Multi(ST_GeomFromText('LINESTRING(77.6200 12.9350,77.6240 12.9350)',4326)), TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-road-3')::uuid, 'DEMO-R003', 'Demo Service Road — Whitefield', 'LOCAL', ST_Multi(ST_GeomFromText('LINESTRING(77.7450 12.9700,77.7480 12.9700)',4326)), TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO road_segments (id, road_id, external_reference, name, classification, surface_type, length_meters, geometry, active, created_at, updated_at)
VALUES
    (md5('demo-road-segment-1')::uuid, md5('demo-road-1')::uuid, 'DEMO-RS001', 'R001 — 850 m coordination corridor', 'ARTERIAL', 'BITUMINOUS', 850.00, ST_GeomFromText('LINESTRING(77.6400 12.9250,77.6480 12.9250)',4326), TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-road-segment-2')::uuid, md5('demo-road-2')::uuid, 'DEMO-RS002', 'R002 — 420 m control corridor', 'COLLECTOR', 'CONCRETE', 420.00, ST_GeomFromText('LINESTRING(77.6200 12.9350,77.6240 12.9350)',4326), TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30'),
    (md5('demo-road-segment-3')::uuid, md5('demo-road-3')::uuid, 'DEMO-RS003', 'R003 — 310 m exception corridor', 'LOCAL', 'PAVER_BLOCK', 310.00, ST_GeomFromText('LINESTRING(77.7450 12.9700,77.7480 12.9700)',4326), TRUE, TIMESTAMPTZ '2026-08-20 09:00:00+05:30', TIMESTAMPTZ '2026-08-20 09:00:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO civic_cases (id, case_number, source, status, priority, road_segment_id, created_at, updated_at)
VALUES
    (md5('demo-case-1')::uuid, 'CASE-001', 'CITIZEN', 'IN_PROGRESS', 'HIGH', md5('demo-road-segment-1')::uuid, TIMESTAMPTZ '2026-08-20 09:10:00+05:30', TIMESTAMPTZ '2026-08-20 09:10:00+05:30'),
    (md5('demo-case-2')::uuid, 'CASE-002', 'AGENCY', 'UNDER_REVIEW', 'NORMAL', md5('demo-road-segment-2')::uuid, TIMESTAMPTZ '2026-08-20 09:11:00+05:30', TIMESTAMPTZ '2026-08-20 09:11:00+05:30'),
    (md5('demo-case-3')::uuid, 'CASE-003', 'CITIZEN', 'PENDING_VERIFICATION', 'HIGH', md5('demo-road-segment-3')::uuid, TIMESTAMPTZ '2026-08-20 09:12:00+05:30', TIMESTAMPTZ '2026-08-20 09:12:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO citizen_observations (id, case_id, submitted_by, category, description, location, road_segment_id, submitted_at, status, ai_suggested_category, ai_confidence, created_at, updated_at)
VALUES
    (md5('demo-observation-1')::uuid, md5('demo-case-1')::uuid, md5('demo-user-1')::uuid, 'REPEAT_EXCAVATION', '[DEMO / SYNTHETIC] Citizen reports repeated markings and planned cuts on the same corridor.', ST_GeomFromText('POINT(77.6430 12.9250)',4326), md5('demo-road-segment-1')::uuid, TIMESTAMPTZ '2026-08-20 09:20:00+05:30', 'MATCHED', 'REPEAT_EXCAVATION', 0.9100, TIMESTAMPTZ '2026-08-20 09:20:00+05:30', TIMESTAMPTZ '2026-08-20 09:20:00+05:30'),
    (md5('demo-observation-2')::uuid, md5('demo-case-1')::uuid, md5('demo-user-1')::uuid, 'REPEAT_EXCAVATION', '[DEMO / SYNTHETIC] Duplicate report for the same physical-road problem.', ST_GeomFromText('POINT(77.6431 12.9250)',4326), md5('demo-road-segment-1')::uuid, TIMESTAMPTZ '2026-08-20 09:22:00+05:30', 'DUPLICATE', 'REPEAT_EXCAVATION', 0.8800, TIMESTAMPTZ '2026-08-20 09:22:00+05:30', TIMESTAMPTZ '2026-08-20 09:22:00+05:30'),
    (md5('demo-observation-3')::uuid, md5('demo-case-1')::uuid, md5('demo-user-1')::uuid, 'CITIZEN_DISAGREEMENT', '[DEMO / SYNTHETIC] Citizen disputes that the earlier restoration is satisfactory.', ST_GeomFromText('POINT(77.6460 12.9250)',4326), md5('demo-road-segment-1')::uuid, TIMESTAMPTZ '2026-08-20 09:24:00+05:30', 'FLAGGED', NULL, NULL, TIMESTAMPTZ '2026-08-20 09:24:00+05:30', TIMESTAMPTZ '2026-08-20 09:24:00+05:30'),
    (md5('demo-observation-4')::uuid, md5('demo-case-3')::uuid, md5('demo-user-1')::uuid, 'UNAUTHORISED_SUSPECTED_WORK', '[DEMO / SYNTHETIC] Suspected excavation without a visible authorization marker.', ST_GeomFromText('POINT(77.7460 12.9700)',4326), md5('demo-road-segment-3')::uuid, TIMESTAMPTZ '2026-08-20 09:26:00+05:30', 'FORWARDED', 'UNAUTHORISED_SUSPECTED_WORK', 0.7300, TIMESTAMPTZ '2026-08-20 09:26:00+05:30', TIMESTAMPTZ '2026-08-20 09:26:00+05:30'),
    (md5('demo-observation-5')::uuid, md5('demo-case-3')::uuid, md5('demo-user-1')::uuid, 'FAILED_RESTORATION', '[DEMO / SYNTHETIC] Surface settlement observed after restoration.', ST_GeomFromText('POINT(77.7470 12.9700)',4326), md5('demo-road-segment-3')::uuid, TIMESTAMPTZ '2026-08-20 09:28:00+05:30', 'MATCHED', 'FAILED_RESTORATION', 0.9500, TIMESTAMPTZ '2026-08-20 09:28:00+05:30', TIMESTAMPTZ '2026-08-20 09:28:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO interventions (id, intervention_number, case_id, agency_id, intervention_type, description, road_segment_id, geometry, planned_start, planned_end, actual_start, actual_end, status, priority, created_by, created_at, updated_at)
VALUES
    (md5('demo-intervention-1')::uuid, 'INT-001', md5('demo-case-1')::uuid, (SELECT id FROM agencies WHERE code='BESCOM'), 'ELECTRICAL_CABLE', '[DEMO / SYNTHETIC] BESCOM electrical cable intervention. Simulated source record; no live MARCS connection.', md5('demo-road-segment-1')::uuid, ST_GeomFromText('LINESTRING(77.6405 12.9250,77.6440 12.9250)',4326), TIMESTAMPTZ '2026-09-10 09:00:00+05:30', TIMESTAMPTZ '2026-09-12 18:00:00+05:30', NULL, NULL, 'SCHEDULED', 'HIGH', md5('demo-user-4')::uuid, TIMESTAMPTZ '2026-08-20 10:00:00+05:30', TIMESTAMPTZ '2026-08-20 10:00:00+05:30'),
    (md5('demo-intervention-2')::uuid, 'INT-002', md5('demo-case-1')::uuid, (SELECT id FROM agencies WHERE code='BWSSB'), 'WATER_PIPE', '[DEMO / SYNTHETIC] BWSSB water-pipeline intervention. Simulated source record; no live MARCS connection.', md5('demo-road-segment-1')::uuid, ST_GeomFromText('LINESTRING(77.6420 12.9250,77.6460 12.9250)',4326), TIMESTAMPTZ '2026-09-13 09:00:00+05:30', TIMESTAMPTZ '2026-09-15 18:00:00+05:30', NULL, NULL, 'SCHEDULED', 'HIGH', md5('demo-user-5')::uuid, TIMESTAMPTZ '2026-08-20 10:01:00+05:30', TIMESTAMPTZ '2026-08-20 10:01:00+05:30'),
    (md5('demo-intervention-3')::uuid, 'INT-003', md5('demo-case-1')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'CONSOLIDATED_RESTORATION', '[DEMO / SYNTHETIC] BBMP consolidated restoration, moved by human coordination to follow all utility works.', md5('demo-road-segment-1')::uuid, ST_GeomFromText('LINESTRING(77.6405 12.9250,77.6470 12.9250)',4326), TIMESTAMPTZ '2026-09-17 09:00:00+05:30', TIMESTAMPTZ '2026-09-19 18:00:00+05:30', NULL, NULL, 'SCHEDULED', 'HIGH', md5('demo-user-2')::uuid, TIMESTAMPTZ '2026-08-20 10:02:00+05:30', TIMESTAMPTZ '2026-08-20 10:02:00+05:30'),
    (md5('demo-intervention-4')::uuid, 'INT-004', md5('demo-case-2')::uuid, (SELECT id FROM agencies WHERE code='BESCOM'), 'ELECTRICAL_MAINTENANCE', '[DEMO / SYNTHETIC] Negative control: isolated maintenance with no spatial or temporal conflict.', md5('demo-road-segment-2')::uuid, ST_GeomFromText('LINESTRING(77.6205 12.9350,77.6215 12.9350)',4326), TIMESTAMPTZ '2026-09-20 09:00:00+05:30', TIMESTAMPTZ '2026-09-20 17:00:00+05:30', NULL, NULL, 'SCHEDULED', 'NORMAL', md5('demo-user-4')::uuid, TIMESTAMPTZ '2026-08-20 10:03:00+05:30', TIMESTAMPTZ '2026-08-20 10:03:00+05:30'),
    (md5('demo-intervention-5')::uuid, 'INT-005', md5('demo-case-1')::uuid, (SELECT id FROM agencies WHERE code='TSP'), 'TELECOM_OFC', '[DEMO / SYNTHETIC] Telecom optical-fibre intervention in the shared R001 physical corridor.', md5('demo-road-segment-1')::uuid, ST_GeomFromText('LINESTRING(77.6430 12.9250,77.6470 12.9250)',4326), TIMESTAMPTZ '2026-09-14 09:00:00+05:30', TIMESTAMPTZ '2026-09-16 18:00:00+05:30', NULL, NULL, 'SCHEDULED', 'HIGH', md5('demo-user-3')::uuid, TIMESTAMPTZ '2026-08-20 10:04:00+05:30', TIMESTAMPTZ '2026-08-20 10:04:00+05:30'),
    (md5('demo-intervention-6')::uuid, 'INT-006', md5('demo-case-3')::uuid, (SELECT id FROM agencies WHERE code='GAIL'), 'SUSPECTED_UNAUTHORISED_WORK', '[DEMO / SYNTHETIC] Negative-path record awaiting a human approval decision; no authorization is asserted.', md5('demo-road-segment-3')::uuid, ST_GeomFromText('LINESTRING(77.7455 12.9700,77.7465 12.9700)',4326), TIMESTAMPTZ '2026-09-05 09:00:00+05:30', TIMESTAMPTZ '2026-09-05 18:00:00+05:30', NULL, NULL, 'APPROVAL_PENDING', 'CRITICAL', md5('demo-user-3')::uuid, TIMESTAMPTZ '2026-08-20 10:05:00+05:30', TIMESTAMPTZ '2026-08-20 10:05:00+05:30'),
    (md5('demo-intervention-7')::uuid, 'INT-007', md5('demo-case-3')::uuid, (SELECT id FROM agencies WHERE code='BBMP'), 'RESTORATION_REPAIR', '[DEMO / SYNTHETIC] Negative-path failed restoration awaiting corrective action.', md5('demo-road-segment-3')::uuid, ST_GeomFromText('LINESTRING(77.7465 12.9700,77.7475 12.9700)',4326), TIMESTAMPTZ '2026-08-10 09:00:00+05:30', TIMESTAMPTZ '2026-08-12 18:00:00+05:30', TIMESTAMPTZ '2026-08-10 09:15:00+05:30', TIMESTAMPTZ '2026-08-12 17:30:00+05:30', 'VERIFICATION_PENDING', 'HIGH', md5('demo-user-2')::uuid, TIMESTAMPTZ '2026-08-01 10:00:00+05:30', TIMESTAMPTZ '2026-08-20 10:06:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO dependencies (id, source_intervention_id, target_intervention_id, dependency_type, required, status, reason, created_at)
VALUES
    (md5('demo-dependency-1')::uuid, md5('demo-intervention-1')::uuid, md5('demo-intervention-2')::uuid, 'MUST_COMPLETE_BEFORE', TRUE, 'ACTIVE', '[DEMO / SYNTHETIC] Electrical work precedes water work in the coordinated sequence.', TIMESTAMPTZ '2026-08-20 10:20:00+05:30'),
    (md5('demo-dependency-2')::uuid, md5('demo-intervention-2')::uuid, md5('demo-intervention-5')::uuid, 'MUST_COMPLETE_BEFORE', TRUE, 'ACTIVE', '[DEMO / SYNTHETIC] Water work precedes telecom work.', TIMESTAMPTZ '2026-08-20 10:21:00+05:30'),
    (md5('demo-dependency-3')::uuid, md5('demo-intervention-1')::uuid, md5('demo-intervention-5')::uuid, 'MUST_VERIFY_BEFORE', TRUE, 'ACTIVE', '[DEMO / SYNTHETIC] Electrical corridor clearance is required before OFC execution.', TIMESTAMPTZ '2026-08-20 10:22:00+05:30'),
    (md5('demo-dependency-4')::uuid, md5('demo-intervention-5')::uuid, md5('demo-intervention-3')::uuid, 'RESTORATION_DEPENDS_ON', TRUE, 'ACTIVE', '[DEMO / SYNTHETIC] Consolidated restoration follows telecom completion.', TIMESTAMPTZ '2026-08-20 10:23:00+05:30'),
    (md5('demo-dependency-5')::uuid, md5('demo-intervention-2')::uuid, md5('demo-intervention-3')::uuid, 'RESTORATION_DEPENDS_ON', TRUE, 'ACTIVE', '[DEMO / SYNTHETIC] Consolidated restoration follows water work.', TIMESTAMPTZ '2026-08-20 10:24:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO conflicts (id, conflict_number, road_segment_id, conflict_type, severity, status, detected_at, explanation)
VALUES
    (md5('demo-conflict-1')::uuid, 'C001', md5('demo-road-segment-1')::uuid, 'UNSAFE_SEQUENCING', 'HIGH', 'UNDER_REVIEW', TIMESTAMPTZ '2026-08-20 10:30:00+05:30', '[DEMO / SYNTHETIC] Three interventions affect the same road inside a ten-day window; restoration must follow utility work.'),
    (md5('demo-conflict-2')::uuid, 'C002', md5('demo-road-segment-1')::uuid, 'MULTI_AGENCY_COORDINATION', 'HIGH', 'OPEN', TIMESTAMPTZ '2026-08-20 10:31:00+05:30', '[DEMO / SYNTHETIC] BESCOM, BWSSB, telecom, and BBMP plans are one physical-road coordination problem.'),
    (md5('demo-conflict-3')::uuid, 'C003', md5('demo-road-segment-1')::uuid, 'REPEAT_DIGGING_RISK', 'MEDIUM', 'OPEN', TIMESTAMPTZ '2026-08-20 10:32:00+05:30', '[DEMO / SYNTHETIC] Separate excavations create a repeat-digging risk unless sequenced together.')
ON CONFLICT DO NOTHING;

INSERT INTO conflict_interventions (conflict_id, intervention_id)
VALUES
    (md5('demo-conflict-1')::uuid, md5('demo-intervention-2')::uuid),
    (md5('demo-conflict-1')::uuid, md5('demo-intervention-5')::uuid),
    (md5('demo-conflict-1')::uuid, md5('demo-intervention-3')::uuid),
    (md5('demo-conflict-2')::uuid, md5('demo-intervention-1')::uuid),
    (md5('demo-conflict-2')::uuid, md5('demo-intervention-2')::uuid),
    (md5('demo-conflict-2')::uuid, md5('demo-intervention-5')::uuid),
    (md5('demo-conflict-2')::uuid, md5('demo-intervention-3')::uuid),
    (md5('demo-conflict-3')::uuid, md5('demo-intervention-1')::uuid),
    (md5('demo-conflict-3')::uuid, md5('demo-intervention-2')::uuid),
    (md5('demo-conflict-3')::uuid, md5('demo-intervention-5')::uuid)
ON CONFLICT DO NOTHING;

INSERT INTO ai_runs (
    id, operation, provider, model, prompt_version, schema_version, input_reference, output,
    confidence, status, created_at, completed_at, requested_by, entity_type, entity_id,
    model_version, input_hash, configuration, started_at, input_tokens, output_tokens,
    estimated_cost, latency_ms, review_required
)
SELECT
    md5('demo-ai-run-' || seed.sequence)::uuid,
    seed.operation,
    'mock',
    'deterministic-demo-advisor',
    'demo-v1',
    'demo-v1',
    jsonb_build_object(
        'dataset', 'DEMO / SYNTHETIC',
        'entityType', seed.entity_type,
        'entityId', seed.entity_id,
        'notice', 'Simulated advisory input; not a live external integration.'
    ),
    jsonb_build_object(
        'dataset', 'DEMO / SYNTHETIC',
        'advisoryOnly', TRUE,
        'summary', seed.summary,
        'humanReviewRequired', TRUE
    ),
    seed.confidence,
    'COMPLETED',
    TIMESTAMPTZ '2026-08-20 10:40:00+05:30' + (seed.sequence || ' minutes')::interval,
    TIMESTAMPTZ '2026-08-20 10:40:20+05:30' + (seed.sequence || ' minutes')::interval,
    md5('demo-user-3')::uuid,
    seed.entity_type,
    seed.entity_id::uuid,
    'deterministic-demo-advisor-v1',
    md5('demo-ai-input-' || seed.sequence) || md5('demo-ai-input-' || seed.sequence),
    '{"dataset":"DEMO / SYNTHETIC","mode":"offline-mock","advisoryOnly":true}'::jsonb,
    TIMESTAMPTZ '2026-08-20 10:40:05+05:30' + (seed.sequence || ' minutes')::interval,
    0,
    0,
    0.000000,
    15,
    TRUE
FROM (VALUES
    (1, 'CLASSIFICATION', 'CITIZEN_OBSERVATION', md5('demo-observation-1'), '[DEMO / SYNTHETIC] Suggested repeat-excavation classification.', 0.9100),
    (2, 'MATCHING_ASSISTANCE', 'CIVIC_CASE', md5('demo-case-1'), '[DEMO / SYNTHETIC] Suggested matching the observation to CASE-001.', 0.8800),
    (3, 'CONFLICT_EXPLANATION', 'CONFLICT', md5('demo-conflict-1'), '[DEMO / SYNTHETIC] Explained deterministic conflict facts without creating them.', 0.9000),
    (4, 'RECOMMENDATION_GENERATION', 'CONFLICT', md5('demo-conflict-1'), '[DEMO / SYNTHETIC] Proposed a utility-first consolidated sequence.', 0.8700),
    (5, 'RECOMMENDATION_GENERATION', 'CONFLICT', md5('demo-conflict-2'), '[DEMO / SYNTHETIC] Proposed cross-agency coordination for four plans.', 0.8200),
    (6, 'EVIDENCE_ANALYSIS', 'INTERVENTION', md5('demo-intervention-7'), '[DEMO / SYNTHETIC] Flagged possible surface settlement for inspector review.', 0.7600)
) AS seed(sequence, operation, entity_type, entity_id, summary, confidence)
ON CONFLICT DO NOTHING;

INSERT INTO ai_recommendations (
    id, ai_run_id, conflict_id, recommendation_type, recommendation, confidence, status,
    reviewed_by, reviewed_at, review_reason, created_at
)
VALUES
    (
        md5('demo-recommendation-1')::uuid,
        md5('demo-ai-run-4')::uuid,
        md5('demo-conflict-1')::uuid,
        'COORDINATED_SEQUENCE',
        '{"dataset":"DEMO / SYNTHETIC","advisoryOnly":true,"estimatedAvoidedRepeatExcavation":true,"sequence":["INT-001 BESCOM","INT-002 BWSSB","INT-005 Telecom","INT-003 consolidated BBMP restoration"],"reason":"Utility works precede one consolidated restoration."}'::jsonb,
        0.8700,
        'ACCEPTED',
        md5('demo-user-3')::uuid,
        TIMESTAMPTZ '2026-08-20 11:00:00+05:30',
        '[DEMO / SYNTHETIC] Human coordinator accepted the advisory sequence with a modified restoration window.',
        TIMESTAMPTZ '2026-08-20 10:44:20+05:30'
    ),
    (
        md5('demo-recommendation-2')::uuid,
        md5('demo-ai-run-5')::uuid,
        md5('demo-conflict-2')::uuid,
        'CROSS_AGENCY_REVIEW',
        '{"dataset":"DEMO / SYNTHETIC","advisoryOnly":true,"sequence":["BESCOM","BWSSB","Telecom","BBMP restoration"],"reason":"Treat four records as one physical-road problem."}'::jsonb,
        0.8200,
        'PENDING_REVIEW',
        NULL,
        NULL,
        NULL,
        TIMESTAMPTZ '2026-08-20 10:45:20+05:30'
    )
ON CONFLICT DO NOTHING;

INSERT INTO coordination_decisions (
    id, conflict_id, coordinator_id, decision_type, decision_text,
    accepted_recommendation_id, created_at
)
VALUES (
    md5('demo-coordination-decision-1')::uuid,
    md5('demo-conflict-1')::uuid,
    md5('demo-user-3')::uuid,
    'ACCEPT_WITH_MODIFICATION',
    '[DEMO / SYNTHETIC] Human decision: retain BESCOM → BWSSB → Telecom → consolidated restoration and move BBMP restoration to 17–19 September.',
    md5('demo-recommendation-1')::uuid,
    TIMESTAMPTZ '2026-08-20 11:00:00+05:30'
)
ON CONFLICT DO NOTHING;

INSERT INTO approvals (id, intervention_id, actor_id, status, decision, reason, conditions, created_at, decided_at)
VALUES
    (md5('demo-approval-1')::uuid, md5('demo-intervention-1')::uuid, md5('demo-user-4')::uuid, 'APPROVED', 'APPROVE', '[DEMO / SYNTHETIC] Agency approval recorded by an authorized officer.', '[]'::jsonb, TIMESTAMPTZ '2026-08-20 11:10:00+05:30', TIMESTAMPTZ '2026-08-20 11:20:00+05:30'),
    (md5('demo-approval-2')::uuid, md5('demo-intervention-2')::uuid, md5('demo-user-5')::uuid, 'APPROVED', 'APPROVE', '[DEMO / SYNTHETIC] Agency approval recorded by an authorized officer.', '[]'::jsonb, TIMESTAMPTZ '2026-08-20 11:11:00+05:30', TIMESTAMPTZ '2026-08-20 11:21:00+05:30'),
    (md5('demo-approval-3')::uuid, md5('demo-intervention-3')::uuid, md5('demo-user-2')::uuid, 'APPROVED_WITH_CONDITIONS', 'APPROVE_WITH_CONDITIONS', '[DEMO / SYNTHETIC] Consolidated restoration must follow all active dependencies.', '[{"condition":"All utility completion evidence accepted"}]'::jsonb, TIMESTAMPTZ '2026-08-20 11:12:00+05:30', TIMESTAMPTZ '2026-08-20 11:22:00+05:30'),
    (md5('demo-approval-4')::uuid, md5('demo-intervention-4')::uuid, md5('demo-user-4')::uuid, 'APPROVED', 'APPROVE', '[DEMO / SYNTHETIC] Isolated control intervention approved.', '[]'::jsonb, TIMESTAMPTZ '2026-08-20 11:13:00+05:30', TIMESTAMPTZ '2026-08-20 11:23:00+05:30'),
    (md5('demo-approval-5')::uuid, md5('demo-intervention-5')::uuid, md5('demo-user-3')::uuid, 'APPROVED_WITH_CONDITIONS', 'APPROVE_WITH_CONDITIONS', '[DEMO / SYNTHETIC] Telecom work is conditional on coordinated sequencing.', '[{"condition":"Follow C001 human coordination decision"}]'::jsonb, TIMESTAMPTZ '2026-08-20 11:14:00+05:30', TIMESTAMPTZ '2026-08-20 11:24:00+05:30'),
    (md5('demo-approval-6')::uuid, md5('demo-intervention-6')::uuid, NULL, 'PENDING', NULL, '[DEMO / SYNTHETIC] Awaiting an authorized human decision on suspected unauthorized work.', '[]'::jsonb, TIMESTAMPTZ '2026-08-20 11:15:00+05:30', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO sla_instances (id, target_type, target_id, sla_type, start_at, deadline, status, paused_at, completed_at)
VALUES
    (md5('demo-sla-1')::uuid, 'CONFLICT', md5('demo-conflict-1')::uuid, 'COORDINATION_HIGH', TIMESTAMPTZ '2026-08-20 10:30:00+05:30', TIMESTAMPTZ '2026-08-21 12:00:00+05:30', 'AT_RISK', NULL, NULL),
    (md5('demo-sla-2')::uuid, 'CONFLICT', md5('demo-conflict-2')::uuid, 'COORDINATION_HIGH', TIMESTAMPTZ '2026-08-20 10:31:00+05:30', TIMESTAMPTZ '2026-08-21 17:00:00+05:30', 'NORMAL', NULL, NULL),
    (md5('demo-sla-3')::uuid, 'CONFLICT', md5('demo-conflict-3')::uuid, 'COORDINATION_MEDIUM', TIMESTAMPTZ '2026-08-20 10:32:00+05:30', TIMESTAMPTZ '2026-08-24 17:00:00+05:30', 'NORMAL', NULL, NULL),
    (md5('demo-sla-4')::uuid, 'APPROVAL', md5('demo-approval-6')::uuid, 'APPROVAL_REVIEW', TIMESTAMPTZ '2026-08-20 11:15:00+05:30', TIMESTAMPTZ '2026-08-21 17:00:00+05:30', 'NORMAL', NULL, NULL),
    (md5('demo-sla-5')::uuid, 'INTERVENTION', md5('demo-intervention-7')::uuid, 'VERIFICATION', TIMESTAMPTZ '2026-08-20 08:00:00+05:30', TIMESTAMPTZ '2026-08-21 17:00:00+05:30', 'NORMAL', NULL, NULL),
    (md5('demo-sla-6')::uuid, 'CIVIC_CASE', md5('demo-case-2')::uuid, 'REVIEW', TIMESTAMPTZ '2026-08-20 09:11:00+05:30', TIMESTAMPTZ '2026-08-20 18:00:00+05:30', 'BREACHED', NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO escalations (id, sla_id, level, status, reason, escalated_to, created_at, resolved_at)
VALUES (
    md5('demo-escalation-1')::uuid,
    md5('demo-sla-6')::uuid,
    1,
    'ACKNOWLEDGED',
    '[DEMO / SYNTHETIC] Control-case review exceeded its configured demonstration deadline.',
    md5('demo-user-3')::uuid,
    TIMESTAMPTZ '2026-08-20 18:05:00+05:30',
    NULL
)
ON CONFLICT DO NOTHING;

INSERT INTO evidence (
    id, target_type, target_id, uploaded_by, evidence_type, file_reference, original_filename,
    mime_type, file_size_bytes, checksum, captured_at, latitude, longitude, captured_location,
    metadata, status, created_at
)
SELECT
    md5('demo-evidence-' || seed.sequence)::uuid,
    seed.target_type,
    seed.target_id::uuid,
    seed.uploaded_by::uuid,
    seed.evidence_type,
    'demo/synthetic/evidence-' || lpad(seed.sequence::text, 2, '0') || '.jpg',
    'DEMO_SYNTHETIC_' || lpad(seed.sequence::text, 2, '0') || '.jpg',
    'image/jpeg',
    1024 + seed.sequence,
    md5('demo-evidence-checksum-' || seed.sequence) || md5('demo-evidence-checksum-' || seed.sequence),
    TIMESTAMPTZ '2026-08-20 12:00:00+05:30' + (seed.sequence || ' minutes')::interval,
    seed.latitude,
    seed.longitude,
    ST_SetSRID(ST_MakePoint(seed.longitude, seed.latitude), 4326),
    jsonb_build_object(
        'dataset', 'DEMO / SYNTHETIC',
        'simulatedFile', TRUE,
        'notice', 'Metadata-only demonstration evidence; not an official field artifact.'
    ),
    'ACCEPTED',
    TIMESTAMPTZ '2026-08-20 12:00:00+05:30' + (seed.sequence || ' minutes')::interval
FROM (VALUES
    (1, 'INTERVENTION', md5('demo-intervention-1'), md5('demo-user-4'), 'BEFORE_WORK', 12.925000::numeric, 77.641000::numeric),
    (2, 'INTERVENTION', md5('demo-intervention-2'), md5('demo-user-5'), 'BEFORE_WORK', 12.925000::numeric, 77.642000::numeric),
    (3, 'INTERVENTION', md5('demo-intervention-3'), md5('demo-user-8'), 'BEFORE_WORK', 12.925000::numeric, 77.644000::numeric),
    (4, 'INTERVENTION', md5('demo-intervention-4'), md5('demo-user-4'), 'DOCUMENT', 12.935000::numeric, 77.621000::numeric),
    (5, 'INTERVENTION', md5('demo-intervention-5'), md5('demo-user-3'), 'BEFORE_WORK', 12.925000::numeric, 77.645000::numeric),
    (6, 'INTERVENTION', md5('demo-intervention-7'), md5('demo-user-8'), 'DURING_WORK', 12.970000::numeric, 77.746500::numeric),
    (7, 'INTERVENTION', md5('demo-intervention-7'), md5('demo-user-8'), 'COMPLETION', 12.970000::numeric, 77.747000::numeric),
    (8, 'INTERVENTION', md5('demo-intervention-7'), md5('demo-user-8'), 'RESTORATION', 12.970000::numeric, 77.747200::numeric),
    (9, 'INTERVENTION', md5('demo-intervention-7'), md5('demo-user-6'), 'INSPECTION', 12.970000::numeric, 77.747300::numeric),
    (10, 'CITIZEN_OBSERVATION', md5('demo-observation-5'), md5('demo-user-1'), 'CITIZEN_VALIDATION', 12.970000::numeric, 77.747000::numeric),
    (11, 'CIVIC_CASE', md5('demo-case-1'), md5('demo-user-3'), 'DOCUMENT', 12.925000::numeric, 77.643000::numeric),
    (12, 'CONFLICT', md5('demo-conflict-1'), md5('demo-user-3'), 'DOCUMENT', 12.925000::numeric, 77.644000::numeric)
) AS seed(sequence, target_type, target_id, uploaded_by, evidence_type, latitude, longitude)
ON CONFLICT DO NOTHING;

INSERT INTO inspections (id, intervention_id, inspector_id, status, result, started_at, completed_at, notes, created_at)
VALUES
    (md5('demo-inspection-1')::uuid, md5('demo-intervention-7')::uuid, md5('demo-user-6')::uuid, 'COMPLETED', 'FAILED', TIMESTAMPTZ '2026-08-20 14:00:00+05:30', TIMESTAMPTZ '2026-08-20 14:30:00+05:30', '[DEMO / SYNTHETIC] Surface settlement requires corrective action.', TIMESTAMPTZ '2026-08-20 13:30:00+05:30'),
    (md5('demo-inspection-2')::uuid, md5('demo-intervention-3')::uuid, md5('demo-user-6')::uuid, 'SCHEDULED', NULL, NULL, NULL, '[DEMO / SYNTHETIC] Future inspection for consolidated R001 restoration.', TIMESTAMPTZ '2026-08-20 13:31:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO verifications (id, target_type, target_id, source, result, submitted_by, reason, created_at)
VALUES
    (md5('demo-verification-1')::uuid, 'INTERVENTION', md5('demo-intervention-7')::uuid, 'FIELD_INSPECTOR', 'FAILED', md5('demo-user-6')::uuid, '[DEMO / SYNTHETIC] Inspector found settlement after restoration.', TIMESTAMPTZ '2026-08-20 14:31:00+05:30'),
    (md5('demo-verification-2')::uuid, 'INTERVENTION', md5('demo-intervention-7')::uuid, 'CITIZEN', 'DISPUTED', md5('demo-user-1')::uuid, '[DEMO / SYNTHETIC] Citizen disagrees that the restoration is satisfactory.', TIMESTAMPTZ '2026-08-20 14:35:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO notifications (id, notification_type, recipient_id, title, message, target_type, target_id, read_at, created_at)
VALUES
    (md5('demo-notification-1')::uuid, 'CONFLICT_DETECTED', md5('demo-user-3')::uuid, 'DEMO: High conflict C001', '[DEMO / SYNTHETIC] Unsafe sequencing requires human coordination.', 'CONFLICT', md5('demo-conflict-1')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:01:00+05:30'),
    (md5('demo-notification-2')::uuid, 'CONFLICT_DETECTED', md5('demo-user-3')::uuid, 'DEMO: Multi-agency conflict C002', '[DEMO / SYNTHETIC] Four plans affect one physical road.', 'CONFLICT', md5('demo-conflict-2')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:02:00+05:30'),
    (md5('demo-notification-3')::uuid, 'CONFLICT_DETECTED', md5('demo-user-3')::uuid, 'DEMO: Repeat-digging risk C003', '[DEMO / SYNTHETIC] Review the shared excavation window.', 'CONFLICT', md5('demo-conflict-3')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:03:00+05:30'),
    (md5('demo-notification-4')::uuid, 'APPROVAL_PENDING', md5('demo-user-3')::uuid, 'DEMO: Approval pending', '[DEMO / SYNTHETIC] INT-006 awaits an authorized human decision.', 'INTERVENTION', md5('demo-intervention-6')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:04:00+05:30'),
    (md5('demo-notification-5')::uuid, 'DEADLINE_APPROACHING', md5('demo-user-3')::uuid, 'DEMO: Coordination SLA at risk', '[DEMO / SYNTHETIC] C001 is inside the configured at-risk window.', 'CONFLICT', md5('demo-conflict-1')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:05:00+05:30'),
    (md5('demo-notification-6')::uuid, 'SLA_BREACHED', md5('demo-user-3')::uuid, 'DEMO: Review SLA breached', '[DEMO / SYNTHETIC] CASE-002 control-case review requires acknowledgement.', 'CIVIC_CASE', md5('demo-case-2')::uuid, TIMESTAMPTZ '2026-08-20 15:16:00+05:30', TIMESTAMPTZ '2026-08-20 15:06:00+05:30'),
    (md5('demo-notification-7')::uuid, 'VERIFICATION_REQUESTED', md5('demo-user-6')::uuid, 'DEMO: Inspection scheduled', '[DEMO / SYNTHETIC] INT-003 restoration inspection is scheduled.', 'INTERVENTION', md5('demo-intervention-3')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:07:00+05:30'),
    (md5('demo-notification-8')::uuid, 'VERIFICATION_FAILED', md5('demo-user-2')::uuid, 'DEMO: Restoration verification failed', '[DEMO / SYNTHETIC] INT-007 requires corrective action.', 'INTERVENTION', md5('demo-intervention-7')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:08:00+05:30'),
    (md5('demo-notification-9')::uuid, 'CITIZEN_VALIDATION', md5('demo-user-3')::uuid, 'DEMO: Citizen disagreement', '[DEMO / SYNTHETIC] Citizen validation disputed INT-007 restoration.', 'INTERVENTION', md5('demo-intervention-7')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:09:00+05:30'),
    (md5('demo-notification-10')::uuid, 'DEPENDENCY_BLOCKED', md5('demo-user-8')::uuid, 'DEMO: Restoration dependency', '[DEMO / SYNTHETIC] Consolidated restoration waits for utility completion.', 'INTERVENTION', md5('demo-intervention-3')::uuid, NULL, TIMESTAMPTZ '2026-08-20 15:10:00+05:30')
ON CONFLICT DO NOTHING;

INSERT INTO audit_events (
    id, event_id, actor_id, action, entity_type, entity_id, occurred_at,
    before_state, after_state, reason, request_id, metadata
)
SELECT
    md5('demo-audit-' || sequence)::uuid,
    md5('demo-audit-event-' || sequence)::uuid,
    CASE WHEN sequence IN (1, 2, 3, 28) THEN md5('demo-user-7')::uuid ELSE md5('demo-user-3')::uuid END,
    CASE
        WHEN sequence = 1 THEN 'DEMO_DATASET_LOADED'
        WHEN sequence BETWEEN 2 AND 8 THEN 'DEMO_INTERVENTION_REGISTERED'
        WHEN sequence BETWEEN 9 AND 11 THEN 'DEMO_CONFLICT_DETECTED'
        WHEN sequence BETWEEN 12 AND 16 THEN 'DEMO_DEPENDENCY_RECORDED'
        WHEN sequence BETWEEN 17 AND 22 THEN 'DEMO_APPROVAL_RECORDED'
        WHEN sequence BETWEEN 23 AND 24 THEN 'DEMO_EVIDENCE_REVIEWED'
        WHEN sequence = 25 THEN 'DEMO_INSPECTION_COMPLETED'
        WHEN sequence = 26 THEN 'DEMO_VERIFICATION_FAILED'
        WHEN sequence = 27 THEN 'DEMO_CITIZEN_DISPUTED'
        WHEN sequence = 28 THEN 'DEMO_ESCALATION_ACKNOWLEDGED'
        WHEN sequence = 29 THEN 'DEMO_AI_ADVISORY_REVIEWED'
        ELSE 'DEMO_COORDINATION_DECISION_RECORDED'
    END,
    CASE
        WHEN sequence = 1 THEN 'DEMO_DATASET'
        WHEN sequence BETWEEN 2 AND 8 THEN 'INTERVENTION'
        WHEN sequence BETWEEN 9 AND 11 THEN 'CONFLICT'
        WHEN sequence BETWEEN 12 AND 16 THEN 'DEPENDENCY'
        WHEN sequence BETWEEN 17 AND 22 THEN 'APPROVAL'
        WHEN sequence BETWEEN 23 AND 24 THEN 'EVIDENCE'
        WHEN sequence = 25 THEN 'INSPECTION'
        WHEN sequence = 26 THEN 'VERIFICATION'
        WHEN sequence = 27 THEN 'CITIZEN_OBSERVATION'
        WHEN sequence = 28 THEN 'ESCALATION'
        WHEN sequence = 29 THEN 'AI_RECOMMENDATION'
        ELSE 'COORDINATION_DECISION'
    END,
    CASE
        WHEN sequence = 1 THEN md5('demo-dataset')::uuid
        WHEN sequence BETWEEN 2 AND 8 THEN md5('demo-intervention-' || (sequence - 1))::uuid
        WHEN sequence BETWEEN 9 AND 11 THEN md5('demo-conflict-' || (sequence - 8))::uuid
        WHEN sequence BETWEEN 12 AND 16 THEN md5('demo-dependency-' || (sequence - 11))::uuid
        WHEN sequence BETWEEN 17 AND 22 THEN md5('demo-approval-' || (sequence - 16))::uuid
        WHEN sequence BETWEEN 23 AND 24 THEN md5('demo-evidence-' || (sequence - 22))::uuid
        WHEN sequence = 25 THEN md5('demo-inspection-1')::uuid
        WHEN sequence = 26 THEN md5('demo-verification-1')::uuid
        WHEN sequence = 27 THEN md5('demo-observation-3')::uuid
        WHEN sequence = 28 THEN md5('demo-escalation-1')::uuid
        WHEN sequence = 29 THEN md5('demo-recommendation-1')::uuid
        ELSE md5('demo-coordination-decision-1')::uuid
    END,
    TIMESTAMPTZ '2026-08-20 16:00:00+05:30' + (sequence || ' minutes')::interval,
    NULL,
    jsonb_build_object('dataset', 'DEMO / SYNTHETIC', 'sequence', sequence),
    '[DEMO / SYNTHETIC] Deterministic demonstration history; not an official government audit record.',
    md5('demo-request-' || sequence)::uuid,
    '{"dataset":"DEMO / SYNTHETIC","simulated":true,"officialRecord":false}'::jsonb
FROM generate_series(1, 30) AS sequence
ON CONFLICT DO NOTHING;
