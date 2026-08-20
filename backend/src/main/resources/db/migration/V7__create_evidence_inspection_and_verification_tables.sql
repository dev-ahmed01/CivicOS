CREATE TABLE evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type VARCHAR(60) NOT NULL,
    target_id UUID NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    evidence_type VARCHAR(60) NOT NULL,
    file_reference VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    mime_type VARCHAR(100),
    file_size_bytes BIGINT,
    checksum VARCHAR(128),
    captured_at TIMESTAMPTZ,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    captured_location geometry(Point, 4326),
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_evidence_file_reference UNIQUE (file_reference),
    CONSTRAINT ck_evidence_file_size CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    CONSTRAINT ck_evidence_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_evidence_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_evidence_location_pair CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    CONSTRAINT ck_evidence_status CHECK (
        status IN ('UPLOADED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'SUPERSEDED')
    ),
    CONSTRAINT ck_evidence_location_valid CHECK (
        captured_location IS NULL OR ST_IsValid(captured_location)
    ),
    CONSTRAINT ck_evidence_accepted_checksum CHECK (
        status <> 'ACCEPTED' OR (checksum IS NOT NULL AND LENGTH(TRIM(checksum)) > 0)
    )
);
CREATE TABLE inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    intervention_id UUID NOT NULL REFERENCES interventions(id) ON DELETE RESTRICT,
    inspector_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    result VARCHAR(30),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inspections_status CHECK (
        status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT ck_inspections_result CHECK (
        result IS NULL OR result IN ('PASSED', 'FAILED', 'CONDITIONAL')
    ),
    CONSTRAINT ck_inspections_completion CHECK (
        (status = 'COMPLETED' AND result IS NOT NULL AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED' AND completed_at IS NULL)
    ),
    CONSTRAINT ck_inspections_times CHECK (
        completed_at IS NULL OR (started_at IS NOT NULL AND completed_at >= started_at)
    )
);

CREATE TABLE verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type VARCHAR(60) NOT NULL,
    target_id UUID NOT NULL,
    source VARCHAR(30) NOT NULL,
    result VARCHAR(30) NOT NULL,
    submitted_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_verifications_source CHECK (source IN ('FIELD_INSPECTOR', 'CITIZEN', 'SYSTEM')),
    CONSTRAINT ck_verifications_result CHECK (
        result IN ('PASSED', 'FAILED', 'CONDITIONAL', 'CONFIRMED', 'DISPUTED', 'CANNOT_VERIFY')
    ),
    CONSTRAINT ck_verifications_actor CHECK (
        source = 'SYSTEM' OR submitted_by IS NOT NULL
    )
);
