CREATE TABLE subscription_plans (

    id UUID PRIMARY KEY,

    financial_entity_id UUID NOT NULL,

    name VARCHAR(150) NOT NULL,

    description VARCHAR(500),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_subscription_plan_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT
);


CREATE INDEX idx_subscription_plan_financial_entity
ON subscription_plans(
    financial_entity_id,
    active
);

-------------------------------------

CREATE TABLE subscription_plan_versions (

    id UUID PRIMARY KEY,

    subscription_plan_id UUID NOT NULL,

    version INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL,

    entitlement_package_version_id UUID NOT NULL,

    cycle_price NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    billing_interval VARCHAR(30) NOT NULL,

    interval_count INTEGER NOT NULL DEFAULT 1,

    grant_policy VARCHAR(40) NOT NULL,

    rollover_policy VARCHAR(40) NOT NULL,

    cancellation_policy VARCHAR(40) NOT NULL,

    grace_days INTEGER NOT NULL DEFAULT 0,

    effective_from DATE NOT NULL,

    retired_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_subscription_version_plan
        FOREIGN KEY (subscription_plan_id)
        REFERENCES subscription_plans(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subscription_entitlement_package
        FOREIGN KEY (entitlement_package_version_id)
        REFERENCES package_plan_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_subscription_plan_version
        UNIQUE (
            subscription_plan_id,
            version
        ),

    CONSTRAINT chk_subscription_version_status
        CHECK (
            status IN (
                'DRAFT',
                'PUBLISHED',
                'RETIRED'
            )
        ),

    CONSTRAINT chk_subscription_price
        CHECK (cycle_price > 0),

    CONSTRAINT chk_subscription_interval
        CHECK (
            billing_interval = 'MONTHLY'
            AND interval_count > 0
        ),

    CONSTRAINT chk_subscription_grant_policy
        CHECK (
            grant_policy IN (
                'ON_CYCLE_START',
                'ON_FIRST_PAYMENT',
                'ON_FULL_PAYMENT'
            )
        ),

    CONSTRAINT chk_subscription_rollover
        CHECK (
            rollover_policy IN (
                'EXPIRE_AT_CYCLE_END',
                'ROLL_OVER'
            )
        ),

    CONSTRAINT chk_subscription_cancellation
        CHECK (
            cancellation_policy IN (
                'END_OF_CURRENT_PERIOD',
                'MANUAL_IMMEDIATE'
            )
        ),

    CONSTRAINT chk_subscription_grace_days
        CHECK (grace_days >= 0)
);

CREATE UNIQUE INDEX ux_subscription_plan_published_version
ON subscription_plan_versions(subscription_plan_id)
WHERE status = 'PUBLISHED';

CREATE OR REPLACE FUNCTION protect_published_subscription_version()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status IN ('PUBLISHED', 'RETIRED') THEN
        IF NEW.entitlement_package_version_id IS DISTINCT FROM OLD.entitlement_package_version_id
           OR NEW.cycle_price IS DISTINCT FROM OLD.cycle_price
           OR NEW.currency IS DISTINCT FROM OLD.currency
           OR NEW.billing_interval IS DISTINCT FROM OLD.billing_interval
           OR NEW.interval_count IS DISTINCT FROM OLD.interval_count
           OR NEW.grant_policy IS DISTINCT FROM OLD.grant_policy
           OR NEW.rollover_policy IS DISTINCT FROM OLD.rollover_policy
           OR NEW.cancellation_policy IS DISTINCT FROM OLD.cancellation_policy
           OR NEW.grace_days IS DISTINCT FROM OLD.grace_days
           OR NEW.effective_from IS DISTINCT FROM OLD.effective_from THEN
            RAISE EXCEPTION 'published subscription plan version is immutable';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_protect_published_subscription_version
BEFORE UPDATE ON subscription_plan_versions
FOR EACH ROW
EXECUTE FUNCTION protect_published_subscription_version();
