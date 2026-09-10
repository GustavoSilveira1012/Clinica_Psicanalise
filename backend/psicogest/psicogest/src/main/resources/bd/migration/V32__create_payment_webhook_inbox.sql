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