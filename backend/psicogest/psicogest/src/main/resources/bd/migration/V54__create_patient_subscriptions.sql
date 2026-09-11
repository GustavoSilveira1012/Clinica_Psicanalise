CREATE TABLE patient_subscriptions (

    id UUID PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    financial_entity_id UUID NOT NULL,

    subscription_plan_version_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    starts_on DATE NOT NULL,

    current_period_start DATE,

    current_period_end DATE,

    next_cycle_start DATE,

    billing_anchor_day INTEGER NOT NULL,

    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,

    cancellation_requested_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,

    paused_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_patient_subscription_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_subscription_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_subscription_version
        FOREIGN KEY (subscription_plan_version_id)
        REFERENCES subscription_plan_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_patient_subscription_status
        CHECK (
            status IN (
                'PENDING_START',
                'ACTIVE',
                'PAST_DUE',
                'PAUSED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_subscription_anchor_day
        CHECK (billing_anchor_day BETWEEN 1 AND 31)
);

-----------------------------------------

CREATE TABLE subscription_cycles (

    id UUID PRIMARY KEY,

    subscription_id UUID NOT NULL,

    cycle_number INTEGER NOT NULL,

    period_start DATE NOT NULL,

    period_end DATE NOT NULL,

    billing_date DATE NOT NULL,

    status VARCHAR(40) NOT NULL,

    receivable_id UUID,

    patient_package_id UUID,

    billed_amount NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    billed_at TIMESTAMPTZ,

    entitlement_granted_at TIMESTAMPTZ,

    closed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_subscription_cycle_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES patient_subscriptions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subscription_cycle_receivable
        FOREIGN KEY (receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subscription_cycle_patient_package
        FOREIGN KEY (patient_package_id)
        REFERENCES patient_packages(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_subscription_cycle_number
        UNIQUE (
            subscription_id,
            cycle_number
        ),

    CONSTRAINT ux_subscription_cycle_period
        UNIQUE (
            subscription_id,
            period_start
        ),

    CONSTRAINT chk_subscription_cycle_period
        CHECK (
            period_end >= period_start
        ),

    CONSTRAINT chk_subscription_cycle_amount
        CHECK (
            billed_amount > 0
        ),

    CONSTRAINT chk_subscription_cycle_status
        CHECK (
            status IN (
                'SCHEDULED',
                'BILLED',
                'PAST_DUE',
                'ENTITLEMENT_GRANTED',
                'CLOSED',
                'CANCELLED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_subscription_cycle_due
ON subscription_cycles(status, billing_date);
