-- v0.8: governança de privacidade, inventário de tratamento e retenção.

CREATE TABLE legal_basis_definitions (

    id UUID PRIMARY KEY,

    code VARCHAR(100) NOT NULL UNIQUE,

    law_reference VARCHAR(100) NOT NULL,

    applies_to_sensitive_data BOOLEAN NOT NULL,

    requires_consent BOOLEAN NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE retention_policies (

    id UUID PRIMARY KEY,

    code VARCHAR(100) NOT NULL UNIQUE,

    data_domain VARCHAR(100) NOT NULL,

    retention_mode VARCHAR(40) NOT NULL,

    retention_period_days INTEGER,

    action_after_retention VARCHAR(40) NOT NULL,

    legal_reference VARCHAR(500),

    requires_manual_review BOOLEAN NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_retention_mode
        CHECK (
            retention_mode IN (
                'FIXED_PERIOD',
                'EVENT_BASED',
                'INDEFINITE_UNTIL_REVIEW'
            )
        ),

    CONSTRAINT chk_retention_action
        CHECK (
            action_after_retention IN (
                'DELETE',
                'ANONYMIZE',
                'BLOCK',
                'MANUAL_REVIEW'
            )
        ),

    CONSTRAINT chk_retention_period
        CHECK (
            (
                retention_mode = 'FIXED_PERIOD'
                AND retention_period_days IS NOT NULL
                AND retention_period_days > 0
            )
            OR
            (
                retention_mode <> 'FIXED_PERIOD'
                AND retention_period_days IS NULL
            )
        )
);

CREATE TABLE processing_activities (

    id UUID PRIMARY KEY,

    financial_entity_id UUID,

    code VARCHAR(100) NOT NULL,

    name VARCHAR(200) NOT NULL,

    description VARCHAR(1000),

    processing_role VARCHAR(30) NOT NULL,

    purpose VARCHAR(1000) NOT NULL,

    legal_basis_id UUID NOT NULL,

    sensitive_data BOOLEAN NOT NULL,

    retention_policy_id UUID,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_processing_activity_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_processing_activity_basis
        FOREIGN KEY (legal_basis_id)
        REFERENCES legal_basis_definitions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_processing_activity_retention
        FOREIGN KEY (retention_policy_id)
        REFERENCES retention_policies(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_processing_role
        CHECK (
            processing_role IN (
                'CONTROLLER',
                'PROCESSOR',
                'JOINT_CONTROLLER'
            )
        )
);

CREATE INDEX idx_processing_activity_entity
    ON processing_activities(financial_entity_id, active);

CREATE TABLE processing_activity_data_categories (

    id UUID PRIMARY KEY,

    processing_activity_id UUID NOT NULL,

    category VARCHAR(50) NOT NULL,

    sensitivity VARCHAR(20) NOT NULL,

    CONSTRAINT fk_processing_data_activity
        FOREIGN KEY (processing_activity_id)
        REFERENCES processing_activities(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_processing_activity_category
        UNIQUE (
            processing_activity_id,
            category
        ),

    CONSTRAINT chk_processing_data_category
        CHECK (
            category IN (
                'IDENTIFICATION',
                'CONTACT',
                'FINANCIAL',
                'AUTHENTICATION',
                'HEALTH',
                'CLINICAL',
                'FISCAL',
                'COMMUNICATION',
                'TECHNICAL',
                'SECURITY'
            )
        ),

    CONSTRAINT chk_processing_data_sensitivity
        CHECK (sensitivity IN ('PERSONAL', 'SENSITIVE'))
);

CREATE TABLE data_recipients (

    id UUID PRIMARY KEY,

    name VARCHAR(200) NOT NULL,

    recipient_type VARCHAR(50) NOT NULL,

    country_code VARCHAR(2),

    privacy_contact VARCHAR(255),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_data_recipient_country
        CHECK (
            country_code IS NULL
            OR country_code = upper(country_code)
        )
);

CREATE TABLE processing_activity_recipients (

    processing_activity_id UUID NOT NULL,

    recipient_id UUID NOT NULL,

    purpose VARCHAR(500) NOT NULL,

    PRIMARY KEY (
        processing_activity_id,
        recipient_id
    ),

    CONSTRAINT fk_activity_recipient_activity
        FOREIGN KEY (processing_activity_id)
        REFERENCES processing_activities(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_activity_recipient_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES data_recipients(id)
        ON DELETE RESTRICT
);

CREATE TABLE international_data_transfers (

    id UUID PRIMARY KEY,

    recipient_id UUID NOT NULL,

    destination_country VARCHAR(2) NOT NULL,

    transfer_mechanism VARCHAR(100) NOT NULL,

    contract_reference VARCHAR(255),

    effective_from DATE NOT NULL,

    effective_to DATE,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_international_transfer_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES data_recipients(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_international_transfer_country
        CHECK (destination_country = upper(destination_country)),

    CONSTRAINT chk_international_transfer_period
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        )
);

CREATE TABLE privacy_notice_versions (

    id UUID PRIMARY KEY,

    notice_key VARCHAR(100) NOT NULL,

    version VARCHAR(30) NOT NULL,

    content_sha256 VARCHAR(64) NOT NULL,

    document_storage_key VARCHAR(500) NOT NULL,

    status VARCHAR(30) NOT NULL,

    effective_from TIMESTAMPTZ,

    retired_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ux_privacy_notice_version
        UNIQUE (
            notice_key,
            version
        ),

    CONSTRAINT chk_privacy_notice_status
        CHECK (
            status IN (
                'DRAFT',
                'PUBLISHED',
                'RETIRED'
            )
        ),

    CONSTRAINT chk_privacy_notice_hash
        CHECK (content_sha256 ~ '^[0-9a-fA-F]{64}$')
);

CREATE TABLE consent_records (

    id UUID PRIMARY KEY,

    patient_id BIGINT,

    user_id BIGINT,

    processing_activity_id UUID NOT NULL,

    privacy_notice_version_id UUID,

    status VARCHAR(20) NOT NULL,

    consent_text_hash VARCHAR(64) NOT NULL,

    granted_at TIMESTAMPTZ NOT NULL,

    revoked_at TIMESTAMPTZ,

    source VARCHAR(50) NOT NULL,

    correlation_id VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_consent_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consent_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consent_activity
        FOREIGN KEY (processing_activity_id)
        REFERENCES processing_activities(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_consent_notice
        FOREIGN KEY (privacy_notice_version_id)
        REFERENCES privacy_notice_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_consent_owner
        CHECK (
            (patient_id IS NOT NULL AND user_id IS NULL)
            OR
            (patient_id IS NULL AND user_id IS NOT NULL)
        ),

    CONSTRAINT chk_consent_status
        CHECK (status IN ('GRANTED', 'REVOKED')),

    CONSTRAINT chk_consent_revocation
        CHECK (
            (status = 'GRANTED' AND revoked_at IS NULL)
            OR
            (status = 'REVOKED' AND revoked_at IS NOT NULL)
        ),

    CONSTRAINT chk_consent_hash
        CHECK (consent_text_hash ~ '^[0-9a-fA-F]{64}$')
);

CREATE INDEX idx_consent_patient
    ON consent_records(patient_id, processing_activity_id, created_at DESC);

CREATE INDEX idx_consent_user
    ON consent_records(user_id, processing_activity_id, created_at DESC);

CREATE TABLE legal_holds (

    id UUID PRIMARY KEY,

    scope_type VARCHAR(50) NOT NULL,

    scope_id VARCHAR(100) NOT NULL,

    reason_code VARCHAR(100) NOT NULL,

    legal_reference VARCHAR(500),

    starts_at TIMESTAMPTZ NOT NULL,

    ends_at TIMESTAMPTZ,

    active BOOLEAN NOT NULL,

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_legal_hold_creator
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_legal_hold_period
        CHECK (
            ends_at IS NULL
            OR ends_at >= starts_at
        )
);

CREATE INDEX idx_legal_hold_scope
    ON legal_holds(scope_type, scope_id, active);

CREATE TABLE data_disposal_jobs (

    id UUID PRIMARY KEY,

    resource_type VARCHAR(100) NOT NULL,

    resource_id VARCHAR(100) NOT NULL,

    retention_policy_id UUID NOT NULL,

    action VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    eligible_at TIMESTAMPTZ NOT NULL,

    executed_at TIMESTAMPTZ,

    blocked_reason VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_disposal_job_policy
        FOREIGN KEY (retention_policy_id)
        REFERENCES retention_policies(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_disposal_job_action
        CHECK (
            action IN (
                'DELETE',
                'ANONYMIZE',
                'BLOCK',
                'MANUAL_REVIEW'
            )
        ),

    CONSTRAINT chk_disposal_job_status
        CHECK (
            status IN (
                'CANDIDATE',
                'PENDING_REVIEW',
                'BLOCKED_BY_HOLD',
                'APPROVED',
                'PROCESSING',
                'COMPLETED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_disposal_job_status
    ON data_disposal_jobs(status, eligible_at);

CREATE OR REPLACE FUNCTION prevent_privacy_history_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_consent_records_immutable
BEFORE UPDATE OR DELETE
ON consent_records
FOR EACH ROW
EXECUTE FUNCTION prevent_privacy_history_mutation();

CREATE TRIGGER trg_legal_holds_immutable
BEFORE UPDATE OR DELETE
ON legal_holds
FOR EACH ROW
EXECUTE FUNCTION prevent_privacy_history_mutation();

CREATE OR REPLACE FUNCTION prevent_published_privacy_notice_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' OR OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'published privacy notice versions are immutable';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_privacy_notice_versions_immutable
BEFORE UPDATE OR DELETE
ON privacy_notice_versions
FOR EACH ROW
EXECUTE FUNCTION prevent_published_privacy_notice_mutation();
