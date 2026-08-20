CREATE TABLE conflicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conflict_number VARCHAR(50) NOT NULL,
    road_segment_id UUID NOT NULL REFERENCES road_segments(id) ON DELETE RESTRICT,
    conflict_type VARCHAR(70) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    explanation TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_conflicts_number UNIQUE (conflict_number),
    CONSTRAINT ck_conflicts_type CHECK (
        conflict_type IN (
            'SAME_ROAD_OVERLAP',
            'SPATIAL_OVERLAP',
            'TEMPORAL_OVERLAP',
            'UNSAFE_SEQUENCING',
            'REPEAT_DIGGING_RISK',
            'RESTORATION_BEFORE_EXCAVATION_COMPLETION',
            'MULTI_AGENCY_COORDINATION'
        )
    ),
    CONSTRAINT ck_conflicts_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_conflicts_status CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT ck_conflicts_resolution_time CHECK (
        (status IN ('RESOLVED', 'DISMISSED') AND resolved_at IS NOT NULL)
        OR (status IN ('OPEN', 'UNDER_REVIEW') AND resolved_at IS NULL)
    )
);
CREATE TABLE conflict_interventions (
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE CASCADE,
    intervention_id UUID NOT NULL REFERENCES interventions(id) ON DELETE RESTRICT,
    PRIMARY KEY (conflict_id, intervention_id)
);

CREATE TABLE ai_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation VARCHAR(60) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(150) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    schema_version VARCHAR(50) NOT NULL,
    input_reference JSONB NOT NULL DEFAULT '{}'::JSONB,
    output JSONB,
    confidence NUMERIC(5, 4),
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_ai_runs_confidence CHECK (
        confidence IS NULL OR (confidence >= 0 AND confidence <= 1)
    ),
    CONSTRAINT ck_ai_runs_status CHECK (status IN ('REQUESTED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ai_run_id UUID NOT NULL REFERENCES ai_runs(id) ON DELETE RESTRICT,
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE RESTRICT,
    recommendation_type VARCHAR(60) NOT NULL,
    recommendation JSONB NOT NULL,
    confidence NUMERIC(5, 4),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ai_recommendations_confidence CHECK (
        confidence IS NULL OR (confidence >= 0 AND confidence <= 1)
    ),
    CONSTRAINT ck_ai_recommendations_status CHECK (
        status IN ('PENDING_REVIEW', 'ACCEPTED', 'REJECTED', 'SUPERSEDED')
    ),
    CONSTRAINT ck_ai_recommendations_review CHECK (
        (status = 'PENDING_REVIEW' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR (status <> 'PENDING_REVIEW' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);

CREATE TABLE coordination_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE RESTRICT,
    coordinator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decision_type VARCHAR(40) NOT NULL,
    decision_text TEXT NOT NULL,
    accepted_recommendation_id UUID REFERENCES ai_recommendations(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_coordination_decisions_type CHECK (
        decision_type IN ('ACCEPT', 'ACCEPT_WITH_MODIFICATION', 'REJECT', 'REQUEST_INFORMATION')
    )
);
