-- v0.8: avaliação de incidentes de privacidade, contatos e DPIA.

CREATE TABLE privacy_incident_assessments (

    id UUID PRIMARY KEY,

    security_incident_id UUID NOT NULL UNIQUE,

    personal_data_involved BOOLEAN NOT NULL,

    sensitive_data_involved BOOLEAN NOT NULL,

    financial_data_involved BOOLEAN NOT NULL,

    authentication_data_involved BOOLEAN NOT NULL,

    professional_secrecy_data_involved BOOLEAN NOT NULL,

    children_or_vulnerable_data_involved BOOLEAN NOT NULL,

    large_scale BOOLEAN NOT NULL,

    estimated_subject_count BIGINT,

    risk_level VARCHAR(30) NOT NULL,

    notification_required BOOLEAN,

    assessment_status VARCHAR(30) NOT NULL,

    aware_at TIMESTAMPTZ NOT NULL,

    assessed_at TIMESTAMPTZ,

    anpd_due_at TIMESTAMPTZ,

    subject_due_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_privacy_assessment_incident
        FOREIGN KEY (security_incident_id)
        REFERENCES security_incidents(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_privacy_assessment_risk
        CHECK (
            risk_level IN (
                'LOW',
                'MODERATE',
                'RELEVANT',
                'CRITICAL'
            )
        ),

    CONSTRAINT chk_privacy_assessment_subject_count
        CHECK (
            estimated_subject_count IS NULL
            OR estimated_subject_count >= 0
        )
);

CREATE INDEX idx_privacy_assessment_risk
    ON privacy_incident_assessments(risk_level, assessment_status);

CREATE TRIGGER trg_privacy_incident_assessments_immutable
BEFORE UPDATE OR DELETE
ON privacy_incident_assessments
FOR EACH ROW
EXECUTE FUNCTION prevent_privacy_history_mutation();

CREATE TABLE privacy_contacts (

    id UUID PRIMARY KEY,

    financial_entity_id UUID,

    contact_role VARCHAR(30) NOT NULL,

    public_name VARCHAR(200),

    public_email VARCHAR(255),

    public_phone VARCHAR(50),

    active BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_privacy_contact_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_privacy_contact_entity
    ON privacy_contacts(financial_entity_id, contact_role, active);

CREATE TABLE privacy_impact_assessments (

    id UUID PRIMARY KEY,

    processing_activity_id UUID NOT NULL,

    version INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL,

    risk_level VARCHAR(30) NOT NULL,

    document_storage_key VARCHAR(500),

    document_sha256 VARCHAR(64),

    approved_by_user_id BIGINT,

    approved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_privacy_impact_activity
        FOREIGN KEY (processing_activity_id)
        REFERENCES processing_activities(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_privacy_impact_approver
        FOREIGN KEY (approved_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_privacy_impact_version
        CHECK (version > 0),

    CONSTRAINT chk_privacy_impact_status
        CHECK (
            status IN (
                'DRAFT',
                'UNDER_REVIEW',
                'APPROVED',
                'SUPERSEDED'
            )
        ),

    CONSTRAINT chk_privacy_impact_risk
        CHECK (
            risk_level IN (
                'LOW',
                'MODERATE',
                'RELEVANT',
                'CRITICAL'
            )
        ),

    CONSTRAINT chk_privacy_impact_hash
        CHECK (
            document_sha256 IS NULL
            OR document_sha256 ~ '^[0-9a-fA-F]{64}$'
        ),

    CONSTRAINT ux_privacy_impact_activity_version
        UNIQUE (
            processing_activity_id,
            version
        )
);

CREATE INDEX idx_privacy_impact_status
    ON privacy_impact_assessments(status, risk_level);
