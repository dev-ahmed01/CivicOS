ALTER TABLE ai_runs
    ADD COLUMN requested_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    ADD COLUMN entity_type VARCHAR(100),
    ADD COLUMN entity_id UUID,
    ADD COLUMN model_version VARCHAR(100),
    ADD COLUMN input_hash VARCHAR(64),
    ADD COLUMN configuration JSONB NOT NULL DEFAULT '{}'::JSONB,
    ADD COLUMN started_at TIMESTAMPTZ,
    ADD COLUMN input_tokens BIGINT,
    ADD COLUMN output_tokens BIGINT,
    ADD COLUMN estimated_cost NUMERIC(19, 6),
    ADD COLUMN latency_ms BIGINT,
    ADD COLUMN review_required BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE ai_runs
SET model_version = model,
    input_hash = encode(digest(input_reference::TEXT, 'sha256'), 'hex'),
    entity_type = NULLIF(input_reference ->> 'entityType', ''),
    entity_id = CASE
        WHEN (input_reference ->> 'entityId') ~
             '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$'
        THEN (input_reference ->> 'entityId')::UUID
        ELSE NULL
    END,
    started_at = CASE
        WHEN status IN ('RUNNING', 'COMPLETED', 'FAILED') THEN created_at
        ELSE NULL
    END,
    review_required = TRUE;

ALTER TABLE ai_runs
    ALTER COLUMN requested_by SET NOT NULL,
    ALTER COLUMN entity_type SET NOT NULL,
    ALTER COLUMN entity_id SET NOT NULL,
    ALTER COLUMN model_version SET NOT NULL,
    ALTER COLUMN input_hash SET NOT NULL,
    ADD CONSTRAINT ck_ai_runs_operation CHECK (
        operation IN (
            'CLASSIFICATION',
            'MATCHING_ASSISTANCE',
            'EVIDENCE_ANALYSIS',
            'CONFLICT_EXPLANATION',
            'RECOMMENDATION_GENERATION',
            'CASE_SUMMARIZATION'
        )
    ),
    ADD CONSTRAINT ck_ai_runs_input_hash CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT ck_ai_runs_usage CHECK (
        (input_tokens IS NULL OR input_tokens >= 0)
        AND (output_tokens IS NULL OR output_tokens >= 0)
        AND (estimated_cost IS NULL OR estimated_cost >= 0)
        AND (latency_ms IS NULL OR latency_ms >= 0)
    ),
    ADD CONSTRAINT ck_ai_runs_lifecycle CHECK (
        (status = 'REQUESTED'
            AND started_at IS NULL AND completed_at IS NULL
            AND output IS NULL AND confidence IS NULL AND error_message IS NULL)
        OR (status = 'RUNNING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND output IS NULL AND confidence IS NULL AND error_message IS NULL)
        OR (status = 'COMPLETED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND output IS NOT NULL AND confidence IS NOT NULL AND error_message IS NULL)
        OR (status = 'FAILED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND output IS NULL AND confidence IS NULL
            AND NULLIF(BTRIM(error_message), '') IS NOT NULL)
        OR status = 'CANCELLED'
    );

ALTER TABLE ai_recommendations
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ALTER COLUMN confidence SET NOT NULL,
    ADD CONSTRAINT uq_ai_recommendations_run UNIQUE (ai_run_id),
    ADD CONSTRAINT ck_ai_recommendations_rejection_reason CHECK (
        status <> 'REJECTED' OR NULLIF(BTRIM(review_reason), '') IS NOT NULL
    );

CREATE INDEX idx_ai_runs_requester_created_at
    ON ai_runs (requested_by, created_at DESC);
CREATE INDEX idx_ai_runs_entity_created_at
    ON ai_runs (entity_type, entity_id, created_at DESC);

CREATE OR REPLACE FUNCTION prevent_ai_run_input_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.operation IS DISTINCT FROM OLD.operation
       OR NEW.requested_by IS DISTINCT FROM OLD.requested_by
       OR NEW.entity_type IS DISTINCT FROM OLD.entity_type
       OR NEW.entity_id IS DISTINCT FROM OLD.entity_id
       OR NEW.provider IS DISTINCT FROM OLD.provider
       OR NEW.model IS DISTINCT FROM OLD.model
       OR NEW.model_version IS DISTINCT FROM OLD.model_version
       OR NEW.prompt_version IS DISTINCT FROM OLD.prompt_version
       OR NEW.schema_version IS DISTINCT FROM OLD.schema_version
       OR NEW.input_hash IS DISTINCT FROM OLD.input_hash
       OR NEW.configuration IS DISTINCT FROM OLD.configuration
       OR NEW.input_reference IS DISTINCT FROM OLD.input_reference
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'AI run identity and input metadata are immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ai_run_input_is_immutable
BEFORE UPDATE ON ai_runs
FOR EACH ROW EXECUTE FUNCTION prevent_ai_run_input_mutation();

CREATE OR REPLACE FUNCTION prevent_ai_recommendation_payload_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.ai_run_id IS DISTINCT FROM OLD.ai_run_id
       OR NEW.conflict_id IS DISTINCT FROM OLD.conflict_id
       OR NEW.recommendation_type IS DISTINCT FROM OLD.recommendation_type
       OR NEW.recommendation IS DISTINCT FROM OLD.recommendation
       OR NEW.confidence IS DISTINCT FROM OLD.confidence
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'AI recommendation source and payload are immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ai_recommendation_payload_is_immutable
BEFORE UPDATE ON ai_recommendations
FOR EACH ROW EXECUTE FUNCTION prevent_ai_recommendation_payload_mutation();
