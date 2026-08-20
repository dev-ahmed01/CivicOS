CREATE TABLE interventions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    intervention_number VARCHAR(50) NOT NULL,
    case_id UUID NOT NULL REFERENCES civic_cases(id) ON DELETE RESTRICT,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE RESTRICT,
    intervention_type VARCHAR(60) NOT NULL,
    description TEXT NOT NULL,
    road_segment_id UUID NOT NULL REFERENCES road_segments(id) ON DELETE RESTRICT,
    geometry geometry(Geometry, 4326) NOT NULL,
    planned_start TIMESTAMPTZ NOT NULL,
    planned_end TIMESTAMPTZ NOT NULL,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ,
    status VARCHAR(60) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_interventions_number UNIQUE (intervention_number),
    CONSTRAINT ck_interventions_planned_dates CHECK (planned_end > planned_start),
    CONSTRAINT ck_interventions_actual_dates CHECK (
        actual_end IS NULL OR (actual_start IS NOT NULL AND actual_end >= actual_start)
    ),
    CONSTRAINT ck_interventions_status CHECK (
        status IN (
            'DRAFT',
            'SUBMITTED',
            'UNDER_REVIEW',
            'COORDINATION_REQUIRED',
            'APPROVED',
            'SCHEDULED',
            'IN_PROGRESS',
            'COMPLETED_PENDING_VERIFICATION',
            'VERIFICATION_FAILED',
            'CORRECTIVE_ACTION',
            'VERIFIED',
            'CLOSED'
        )
    ),
    CONSTRAINT ck_interventions_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_interventions_geometry_valid CHECK (ST_IsValid(geometry))
);

CREATE TABLE dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_intervention_id UUID NOT NULL REFERENCES interventions(id) ON DELETE RESTRICT,
    target_intervention_id UUID NOT NULL REFERENCES interventions(id) ON DELETE RESTRICT,
    dependency_type VARCHAR(60) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dependencies_relationship UNIQUE (
        source_intervention_id,
        target_intervention_id,
        dependency_type
    ),
    CONSTRAINT ck_dependencies_distinct_interventions CHECK (
        source_intervention_id <> target_intervention_id
    ),
    CONSTRAINT ck_dependencies_type CHECK (
        dependency_type IN ('MUST_COMPLETE_BEFORE', 'MUST_VERIFY_BEFORE', 'RESTORATION_DEPENDS_ON')
    ),
    CONSTRAINT ck_dependencies_status CHECK (status IN ('ACTIVE', 'SATISFIED', 'BLOCKED', 'CANCELLED'))
);
