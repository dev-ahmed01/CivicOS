CREATE INDEX idx_roads_geometry ON roads USING GIST (geometry);
CREATE INDEX idx_road_segments_geometry ON road_segments USING GIST (geometry);
CREATE INDEX idx_citizen_observations_location ON citizen_observations USING GIST (location);
CREATE INDEX idx_interventions_geometry ON interventions USING GIST (geometry);
CREATE INDEX idx_evidence_captured_location ON evidence USING GIST (captured_location);

CREATE INDEX idx_civic_cases_status ON civic_cases (status);
CREATE INDEX idx_civic_cases_road_segment ON civic_cases (road_segment_id);
CREATE INDEX idx_citizen_observations_case ON citizen_observations (case_id);
CREATE INDEX idx_citizen_observations_road_segment ON citizen_observations (road_segment_id);

CREATE INDEX idx_interventions_road_segment ON interventions (road_segment_id);
CREATE INDEX idx_interventions_agency ON interventions (agency_id);
CREATE INDEX idx_interventions_planned_range ON interventions (planned_start, planned_end);
CREATE INDEX idx_interventions_status ON interventions (status);

CREATE INDEX idx_dependencies_source ON dependencies (source_intervention_id);
CREATE INDEX idx_dependencies_target ON dependencies (target_intervention_id);

CREATE INDEX idx_conflicts_road_segment ON conflicts (road_segment_id);
CREATE INDEX idx_conflicts_status ON conflicts (status);
CREATE INDEX idx_conflicts_severity_status ON conflicts (severity, status);
CREATE INDEX idx_conflict_interventions_intervention ON conflict_interventions (intervention_id);
CREATE INDEX idx_coordination_decisions_conflict ON coordination_decisions (conflict_id);

CREATE INDEX idx_approvals_intervention_status ON approvals (intervention_id, status);
CREATE UNIQUE INDEX uq_approvals_pending_intervention
    ON approvals (intervention_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_sla_deadline_status ON sla_instances (deadline, status);
CREATE INDEX idx_sla_target ON sla_instances (target_type, target_id);
CREATE INDEX idx_escalations_sla_status ON escalations (sla_id, status);

CREATE INDEX idx_evidence_target ON evidence (target_type, target_id);
CREATE INDEX idx_evidence_uploaded_by ON evidence (uploaded_by);
CREATE INDEX idx_inspections_intervention_status ON inspections (intervention_id, status);
CREATE INDEX idx_verifications_target ON verifications (target_type, target_id);

CREATE INDEX idx_notifications_recipient_read_at ON notifications (recipient_id, read_at);
CREATE INDEX idx_audit_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_events (actor_id);
CREATE INDEX idx_audit_occurred_at ON audit_events (occurred_at);
CREATE INDEX idx_ai_runs_status_created_at ON ai_runs (status, created_at);
CREATE INDEX idx_ai_recommendations_conflict ON ai_recommendations (conflict_id);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER agencies_set_updated_at
    BEFORE UPDATE ON agencies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER roads_set_updated_at
    BEFORE UPDATE ON roads
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER road_segments_set_updated_at
    BEFORE UPDATE ON road_segments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER civic_cases_set_updated_at
    BEFORE UPDATE ON civic_cases
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER citizen_observations_set_updated_at
    BEFORE UPDATE ON citizen_observations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER interventions_set_updated_at
    BEFORE UPDATE ON interventions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION prevent_audit_event_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_events are append-only';
END;
$$;

CREATE TRIGGER audit_events_are_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_event_mutation();
