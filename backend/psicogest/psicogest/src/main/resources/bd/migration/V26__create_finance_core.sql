-- Drop old payments table from V01 if exists (due to redesign)
DROP TABLE IF EXISTS payments CASCADE;

CREATE TABLE receivables (

    id UUID PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    appointment_id BIGINT,

    description VARCHAR(255) NOT NULL,

    gross_amount NUMERIC(19,2) NOT NULL,

    discount_amount NUMERIC(19,2)
        NOT NULL DEFAULT 0,

    net_amount NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL
        DEFAULT 'BRL',

    due_date DATE NOT NULL,

    status VARCHAR(30) NOT NULL,

    cancelled_at TIMESTAMPTZ,

    cancellation_reason VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_receivable_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_receivable_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_receivable_gross_amount
        CHECK (
            gross_amount > 0
        ),

    CONSTRAINT chk_receivable_discount_amount
        CHECK (
            discount_amount >= 0
        ),

    CONSTRAINT chk_receivable_net_amount
        CHECK (
            net_amount > 0
            AND
            net_amount =
                gross_amount - discount_amount
        ),

    CONSTRAINT chk_receivable_currency
        CHECK (
            currency = 'BRL'
        ),

    CONSTRAINT chk_receivable_status
        CHECK (
            status IN (
                'OPEN',
                'PARTIALLY_PAID',
                'PAID',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_receivable_cancellation
        CHECK (
            (
                status = 'CANCELLED'
                AND cancelled_at IS NOT NULL
            )
            OR
            (
                status <> 'CANCELLED'
                AND cancelled_at IS NULL
            )
        )
);

CREATE UNIQUE INDEX ux_receivable_appointment

ON receivables(appointment_id)

WHERE appointment_id IS NOT NULL;
-------------------------------------------------------------------------
CREATE INDEX idx_receivable_patient

ON receivables(
    patient_id,
    due_date DESC
);


CREATE INDEX idx_receivable_status

ON receivables(
    status,
    due_date
);
----------------------------------------------------------------------------------------

CREATE TABLE payments (

    id UUID PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL
        DEFAULT 'BRL',

    payment_method VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    provider VARCHAR(100),

    provider_transaction_id VARCHAR(255),

    idempotency_key VARCHAR(255),

    received_at TIMESTAMPTZ,

    failed_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_payment_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_payment_amount
        CHECK (
            amount > 0
        ),

    CONSTRAINT chk_payment_currency
        CHECK (
            currency = 'BRL'
        ),

    CONSTRAINT chk_payment_status
        CHECK (
            status IN (
                'PENDING',
                'CONFIRMED',
                'FAILED',
                'CANCELLED',
                'PARTIALLY_REFUNDED',
                'REFUNDED'
            )
        )
);
----------------------------------------------------------

CREATE UNIQUE INDEX ux_payment_idempotency

ON payments(idempotency_key)

WHERE idempotency_key IS NOT NULL;

--------------------------------------------

CREATE UNIQUE INDEX ux_payment_provider_transaction

ON payments(
    provider,
    provider_transaction_id
)

WHERE provider IS NOT NULL
  AND provider_transaction_id IS NOT NULL;
-------------------------------------------------------------------

CREATE TABLE payment_allocations (

    id UUID PRIMARY KEY,

    payment_id UUID NOT NULL,

    receivable_id UUID NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_payment_allocation_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_payment_allocation_receivable
        FOREIGN KEY (receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_payment_allocation_amount
        CHECK (
            amount > 0
        ),

    CONSTRAINT ux_payment_receivable_allocation
        UNIQUE (
            payment_id,
            receivable_id
        )
);

---------------------------------------------------------------

CREATE INDEX idx_payment_allocation_payment
ON payment_allocations(payment_id);


CREATE INDEX idx_payment_allocation_receivable
ON payment_allocations(receivable_id);