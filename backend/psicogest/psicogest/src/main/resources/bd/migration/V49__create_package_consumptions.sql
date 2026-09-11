CREATE TABLE package_consumptions (

    id UUID PRIMARY KEY,

    patient_package_id UUID NOT NULL,

    package_item_id UUID NOT NULL,

    appointment_id BIGINT NOT NULL,

    debit_entry_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL,

    consumed_at TIMESTAMPTZ NOT NULL,

    reversed_at TIMESTAMPTZ,

    reversal_entry_id UUID,

    reversal_reason VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_consumption_package
        FOREIGN KEY (patient_package_id)
        REFERENCES patient_packages(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consumption_item
        FOREIGN KEY (package_item_id)
        REFERENCES package_plan_items(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consumption_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consumption_debit
        FOREIGN KEY (debit_entry_id)
        REFERENCES session_credit_entries(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consumption_reversal
        FOREIGN KEY (reversal_entry_id)
        REFERENCES session_credit_entries(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_consumption_status
        CHECK (
            status IN (
                'ACTIVE',
                'REVERSED'
            )
        )
);

--------------------------------

CREATE UNIQUE INDEX ux_active_package_consumption_appointment

ON package_consumptions(appointment_id)

WHERE status = 'ACTIVE';

-----------------------------------------------

CREATE UNIQUE INDEX ux_credit_entry_reversal

ON session_credit_entries(reverses_entry_id)

WHERE reverses_entry_id IS NOT NULL;