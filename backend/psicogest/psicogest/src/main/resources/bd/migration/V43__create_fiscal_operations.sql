CREATE TABLE fiscal_operations (

    id UUID PRIMARY KEY,

    invoice_id UUID NOT NULL,

    operation_type VARCHAR(30) NOT NULL,

    status VARCHAR(40) NOT NULL,

    idempotency_key VARCHAR(100) NOT NULL,

    request_fingerprint VARCHAR(64) NOT NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    next_retry_at TIMESTAMPTZ,

    processing_started_at TIMESTAMPTZ,

    completed_at TIMESTAMPTZ,

    last_error_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_fiscal_operation_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_fiscal_operation_idempotency
        UNIQUE (
            invoice_id,
            operation_type,
            idempotency_key
        ),

    CONSTRAINT chk_fiscal_operation_type
        CHECK (
            operation_type IN (
                'ISSUE',
                'QUERY',
                'CANCEL',
                'SUBSTITUTE'
            )
        ),

    CONSTRAINT chk_fiscal_operation_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SUCCEEDED',
                'REJECTED',
                'RETRYABLE_FAILURE',
                'RECONCILIATION_REQUIRED',
                'DEAD_LETTER'
            )
        )
);