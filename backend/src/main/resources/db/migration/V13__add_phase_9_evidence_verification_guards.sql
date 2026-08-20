ALTER TABLE evidence
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE evidence
    ADD CONSTRAINT ck_evidence_type CHECK (
        evidence_type IN (
            'BEFORE_WORK',
            'DURING_WORK',
            'COMPLETION',
            'RESTORATION',
            'INSPECTION',
            'CITIZEN_VALIDATION',
            'DOCUMENT'
        )
    ),
    ADD CONSTRAINT ck_evidence_checksum_format CHECK (
        checksum IS NULL OR checksum ~ '^[0-9a-f]{64}$'
    );

CREATE UNIQUE INDEX uq_inspections_open_intervention
    ON inspections (intervention_id)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');

CREATE OR REPLACE FUNCTION prevent_completed_inspection_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'COMPLETED' THEN
        RAISE EXCEPTION 'completed inspections are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER completed_inspections_are_immutable
    BEFORE UPDATE OR DELETE ON inspections
    FOR EACH ROW EXECUTE FUNCTION prevent_completed_inspection_mutation();

CREATE OR REPLACE FUNCTION prevent_verification_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'verifications are append-only';
END;
$$;

CREATE TRIGGER verifications_are_append_only
    BEFORE UPDATE OR DELETE ON verifications
    FOR EACH ROW EXECUTE FUNCTION prevent_verification_mutation();
