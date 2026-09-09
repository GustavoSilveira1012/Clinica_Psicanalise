/**
 * Adiciona rastreamento de qual snapshot originou o documento final
 * 
 * Quando um prontuário é finalizado:
 * - finalizedRevisionNumber = currentRevisionNumber
 * - Permite auditoria: qual versão se tornou o documento oficial
 */

ALTER TABLE medical_records
ADD COLUMN finalized_revision_number BIGINT;

ALTER TABLE medical_records
ADD CONSTRAINT fk_finalized_revision_number
FOREIGN KEY (id, finalized_revision_number)
REFERENCES medical_record_revisions(medical_record_id, revision_number)
ON DELETE RESTRICT;

CREATE TABLE clinical_export_jobs (

    id UUID PRIMARY KEY,

    requested_by_user_id BIGINT NOT NULL,

    patient_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    format VARCHAR(20) NOT NULL,

    requested_at TIMESTAMPTZ NOT NULL,

    started_at TIMESTAMPTZ,

    completed_at TIMESTAMPTZ,

    expires_at TIMESTAMPTZ,

    storage_key VARCHAR(500),

    file_sha256 VARCHAR(64),

    file_size_bytes BIGINT,

    error_code VARCHAR(100),

    include_medical_records BOOLEAN NOT NULL,

    include_addendums BOOLEAN NOT NULL,

    include_appointments BOOLEAN NOT NULL,

    from_date DATE,

    to_date DATE,

    CONSTRAINT fk_clinical_export_user
        FOREIGN KEY (requested_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_clinical_export_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_clinical_export_status
        CHECK (
            status IN (
                'REQUESTED',
                'PROCESSING',
                'READY',
                'FAILED',
                'EXPIRED'
            )
        ),

    CONSTRAINT chk_clinical_export_format
        CHECK (
            format IN (
                'PDF',
                'JSON'
            )
        )
);


CREATE INDEX idx_clinical_export_requester
    ON clinical_export_jobs(
        requested_by_user_id,
        requested_at DESC
    );


CREATE INDEX idx_clinical_export_patient
    ON clinical_export_jobs(
        patient_id,
        requested_at DESC
    );