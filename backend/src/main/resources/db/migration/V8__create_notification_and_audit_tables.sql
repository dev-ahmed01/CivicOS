CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_type VARCHAR(60) NOT NULL,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    target_type VARCHAR(60),
    target_id UUID,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notifications_target_pair CHECK (
        (target_type IS NULL AND target_id IS NULL)
        OR (target_type IS NOT NULL AND target_id IS NOT NULL)
    )
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_state JSONB,
    after_state JSONB,
    reason TEXT,
    request_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    CONSTRAINT uq_audit_events_event_id UNIQUE (event_id)
);
