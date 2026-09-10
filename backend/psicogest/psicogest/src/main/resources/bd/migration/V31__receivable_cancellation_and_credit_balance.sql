ALTER TABLE receivables DROP CONSTRAINT chk_receivable_status;

ALTER TABLE receivables
ADD CONSTRAINT chk_receivable_status CHECK (
    status IN (
        'OPEN',
        'PARTIALLY_PAID',
        'PAID',
        'CANCELLATION_PENDING',
        'CANCELLED'
    )
);

------------------------------------------------------------

CREATE TABLE receivable_cancellations (
    id UUID PRIMARY KEY,
    receivable_id UUID NOT NULL UNIQUE,
    requested_by_user_id BIGINT NOT NULL,
    mode VARCHAR(30) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    previous_receivable_status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_receivable_cancellation_receivable FOREIGN KEY (receivable_id) REFERENCES receivables (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receivable_cancellation_user FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_receivable_cancellation_mode CHECK (
        mode IN (
            'NONE',
            'REFUND',
            'CREDIT_BALANCE'
        )
    ),
    CONSTRAINT chk_receivable_cancellation_status CHECK (
        status IN (
            'SETTLING',
            'COMPLETED',
            'FAILED'
        )
    )
);

-------------------------------------------------------------

CREATE TABLE credit_accounts (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    clinic_id BIGINT,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_credit_account_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_credit_account_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id) ON DELETE RESTRICT,
    CONSTRAINT chk_credit_account_currency CHECK (currency = 'BRL')
);

-------------------------------------------------------------

CREATE UNIQUE INDEX ux_credit_account_patient_clinic ON credit_accounts (
    patient_id,
    clinic_id,
    currency
)
WHERE
    clinic_id IS NOT NULL;

CREATE UNIQUE INDEX ux_credit_account_patient_no_clinic ON credit_accounts (patient_id, currency)
WHERE
    clinic_id IS NULL;

-------------------------------------------------------------

CREATE TABLE credit_entries (

    id UUID PRIMARY KEY,

    credit_account_id UUID NOT NULL,

    direction VARCHAR(10) NOT NULL,

    entry_type VARCHAR(50) NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    source_receivable_id UUID,

    source_payment_allocation_id UUID,

    target_receivable_id UUID,

    receivable_cancellation_id UUID,

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_credit_entry_account
        FOREIGN KEY (credit_account_id)
        REFERENCES credit_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_source_receivable
        FOREIGN KEY (source_receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_payment_allocation
        FOREIGN KEY (source_payment_allocation_id)
        REFERENCES payment_allocations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_target_receivable
        FOREIGN KEY (target_receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_cancellation
        FOREIGN KEY (receivable_cancellation_id)
        REFERENCES receivable_cancellations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_credit_entry_amount
        CHECK (amount > 0),

    CONSTRAINT chk_credit_entry_direction
        CHECK (
            direction IN (
                'CREDIT',
                'DEBIT'
            )
        ),

    CONSTRAINT chk_credit_entry_type
        CHECK (
            entry_type IN (
                'RECEIVABLE_CANCELLATION',
                'RECEIVABLE_APPLICATION'
            )
        )
);

---------------------------------------------------------

CREATE OR REPLACE FUNCTION prevent_credit_entry_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'credit entries are append-only';

END;
$$;


CREATE TRIGGER trg_credit_entry_update
BEFORE UPDATE
ON credit_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_credit_entry_mutation();


CREATE TRIGGER trg_credit_entry_delete
BEFORE DELETE
ON credit_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_credit_entry_mutation();

----------------------------------------------------------------

ALTER TABLE refunds
ADD COLUMN receivable_cancellation_id UUID;

ALTER TABLE refunds
ADD CONSTRAINT fk_refund_receivable_cancellation
FOREIGN KEY (receivable_cancellation_id)
REFERENCES receivable_cancellations(id)
ON DELETE RESTRICT;

CREATE INDEX idx_refund_cancellation
ON refunds(receivable_cancellation_id);