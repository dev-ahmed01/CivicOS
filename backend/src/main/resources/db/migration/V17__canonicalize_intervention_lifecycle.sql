ALTER TABLE interventions
    DROP CONSTRAINT ck_interventions_status;

UPDATE interventions
SET status = CASE status
    WHEN 'COMPLETED_PENDING_VERIFICATION' THEN 'VERIFICATION_PENDING'
    WHEN 'VERIFICATION_FAILED' THEN 'REOPENED'
    WHEN 'CORRECTIVE_ACTION' THEN 'REOPENED'
    ELSE status
END;

ALTER TABLE interventions
    ADD COLUMN held_from_status VARCHAR(60),
    ADD CONSTRAINT ck_interventions_status CHECK (
        status IN (
            'DRAFT',
            'SUBMITTED',
            'UNDER_REVIEW',
            'ANALYSIS',
            'COORDINATION_REQUIRED',
            'COORDINATION_COMPLETE',
            'APPROVAL_PENDING',
            'APPROVED',
            'SCHEDULED',
            'IN_PROGRESS',
            'RESTORATION',
            'EVIDENCE_PENDING',
            'VERIFICATION_PENDING',
            'VERIFIED',
            'CLOSED',
            'REJECTED',
            'CANCELLED',
            'ON_HOLD',
            'REOPENED'
        )
    ),
    ADD CONSTRAINT ck_interventions_held_from_status CHECK (
        (status = 'ON_HOLD' AND held_from_status IN (
            'COORDINATION_REQUIRED',
            'COORDINATION_COMPLETE',
            'APPROVED',
            'SCHEDULED',
            'IN_PROGRESS',
            'RESTORATION',
            'EVIDENCE_PENDING',
            'VERIFICATION_PENDING'
        ))
        OR (status <> 'ON_HOLD' AND held_from_status IS NULL)
    );

COMMENT ON COLUMN interventions.held_from_status IS
    'Canonical state restored by the deterministic RESUME transition; populated only while ON_HOLD.';
