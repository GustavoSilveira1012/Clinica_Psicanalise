CREATE TABLE receivable_adjustments (

    id UUID PRIMARY KEY,

    receivable_id UUID NOT NULL,

    direction VARCHAR(20) NOT NULL,

    adjustment_type VARCHAR(50) NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    patient_package_id UUID,

    reason_code VARCHAR(100),

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_receivable_adjustment_receivable
        FOREIGN KEY (receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_receivable_adjustment_package
        FOREIGN KEY (patient_package_id)
        REFERENCES patient_packages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_receivable_adjustment_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_receivable_adjustment_direction
        CHECK (
            direction IN (
                'INCREASE',
                'DECREASE'
            )
        ),

    CONSTRAINT chk_receivable_adjustment_type
        CHECK (
            adjustment_type IN (
                'PACKAGE_CANCELLATION',
                'BILLING_CORRECTION',
                'WRITE_OFF',
                'OTHER'
            )
        ),

    CONSTRAINT chk_receivable_adjustment_amount
        CHECK (
            amount > 0
        )
);


CREATE INDEX idx_receivable_adjustment_receivable
ON receivable_adjustments(
    receivable_id,
    created_at
);

------------------------------------------------

CREATE OR REPLACE FUNCTION prevent_receivable_adjustment_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'receivable adjustments are append-only';

END;
$$;


CREATE TRIGGER trg_receivable_adjustment_update

BEFORE UPDATE
ON receivable_adjustments

FOR EACH ROW

EXECUTE FUNCTION
prevent_receivable_adjustment_mutation();


CREATE TRIGGER trg_receivable_adjustment_delete

BEFORE DELETE
ON receivable_adjustments

FOR EACH ROW

EXECUTE FUNCTION
prevent_receivable_adjustment_mutation();

--------------------------------------------------

ALTER TABLE package_plan_items

ADD COLUMN allocated_amount
NUMERIC(19,2) NOT NULL;


ALTER TABLE package_plan_items

ADD CONSTRAINT chk_package_item_allocated_amount
CHECK (
    allocated_amount > 0
);

----------------------------------------------------

CREATE TABLE patient_package_items (

    id UUID PRIMARY KEY,

    patient_package_id UUID NOT NULL,

    package_plan_item_id UUID NOT NULL,

    service_code VARCHAR(100) NOT NULL,

    appointment_type VARCHAR(30),

    granted_quantity INTEGER NOT NULL,

    allocated_amount NUMERIC(19,2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_patient_package_item_package
        FOREIGN KEY (patient_package_id)
        REFERENCES patient_packages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_package_item_plan_item
        FOREIGN KEY (package_plan_item_id)
        REFERENCES package_plan_items(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_patient_package_plan_item
        UNIQUE (
            patient_package_id,
            package_plan_item_id
        ),

    CONSTRAINT chk_patient_package_item_quantity
        CHECK (
            granted_quantity > 0
        ),

    CONSTRAINT chk_patient_package_item_amount
        CHECK (
            allocated_amount > 0
        )
);

-------------------------

ALTER TABLE session_credit_entries
ADD COLUMN patient_package_item_id UUID;


ALTER TABLE session_credit_entries
ADD CONSTRAINT fk_session_credit_patient_package_item
FOREIGN KEY (patient_package_item_id)
REFERENCES patient_package_items(id)
ON DELETE RESTRICT;