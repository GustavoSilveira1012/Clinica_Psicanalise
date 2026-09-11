CREATE TABLE patient_package_cancellations (

    id UUID PRIMARY KEY,

    patient_package_id UUID NOT NULL,

    settlement_mode VARCHAR(30) NOT NULL,

    reason VARCHAR(60) NOT NULL,

    status VARCHAR(30) NOT NULL,

    calculation_version VARCHAR(50) NOT NULL,

    original_package_amount NUMERIC(19,2) NOT NULL,

    consumed_value NUMERIC(19,2) NOT NULL,

    remaining_service_value NUMERIC(19,2) NOT NULL,

    paid_amount_before NUMERIC(19,2) NOT NULL,

    receivable_adjustment_amount NUMERIC(19,2) NOT NULL,

    settlement_amount NUMERIC(19,2) NOT NULL,

    outstanding_consumed_amount NUMERIC(19,2) NOT NULL,

    requested_by_user_id BIGINT NOT NULL,

    requested_at TIMESTAMPTZ NOT NULL,

    completed_at TIMESTAMPTZ,

    failed_at TIMESTAMPTZ,

    failure_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_package_cancellation_package
        FOREIGN KEY (patient_package_id)
        REFERENCES patient_packages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_package_cancellation_user
        FOREIGN KEY (requested_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_package_cancellation_settlement
        CHECK (
            settlement_mode IN (
                'NONE',
                'REFUND',
                'CREDIT_BALANCE'
            )
        ),

    CONSTRAINT chk_package_cancellation_status
        CHECK (
            status IN (
                'CALCULATED',
                'SETTLING',
                'COMPLETED',
                'FAILED'
            )
        )
);

----------------------------------------

CREATE UNIQUE INDEX ux_patient_package_active_cancellation

ON patient_package_cancellations(
    patient_package_id
)

WHERE status IN (
    'CALCULATED',
    'SETTLING'
);

-------------------------------------

ALTER TABLE credit_entries
ADD COLUMN patient_package_cancellation_id UUID;


ALTER TABLE credit_entries
ADD CONSTRAINT fk_credit_entry_package_cancellation

FOREIGN KEY (
    patient_package_cancellation_id
)

REFERENCES patient_package_cancellations(id)

ON DELETE RESTRICT;

----------------------------------------------------

ALTER TABLE refunds
ADD COLUMN patient_package_cancellation_id UUID;


ALTER TABLE refunds
ADD CONSTRAINT fk_refund_package_cancellation

FOREIGN KEY (
    patient_package_cancellation_id
)

REFERENCES patient_package_cancellations(id)

ON DELETE RESTRICT;

----------------------------

ALTER TABLE patient_package_cancellations

ADD COLUMN idempotency_key
VARCHAR(100);


ALTER TABLE patient_package_cancellations

ADD COLUMN request_fingerprint
VARCHAR(64);


CREATE UNIQUE INDEX ux_package_cancellation_idempotency

ON patient_package_cancellations(
    patient_package_id,
    idempotency_key
)

WHERE idempotency_key IS NOT NULL;