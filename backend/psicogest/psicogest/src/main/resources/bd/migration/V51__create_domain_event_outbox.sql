CREATE TABLE domain_event_outbox (

    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id VARCHAR(100) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    deduplication_key VARCHAR(255) NOT NULL,

    payload JSONB NOT NULL
        DEFAULT '{}'::jsonb,

    status VARCHAR(30) NOT NULL,

    attempt_count INTEGER NOT NULL
        DEFAULT 0,

    occurred_at TIMESTAMPTZ NOT NULL,

    processing_started_at TIMESTAMPTZ,

    processed_at TIMESTAMPTZ,

    next_retry_at TIMESTAMPTZ,

    last_error_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ux_domain_event_dedupe
        UNIQUE (
            deduplication_key
        ),

    CONSTRAINT chk_domain_event_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PROCESSED',
                'FAILED',
                'DEAD_LETTER'
            )
        )
);

---------------------------------------------

CREATE UNIQUE INDEX ux_package_activation_credit

ON session_credit_entries(
    patient_package_id,
    patient_package_item_id
)

WHERE entry_type = 'PACKAGE_ACTIVATION';

---------------------------------------------

ALTER TABLE package_plan_versions

ADD COLUMN cancellation_pricing_policy
VARCHAR(30) NOT NULL
DEFAULT 'PRO_RATA';


ALTER TABLE package_plan_versions

ADD CONSTRAINT chk_package_cancellation_pricing_policy
CHECK (
    cancellation_pricing_policy IN (
        'PRO_RATA',
        'MANUAL_REVIEW'
    )
);