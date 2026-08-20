ALTER TABLE notifications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type CHECK (
        notification_type IN (
            'INTERVENTION_SUBMITTED',
            'CONFLICT_DETECTED',
            'TASK_ASSIGNED',
            'APPROVAL_PENDING',
            'DEADLINE_APPROACHING',
            'SLA_BREACHED',
            'DEPENDENCY_BLOCKED',
            'WORK_STARTED',
            'RESTORATION_PENDING',
            'EVIDENCE_SUBMITTED',
            'VERIFICATION_REQUESTED',
            'VERIFICATION_FAILED',
            'CITIZEN_VALIDATION',
            'INTERVENTION_REOPENED'
        )
    ),
    ADD CONSTRAINT ck_notifications_content CHECK (
        LENGTH(TRIM(title)) > 0 AND LENGTH(TRIM(message)) > 0
    );

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_key VARCHAR(200) NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    target_type VARCHAR(60),
    target_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMPTZ,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification_outbox_event_key UNIQUE (event_key),
    CONSTRAINT ck_notification_outbox_type CHECK (
        notification_type IN (
            'INTERVENTION_SUBMITTED',
            'CONFLICT_DETECTED',
            'TASK_ASSIGNED',
            'APPROVAL_PENDING',
            'DEADLINE_APPROACHING',
            'SLA_BREACHED',
            'DEPENDENCY_BLOCKED',
            'WORK_STARTED',
            'RESTORATION_PENDING',
            'EVIDENCE_SUBMITTED',
            'VERIFICATION_REQUESTED',
            'VERIFICATION_FAILED',
            'CITIZEN_VALIDATION',
            'INTERVENTION_REOPENED'
        )
    ),
    CONSTRAINT ck_notification_outbox_status CHECK (
        status IN ('PENDING', 'RETRY_PENDING', 'DELIVERED', 'FAILED')
    ),
    CONSTRAINT ck_notification_outbox_target_pair CHECK (
        (target_type IS NULL AND target_id IS NULL)
        OR (target_type IS NOT NULL AND target_id IS NOT NULL)
    ),
    CONSTRAINT ck_notification_outbox_attempts CHECK (attempt_count >= 0),
    CONSTRAINT ck_notification_outbox_content CHECK (
        LENGTH(TRIM(event_key)) > 0
        AND LENGTH(TRIM(title)) > 0
        AND LENGTH(TRIM(message)) > 0
    ),
    CONSTRAINT ck_notification_outbox_delivery CHECK (
        (status = 'DELIVERED' AND delivered_at IS NOT NULL)
        OR (status <> 'DELIVERED' AND delivered_at IS NULL)
    ),
    CONSTRAINT ck_notification_outbox_error CHECK (
        status NOT IN ('RETRY_PENDING', 'FAILED') OR last_error IS NOT NULL
    )
);

CREATE INDEX idx_notification_outbox_due
    ON notification_outbox (status, next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY_PENDING');

ALTER TABLE audit_events
    ADD CONSTRAINT ck_audit_events_required_text CHECK (
        LENGTH(TRIM(action)) > 0 AND LENGTH(TRIM(entity_type)) > 0
    );
