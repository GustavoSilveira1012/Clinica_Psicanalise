-- Dropa a tabela medical_records se existir e recriar com as colunas corretas
-- Isso é necessário se a tabela foi criada anteriormente sem as colunas corretas

DROP TABLE IF EXISTS medical_records CASCADE;

CREATE TABLE medical_records (

    id UUID PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    author_psychoanalyst_id BIGINT NOT NULL,

    therapeutic_relationship_id BIGINT NOT NULL,

    appointment_id BIGINT,

    status VARCHAR(30) NOT NULL,

    encrypted_content BYTEA NOT NULL,

    content_iv BYTEA NOT NULL,

    encrypted_dek BYTEA NOT NULL,

    crypto_version INTEGER NOT NULL,

    crypto_algorithm VARCHAR(30) NOT NULL,

    key_id VARCHAR(255) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    finalized_at TIMESTAMPTZ,

    CONSTRAINT fk_medical_record_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_medical_record_author
        FOREIGN KEY (author_psychoanalyst_id)
        REFERENCES psychoanalysts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_medical_record_relationship
        FOREIGN KEY (therapeutic_relationship_id)
        REFERENCES therapeutic_relationships(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_medical_record_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_medical_record_status
        CHECK (
            status IN (
                'DRAFT',
                'FINALIZED'
            )
        ),

    CONSTRAINT chk_medical_record_finalization
        CHECK (
            (
                status = 'DRAFT'
                AND finalized_at IS NULL
            )
            OR
            (
                status = 'FINALIZED'
                AND finalized_at IS NOT NULL
            )
        ),

    CONSTRAINT chk_medical_record_crypto_version
        CHECK (
            crypto_version >= 1
        ),

    CONSTRAINT chk_medical_record_version
        CHECK (
            version >= 0
        )
);


CREATE INDEX idx_medical_record_patient
    ON medical_records(
        patient_id,
        created_at DESC
    );


CREATE INDEX idx_medical_record_author
    ON medical_records(
        author_psychoanalyst_id,
        created_at DESC
    );


CREATE INDEX idx_medical_record_relationship
    ON medical_records(
        therapeutic_relationship_id
    );


CREATE INDEX idx_medical_record_appointment
    ON medical_records(
        appointment_id
    );

CREATE UNIQUE INDEX ux_medical_record_appointment
    ON medical_records(appointment_id)
    WHERE appointment_id IS NOT NULL;