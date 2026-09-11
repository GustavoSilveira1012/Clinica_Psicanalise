CREATE TABLE patient_packages (

    id UUID PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    financial_entity_id UUID NOT NULL,

    package_plan_version_id UUID NOT NULL,

    receivable_id UUID,

    status VARCHAR(40) NOT NULL,

    purchased_at TIMESTAMPTZ NOT NULL,

    activated_at TIMESTAMPTZ,

    starts_at TIMESTAMPTZ,

    expires_at TIMESTAMPTZ,

    exhausted_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_patient_package_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_package_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_package_version
        FOREIGN KEY (package_plan_version_id)
        REFERENCES package_plan_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_patient_package_receivable
        FOREIGN KEY (receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_patient_package_status
        CHECK (
            status IN (
                'PENDING_ACTIVATION',
                'ACTIVE',
                'EXHAUSTED',
                'EXPIRED',
                'CANCELLATION_PENDING',
                'CANCELLED'
            )
        )
);

------------------------------

CREATE INDEX idx_patient_package_patient
ON patient_packages(
    patient_id,
    status
);


CREATE INDEX idx_patient_package_expiration
ON patient_packages(
    status,
    expires_at
);