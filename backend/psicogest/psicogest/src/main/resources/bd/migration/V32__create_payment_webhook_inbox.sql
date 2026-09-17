CREATE TABLE payment_webhook_inbox (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(60),
    payload_sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMPTZ NOT NULL,
    processing_started_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_webhook_provider_event UNIQUE (provider, provider_event_id),
    CONSTRAINT chk_webhook_inbox_status CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD_LETTER')),
    CONSTRAINT chk_webhook_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_payment_webhook_processing
    ON payment_webhook_inbox(status, next_retry_at, received_at);

CREATE TABLE financial_entities (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL,
    clinic_id BIGINT,
    psychoanalyst_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_financial_entity_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE RESTRICT,
    CONSTRAINT fk_financial_entity_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_financial_entity_type CHECK (entity_type IN ('CLINIC', 'PSYCHOANALYST')),
    CONSTRAINT chk_financial_entity_owner CHECK (
        (entity_type = 'CLINIC' AND clinic_id IS NOT NULL AND psychoanalyst_id IS NULL)
        OR (entity_type = 'PSYCHOANALYST' AND psychoanalyst_id IS NOT NULL AND clinic_id IS NULL)
    )
);

CREATE UNIQUE INDEX ux_financial_entity_clinic
    ON financial_entities(clinic_id) WHERE clinic_id IS NOT NULL;
CREATE UNIQUE INDEX ux_financial_entity_psychoanalyst
    ON financial_entities(psychoanalyst_id) WHERE psychoanalyst_id IS NOT NULL;

ALTER TABLE receivables ADD COLUMN financial_entity_id UUID;
ALTER TABLE payments ADD COLUMN financial_entity_id UUID;
ALTER TABLE credit_accounts ADD COLUMN financial_entity_id UUID;

-- Bank account, transaction and reconciliation tables are created by the
-- canonical V34-V36 migrations. Keeping them here would create incompatible
-- duplicate definitions before those migrations run.
