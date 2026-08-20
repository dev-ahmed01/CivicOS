CREATE TABLE approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    intervention_id UUID NOT NULL REFERENCES interventions(id) ON DELETE RESTRICT,
    actor_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    decision VARCHAR(40),
    reason TEXT,
    conditions JSONB NOT NULL DEFAULT '[]'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_approvals_status CHECK (
        status IN ('PENDING', 'APPROVED', 'APPROVED_WITH_CONDITIONS', 'REJECTED', 'RETURNED')
    ),
    CONSTRAINT ck_approvals_decision CHECK (
        decision IS NULL OR decision IN ('APPROVE', 'APPROVE_WITH_CONDITIONS', 'REJECT', 'RETURN')
    ),
    CONSTRAINT ck_approvals_decision_state CHECK (
        (status = 'PENDING' AND decision IS NULL AND decided_at IS NULL)
        OR (status <> 'PENDING' AND decision IS NOT NULL AND decided_at IS NOT NULL AND actor_id IS NOT NULL)
    ),
    CONSTRAINT ck_approvals_rejection_reason CHECK (
        status <> 'REJECTED' OR (reason IS NOT NULL AND LENGTH(TRIM(reason)) > 0)
    )
);
CREATE TABLE sla_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type VARCHAR(60) NOT NULL,
    target_id UUID NOT NULL,
    sla_type VARCHAR(60) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    paused_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_sla_instances_deadline CHECK (deadline > start_at),
    CONSTRAINT ck_sla_instances_status CHECK (
        status IN ('NORMAL', 'AT_RISK', 'BREACHED', 'PAUSED', 'COMPLETED')
    ),
    CONSTRAINT ck_sla_instances_pause_state CHECK (
        (status = 'PAUSED' AND paused_at IS NOT NULL)
        OR (status <> 'PAUSED' AND paused_at IS NULL)
    ),
    CONSTRAINT ck_sla_instances_completion_state CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED' AND completed_at IS NULL)
    )
);

CREATE TABLE escalations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sla_id UUID NOT NULL REFERENCES sla_instances(id) ON DELETE RESTRICT,
    level INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reason TEXT NOT NULL,
    escalated_to UUID REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT ck_escalations_level CHECK (level > 0),
    CONSTRAINT ck_escalations_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT ck_escalations_resolution_state CHECK (
        (status = 'RESOLVED' AND resolved_at IS NOT NULL)
        OR (status <> 'RESOLVED' AND resolved_at IS NULL)
    )
);
