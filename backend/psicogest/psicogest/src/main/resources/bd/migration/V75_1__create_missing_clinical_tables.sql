-- Missing prerequisites for V76 and the current JPA mappings. Legacy
-- clinical_export_jobs is deliberately preserved, never implicitly discarded.
CREATE TABLE IF NOT EXISTS availability_exceptions (
    id BIGSERIAL PRIMARY KEY,
    psychoanalyst_id BIGINT NOT NULL REFERENCES psychoanalysts(id) ON DELETE RESTRICT,
    exception_date DATE NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('BLOCKED', 'EXTRA_AVAILABLE')),
    start_time TIME,
    end_time TIME,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_availability_exception_interval CHECK (
        (start_time IS NULL AND end_time IS NULL) OR
        (start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time))
);
CREATE INDEX IF NOT EXISTS idx_availability_exceptions_psychoanalyst_date
    ON availability_exceptions(psychoanalyst_id, exception_date);

CREATE TABLE IF NOT EXISTS clinical_exports (
    id UUID PRIMARY KEY,
    requester_psychoanalyst_id BIGINT NOT NULL REFERENCES psychoanalysts(id) ON DELETE RESTRICT,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL CHECK (status IN ('REQUESTED','PROCESSING','READY','FAILED','EXPIRED')),
    format VARCHAR(30) NOT NULL CHECK (format IN ('JSON','PDF')),
    include_medical_records BOOLEAN NOT NULL,
    include_addendums BOOLEAN NOT NULL,
    include_appointments BOOLEAN NOT NULL,
    from_date DATE,
    to_date DATE,
    storage_key VARCHAR(500),
    file_sha256 VARCHAR(64),
    file_size BIGINT CHECK (file_size >= 0),
    failure_reason VARCHAR(255),
    requested_at TIMESTAMPTZ NOT NULL,
    processing_started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_downloaded_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_clinical_exports_patient ON clinical_exports(patient_id, requested_at DESC);
