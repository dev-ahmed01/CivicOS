CREATE TABLE api_idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(100) NOT NULL,
    operation VARCHAR(150) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_api_idempotency_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_api_idempotency_key CHECK (idempotency_key ~ '^[A-Za-z0-9._:-]{8,100}$'),
    CONSTRAINT ck_api_idempotency_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_api_idempotency_operation CHECK (NULLIF(BTRIM(operation), '') IS NOT NULL)
);

CREATE INDEX idx_api_idempotency_created_at ON api_idempotency_keys (created_at);

CREATE OR REPLACE FUNCTION prevent_api_idempotency_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'API idempotency records are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER api_idempotency_keys_are_append_only
BEFORE UPDATE OR DELETE ON api_idempotency_keys
FOR EACH ROW EXECUTE FUNCTION prevent_api_idempotency_mutation();
