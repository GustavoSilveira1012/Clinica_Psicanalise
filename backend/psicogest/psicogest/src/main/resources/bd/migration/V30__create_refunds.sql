ALTER TABLE payment_allocations
DROP CONSTRAINT IF EXISTS ux_payment_receivable_allocation;


CREATE TABLE refunds (

    id UUID PRIMARY KEY,

    payment_id UUID NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    reason VARCHAR(40) NOT NULL,

    status VARCHAR(30) NOT NULL,

    provider VARCHAR(100),

    provider_refund_id VARCHAR(255),

    idempotency_key VARCHAR(100),

    request_fingerprint VARCHAR(64),

    requested_at TIMESTAMPTZ NOT NULL,

    confirmed_at TIMESTAMPTZ,

    failed_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_refund_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_refund_amount
        CHECK (amount > 0),

    CONSTRAINT chk_refund_currency
        CHECK (currency = 'BRL'),

    CONSTRAINT chk_refund_status
        CHECK (
            status IN (
                'PENDING',
                'CONFIRMED',
                'FAILED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_refund_reason
        CHECK (
            reason IN (
                'CUSTOMER_REQUEST',
                'PAYMENT_ERROR',
                'DUPLICATE_CHARGE',
                'SERVICE_ISSUE',
                'OTHER'
            )
        )
);


CREATE UNIQUE INDEX ux_refund_idempotency
ON refunds(idempotency_key)
WHERE idempotency_key IS NOT NULL;


CREATE UNIQUE INDEX ux_refund_provider_reference
ON refunds(provider, provider_refund_id)
WHERE provider IS NOT NULL
AND provider_refund_id IS NOT NULL;


/*
 * Para simplificar concorrência e integração externa,
 * permitimos apenas um refund PENDING por Payment.
 */
CREATE UNIQUE INDEX ux_refund_pending_payment
ON refunds(payment_id)
WHERE status = 'PENDING';


CREATE INDEX idx_refund_payment
ON refunds(payment_id, created_at DESC);

---------------------------------------------------------------

CREATE TABLE refund_allocations (

    id UUID PRIMARY KEY,

    refund_id UUID NOT NULL,

    payment_allocation_id UUID NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_refund_allocation_refund
        FOREIGN KEY (refund_id)
        REFERENCES refunds(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_refund_allocation_payment_allocation
        FOREIGN KEY (payment_allocation_id)
        REFERENCES payment_allocations(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_refund_allocation_amount
        CHECK (amount > 0),

    CONSTRAINT ux_refund_payment_allocation
        UNIQUE (
            refund_id,
            payment_allocation_id
        )
);


CREATE INDEX idx_refund_allocation_refund
ON refund_allocations(refund_id);


CREATE INDEX idx_refund_allocation_payment_allocation
ON refund_allocations(payment_allocation_id);