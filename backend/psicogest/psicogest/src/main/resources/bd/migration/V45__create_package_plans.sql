CREATE TABLE package_plans (

    id UUID PRIMARY KEY,

    financial_entity_id UUID NOT NULL,

    name VARCHAR(150) NOT NULL,

    description VARCHAR(500),

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_package_plan_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_package_plan_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE'
            )
        )
);


CREATE INDEX idx_package_plan_financial_entity
ON package_plans(
    financial_entity_id,
    status
);

----------------------------------------

CREATE TABLE package_plan_versions (

    id UUID PRIMARY KEY,

    package_plan_id UUID NOT NULL,

    version INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL,

    total_price NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    validity_days INTEGER,

    activation_policy VARCHAR(40) NOT NULL,

    consume_no_show BOOLEAN NOT NULL DEFAULT FALSE,

    consume_late_cancellation BOOLEAN NOT NULL DEFAULT FALSE,

    late_cancellation_minutes INTEGER,

    published_at TIMESTAMPTZ,

    retired_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_package_version_plan
        FOREIGN KEY (package_plan_id)
        REFERENCES package_plans(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_package_plan_version
        UNIQUE (
            package_plan_id,
            version
        ),

    CONSTRAINT chk_package_version_status
        CHECK (
            status IN (
                'DRAFT',
                'PUBLISHED',
                'RETIRED'
            )
        ),

    CONSTRAINT chk_package_version_price
        CHECK (
            total_price > 0
        ),

    CONSTRAINT chk_package_version_currency
        CHECK (
            currency = 'BRL'
        ),

    CONSTRAINT chk_package_validity
        CHECK (
            validity_days IS NULL
            OR validity_days > 0
        ),

    CONSTRAINT chk_late_cancellation
        CHECK (
            late_cancellation_minutes IS NULL
            OR late_cancellation_minutes >= 0
        )
);

-------------------------------------------------

CREATE OR REPLACE FUNCTION protect_published_package_version()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF OLD.status IN (
        'PUBLISHED',
        'RETIRED'
    ) THEN

        IF
            NEW.total_price IS DISTINCT FROM OLD.total_price

            OR NEW.currency IS DISTINCT FROM OLD.currency

            OR NEW.validity_days IS DISTINCT FROM OLD.validity_days

            OR NEW.activation_policy IS DISTINCT FROM OLD.activation_policy

            OR NEW.consume_no_show IS DISTINCT FROM OLD.consume_no_show

            OR NEW.consume_late_cancellation
                IS DISTINCT FROM OLD.consume_late_cancellation

            OR NEW.late_cancellation_minutes
                IS DISTINCT FROM OLD.late_cancellation_minutes

        THEN

            RAISE EXCEPTION
                'published package version is immutable';

        END IF;

    END IF;

    RETURN NEW;

END;
$$;