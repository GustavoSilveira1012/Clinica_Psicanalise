CREATE TABLE session_credit_entries (

    id UUID PRIMARY KEY,

    patient_package_id UUID NOT NULL,

    package_item_id UUID NOT NULL,

    direction VARCHAR(10) NOT NULL,

    entry_type VARCHAR(50) NOT NULL,

    quantity INTEGER NOT NULL,

    appointment_id BIGINT,

    reverses_entry_id UUID,

    created_by_user_id BIGINT,

    reason_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_credit_entry_patient_package
        FOREIGN KEY (patient_package_id)
        REFERENCES patient_packages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_package_item
        FOREIGN KEY (package_item_id)
        REFERENCES package_plan_items(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_reversal
        FOREIGN KEY (reverses_entry_id)
        REFERENCES session_credit_entries(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_credit_entry_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_session_credit_direction
        CHECK (
            direction IN (
                'CREDIT',
                'DEBIT'
            )
        ),

    CONSTRAINT chk_session_credit_type
        CHECK (
            entry_type IN (
                'PACKAGE_ACTIVATION',
                'APPOINTMENT_CONSUMPTION',
                'CONSUMPTION_REVERSAL',
                'EXPIRATION',
                'PACKAGE_CANCELLATION',
                'MANUAL_ADJUSTMENT'
            )
        ),

    CONSTRAINT chk_session_credit_quantity
        CHECK (
            quantity > 0
        )
);

-----------------------------------------------------------

CREATE OR REPLACE FUNCTION prevent_session_credit_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'session credit ledger is append-only';

END;
$$;


CREATE TRIGGER trg_session_credit_update
BEFORE UPDATE
ON session_credit_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_session_credit_mutation();


CREATE TRIGGER trg_session_credit_delete
BEFORE DELETE
ON session_credit_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_session_credit_mutation();