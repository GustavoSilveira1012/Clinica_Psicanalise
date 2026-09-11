CREATE TABLE subscription_payment_mandates (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_customer_reference VARCHAR(255),
    provider_payment_method_reference VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_subscription_payment_mandate
        FOREIGN KEY (subscription_id)
        REFERENCES patient_subscriptions(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_subscription_mandate_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX ux_subscription_active_mandate
ON subscription_payment_mandates(subscription_id)
WHERE status = 'ACTIVE';

CREATE TABLE subscription_charge_attempts (
    id UUID PRIMARY KEY,
    subscription_cycle_id UUID NOT NULL,
    payment_id UUID,
    provider VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_transaction_id VARCHAR(255),
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    failure_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_subscription_charge_cycle
        FOREIGN KEY (subscription_cycle_id)
        REFERENCES subscription_cycles(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subscription_charge_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_subscription_charge_attempt
        UNIQUE (subscription_cycle_id, attempt_number),

    CONSTRAINT ux_subscription_charge_idempotency
        UNIQUE (idempotency_key),

    CONSTRAINT chk_subscription_charge_status
        CHECK (status IN (
            'PENDING', 'PROCESSING', 'AWAITING_PROVIDER', 'CONFIRMED',
            'FAILED', 'RECONCILIATION_REQUIRED', 'CANCELLED'
        ))
);

CREATE INDEX idx_subscription_charge_retry
ON subscription_charge_attempts(status, next_retry_at);
