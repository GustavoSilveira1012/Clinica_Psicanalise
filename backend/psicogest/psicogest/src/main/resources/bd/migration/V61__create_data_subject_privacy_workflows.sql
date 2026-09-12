-- v0.8: direitos do titular e exportações privadas.

CREATE TABLE data_subject_requests (

    id UUID PRIMARY KEY,

    request_type VARCHAR(50) NOT NULL,

    patient_id BIGINT,

    user_id BIGINT,

    status VARCHAR(50) NOT NULL,

    submitted_at TIMESTAMPTZ NOT NULL,

    identity_verified_at TIMESTAMPTZ,

    review_started_at TIMESTAMPTZ,

    due_at TIMESTAMPTZ,

    completed_at TIMESTAMPTZ,

    decision_code VARCHAR(100),

    encrypted_request_details BYTEA,

    request_details_iv BYTEA,

    encrypted_request_details_dek BYTEA,

    request_details_key_id VARCHAR(255),

    request_details_crypto_version INTEGER,

    request_details_crypto_algorithm VARCHAR(30),

    assigned_to_user_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_request_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subject_request_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subject_request_assignee
        FOREIGN KEY (assigned_to_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_subject_request_type
        CHECK (
            request_type IN (
                'CONFIRMATION',
                'ACCESS',
                'CORRECTION',
                'ANONYMIZATION',
                'BLOCKING',
                'DELETION',
                'PORTABILITY',
                'SHARING_INFORMATION',
                'CONSENT_REVOCATION',
                'AUTOMATED_DECISION_REVIEW',
                'OTHER'
            )
        ),

    CONSTRAINT chk_subject_request_status
        CHECK (
            status IN (
                'RECEIVED',
                'IDENTITY_VERIFICATION_REQUIRED',
                'VERIFIED',
                'IN_REVIEW',
                'WAITING_INFORMATION',
                'APPROVED',
                'PARTIALLY_APPROVED',
                'DENIED',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_subject_request_version
        CHECK (version >= 0)
);

CREATE INDEX idx_subject_request_status
    ON data_subject_requests(status, due_at);

CREATE INDEX idx_subject_request_patient
    ON data_subject_requests(patient_id, submitted_at DESC);

CREATE INDEX idx_subject_request_user
    ON data_subject_requests(user_id, submitted_at DESC);

CREATE TABLE data_subject_request_decisions (

    id UUID PRIMARY KEY,

    request_id UUID NOT NULL,

    decision VARCHAR(30) NOT NULL,

    reason_code VARCHAR(100) NOT NULL,

    encrypted_justification BYTEA,

    justification_iv BYTEA,

    encrypted_justification_dek BYTEA,

    justification_key_id VARCHAR(255),

    decided_by_user_id BIGINT NOT NULL,

    decided_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_subject_request_decision_request
        FOREIGN KEY (request_id)
        REFERENCES data_subject_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_subject_request_decision_user
        FOREIGN KEY (decided_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_subject_request_decision
        CHECK (
            decision IN (
                'APPROVED',
                'PARTIALLY_APPROVED',
                'DENIED'
            )
        )
);

CREATE INDEX idx_subject_request_decision_request
    ON data_subject_request_decisions(request_id, decided_at DESC);

CREATE TRIGGER trg_data_subject_request_decisions_immutable
BEFORE UPDATE OR DELETE
ON data_subject_request_decisions
FOR EACH ROW
EXECUTE FUNCTION prevent_privacy_history_mutation();

CREATE TABLE privacy_export_jobs (

    id UUID PRIMARY KEY,

    request_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    storage_key VARCHAR(500),

    file_sha256 VARCHAR(64),

    file_size BIGINT,

    requested_at TIMESTAMPTZ NOT NULL,

    processing_started_at TIMESTAMPTZ,

    generated_at TIMESTAMPTZ,

    approved_at TIMESTAMPTZ,

    downloaded_at TIMESTAMPTZ,

    expires_at TIMESTAMPTZ,

    failure_reason VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_privacy_export_request
        FOREIGN KEY (request_id)
        REFERENCES data_subject_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_privacy_export_status
        CHECK (
            status IN (
                'REQUESTED',
                'PROCESSING',
                'READY',
                'APPROVED',
                'FAILED',
                'EXPIRED',
                'COMPLETED'
            )
        ),

    CONSTRAINT chk_privacy_export_hash
        CHECK (
            file_sha256 IS NULL
            OR file_sha256 ~ '^[0-9a-fA-F]{64}$'
        ),

    CONSTRAINT chk_privacy_export_size
        CHECK (file_size IS NULL OR file_size >= 0),

    CONSTRAINT chk_privacy_export_version
        CHECK (version >= 0)
);

CREATE INDEX idx_privacy_export_status
    ON privacy_export_jobs(status, expires_at);
