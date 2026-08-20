CREATE TABLE roads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_reference VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    classification VARCHAR(50) NOT NULL,
    geometry geometry(MultiLineString, 4326) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_roads_external_reference UNIQUE (external_reference),
    CONSTRAINT ck_roads_classification CHECK (
        classification IN ('ARTERIAL', 'SUB_ARTERIAL', 'COLLECTOR', 'LOCAL', 'OTHER')
    ),
    CONSTRAINT ck_roads_geometry_valid CHECK (ST_IsValid(geometry))
);

CREATE TABLE road_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    road_id UUID REFERENCES roads(id) ON DELETE RESTRICT,
    external_reference VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    classification VARCHAR(50) NOT NULL,
    surface_type VARCHAR(50) NOT NULL,
    length_meters NUMERIC(12, 2) NOT NULL,
    geometry geometry(LineString, 4326) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_road_segments_external_reference UNIQUE (external_reference),
    CONSTRAINT ck_road_segments_classification CHECK (
        classification IN ('ARTERIAL', 'SUB_ARTERIAL', 'COLLECTOR', 'LOCAL', 'OTHER')
    ),
    CONSTRAINT ck_road_segments_surface_type CHECK (
        surface_type IN ('BITUMINOUS', 'CONCRETE', 'PAVER_BLOCK', 'GRAVEL', 'EARTH', 'OTHER')
    ),
    CONSTRAINT ck_road_segments_length CHECK (length_meters > 0),
    CONSTRAINT ck_road_segments_geometry_valid CHECK (ST_IsValid(geometry))
);

CREATE TABLE civic_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(50) NOT NULL,
    source VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    road_segment_id UUID NOT NULL REFERENCES road_segments(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_civic_cases_case_number UNIQUE (case_number),
    CONSTRAINT ck_civic_cases_source CHECK (source IN ('CITIZEN', 'AGENCY', 'SYSTEM', 'IMPORT')),
    CONSTRAINT ck_civic_cases_status CHECK (
        status IN ('OPEN', 'UNDER_REVIEW', 'IN_PROGRESS', 'PENDING_VERIFICATION', 'VERIFIED', 'CLOSED')
    ),
    CONSTRAINT ck_civic_cases_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_civic_cases_closed_at CHECK (
        (status = 'CLOSED' AND closed_at IS NOT NULL)
        OR (status <> 'CLOSED' AND closed_at IS NULL)
    )
);

CREATE TABLE citizen_observations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES civic_cases(id) ON DELETE RESTRICT,
    submitted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    category VARCHAR(60) NOT NULL,
    description TEXT NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    road_segment_id UUID NOT NULL REFERENCES road_segments(id) ON DELETE RESTRICT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
    ai_suggested_category VARCHAR(60),
    ai_confidence NUMERIC(5, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_citizen_observations_status CHECK (
        status IN ('SUBMITTED', 'TRIAGED', 'MATCHED', 'FLAGGED', 'FORWARDED', 'RESOLVED', 'DISMISSED', 'DUPLICATE')
    ),
    CONSTRAINT ck_citizen_observations_confidence CHECK (
        ai_confidence IS NULL OR (ai_confidence >= 0 AND ai_confidence <= 1)
    ),
    CONSTRAINT ck_citizen_observations_location_valid CHECK (ST_IsValid(location))
);
