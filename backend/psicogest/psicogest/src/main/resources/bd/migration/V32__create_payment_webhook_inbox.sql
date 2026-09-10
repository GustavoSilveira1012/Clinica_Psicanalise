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

    CONSTRAINT ux_webhook_provider_event
        UNIQUE (
            provider,
            provider_event_id
        ),

    CONSTRAINT chk_webhook_inbox_status
        CHECK (
            status IN (
                'RECEIVED',
                'PROCESSING',
                'PROCESSED',
                'FAILED',
                'DEAD_LETTER'
            )
        ),

    CONSTRAINT chk_webhook_attempt_count
        CHECK (
            attempt_count >= 0
        )
);


CREATE INDEX idx_payment_webhook_processing
ON payment_webhook_inbox(
    status,
    next_retry_at,
    received_at
);

-----------------------------------------------------------------

CREATE TABLE financial_entities (

    id UUID PRIMARY KEY,

    entity_type VARCHAR(30) NOT NULL,

    clinic_id BIGINT,

    psychoanalyst_id BIGINT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_financial_entity_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_financial_entity_psychoanalyst
        FOREIGN KEY (psychoanalyst_id)
        REFERENCES psychoanalysts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_financial_entity_type
        CHECK (
            entity_type IN (
                'CLINIC',
                'PSYCHOANALYST'
            )
        ),

    CONSTRAINT chk_financial_entity_owner
        CHECK (
            (
                entity_type = 'CLINIC'
                AND clinic_id IS NOT NULL
                AND psychoanalyst_id IS NULL
            )
            OR
            (
                entity_type = 'PSYCHOANALYST'
                AND psychoanalyst_id IS NOT NULL
                AND clinic_id IS NULL
            )
        )
);


CREATE UNIQUE INDEX ux_financial_entity_clinic
ON financial_entities(clinic_id)
WHERE clinic_id IS NOT NULL;


CREATE UNIQUE INDEX ux_financial_entity_psychoanalyst
ON financial_entities(psychoanalyst_id)
WHERE psychoanalyst_id IS NOT NULL;

---------------------------------------------------

ALTER TABLE receivables
ADD COLUMN financial_entity_id UUID;

ALTER TABLE payments
ADD COLUMN financial_entity_id UUID;

ALTER TABLE credit_accounts
ADD COLUMN financial_entity_id UUID;

-------------------------------------------------------

CREATE TABLE bank_accounts (

    id UUID PRIMARY KEY,

    financial_entity_id UUID NOT NULL,

    bank_code VARCHAR(20) NOT NULL,

    account_type VARCHAR(30) NOT NULL,

    branch VARCHAR(30),

    account_number_last4 VARCHAR(4),

    account_reference_hash VARCHAR(64) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_bank_account_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_bank_account_type
        CHECK (
            account_type IN (
                'CHECKING',
                'SAVINGS',
                'PAYMENT'
            )
        )
);


CREATE UNIQUE INDEX ux_bank_account_identity
ON bank_accounts(
    financial_entity_id,
    account_reference_hash
);

--------------------------------------------------------

CREATE TABLE bank_statement_imports (

    id UUID PRIMARY KEY,

    bank_account_id UUID NOT NULL,

    source VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    source_sha256 VARCHAR(64) NOT NULL,

    transaction_count INTEGER NOT NULL DEFAULT 0,

    imported_by_user_id BIGINT,

    imported_at TIMESTAMPTZ,

    failed_at TIMESTAMPTZ,

    error_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_statement_bank_account
        FOREIGN KEY (bank_account_id)
        REFERENCES bank_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_statement_import_user
        FOREIGN KEY (imported_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_statement_source_hash
        UNIQUE (
            bank_account_id,
            source_sha256
        ),

    CONSTRAINT chk_statement_source
        CHECK (
            source IN (
                'OFX',
                'BANK_API'
            )
        ),

    CONSTRAINT chk_statement_status
        CHECK (
            status IN (
                'PROCESSING',
                'IMPORTED',
                'FAILED'
            )
        )
);

---------------------------------------------------------

CREATE TABLE bank_transactions (

    id UUID PRIMARY KEY,

    bank_account_id UUID NOT NULL,

    statement_import_id UUID,

    external_transaction_id VARCHAR(255),

    transaction_fingerprint VARCHAR(64) NOT NULL,

    direction VARCHAR(10) NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    booking_date DATE NOT NULL,

    posted_at TIMESTAMPTZ,

    reconciliation_status VARCHAR(30) NOT NULL,

    encrypted_description BYTEA,
    description_iv BYTEA,
    encrypted_description_dek BYTEA,
    description_key_id VARCHAR(255),
    description_crypto_version INTEGER,
    description_crypto_algorithm VARCHAR(30),

    ignored_at TIMESTAMPTZ,

    ignored_by_user_id BIGINT,

    ignore_reason VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_bank_transaction_account
        FOREIGN KEY (bank_account_id)
        REFERENCES bank_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bank_transaction_import
        FOREIGN KEY (statement_import_id)
        REFERENCES bank_statement_imports(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bank_transaction_ignored_user
        FOREIGN KEY (ignored_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_bank_transaction_amount
        CHECK (amount > 0),

    CONSTRAINT chk_bank_transaction_currency
        CHECK (currency = 'BRL'),

    CONSTRAINT chk_bank_transaction_direction
        CHECK (
            direction IN (
                'CREDIT',
                'DEBIT'
            )
        ),

    CONSTRAINT chk_bank_reconciliation_status
        CHECK (
            reconciliation_status IN (
                'UNRECONCILED',
                'PARTIALLY_RECONCILED',
                'RECONCILED',
                'IGNORED'
            )
        )
);

-------------------------------------------------------

CREATE INDEX idx_bank_transaction_account_date
ON bank_transactions(
    bank_account_id,
    booking_date DESC
);


CREATE INDEX idx_bank_transaction_unreconciled
ON bank_transactions(
    bank_account_id,
    reconciliation_status,
    booking_date
);

---------------------------------------------------------

CREATE TABLE bank_reconciliation_allocations (

    id UUID PRIMARY KEY,

    bank_transaction_id UUID NOT NULL,

    payment_id UUID,

    refund_id UUID,

    amount NUMERIC(19,2) NOT NULL,

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_bank_reconciliation_transaction
        FOREIGN KEY (bank_transaction_id)
        REFERENCES bank_transactions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bank_reconciliation_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bank_reconciliation_refund
        FOREIGN KEY (refund_id)
        REFERENCES refunds(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bank_reconciliation_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_bank_reconciliation_amount
        CHECK (amount > 0),

    CONSTRAINT chk_bank_reconciliation_target
        CHECK (
            (
                payment_id IS NOT NULL
                AND refund_id IS NULL
            )
            OR
            (
                payment_id IS NULL
                AND refund_id IS NOT NULL
            )
        )
);

-----------------------------------------------------

CREATE OR REPLACE FUNCTION prevent_bank_reconciliation_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'bank reconciliation allocations are append-only';

END;
$$;


CREATE TRIGGER trg_bank_reconciliation_update
BEFORE UPDATE
ON bank_reconciliation_allocations
FOR EACH ROW
EXECUTE FUNCTION prevent_bank_reconciliation_mutation();


CREATE TRIGGER trg_bank_reconciliation_delete
BEFORE DELETE
ON bank_reconciliation_allocations
FOR EACH ROW
EXECUTE FUNCTION prevent_bank_reconciliation_mutation();