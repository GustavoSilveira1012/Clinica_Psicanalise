CREATE TABLE audit_chain_state (

    id SMALLINT PRIMARY KEY,

    last_sequence BIGINT NOT NULL DEFAULT 0,

    last_mac VARCHAR(64),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_audit_chain_singleton
        CHECK (id = 1),

    CONSTRAINT chk_audit_chain_sequence
        CHECK (last_sequence >= 0)
);


INSERT INTO audit_chain_state (
    id,
    last_sequence,
    last_mac
)
VALUES (
    1,
    0,
    NULL
);


CREATE TABLE audit_logs (

    id UUID PRIMARY KEY,

    sequence BIGINT NOT NULL UNIQUE,

    previous_mac VARCHAR(64),

    entry_mac VARCHAR(64) NOT NULL,

    key_id VARCHAR(100) NOT NULL,

    metadata_hash VARCHAR(64) NOT NULL,

    actor_user_id BIGINT,

    session_id UUID,

    action VARCHAR(80) NOT NULL,

    resource_type VARCHAR(100) NOT NULL,

    resource_id VARCHAR(100),

    patient_id BIGINT,

    clinic_context_id BIGINT,

    outcome VARCHAR(20) NOT NULL,

    occurred_at TIMESTAMPTZ NOT NULL,

    correlation_id VARCHAR(100),

    source_ip VARCHAR(45),

    user_agent_hash VARCHAR(64),

    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_audit_session
        FOREIGN KEY (session_id)
        REFERENCES user_sessions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_audit_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_audit_clinic
        FOREIGN KEY (clinic_context_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_audit_outcome
        CHECK (
            outcome IN (
                'SUCCESS',
                'FAILURE',
                'DENIED'
            )
        )
);


CREATE INDEX idx_audit_actor_time
    ON audit_logs(
        actor_user_id,
        occurred_at
    );

CREATE INDEX idx_audit_patient_time
    ON audit_logs(
        patient_id,
        occurred_at
    );

CREATE INDEX idx_audit_resource
    ON audit_logs(
        resource_type,
        resource_id,
        occurred_at
    );

CREATE INDEX idx_audit_correlation
    ON audit_logs(
        correlation_id
    );
    
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'audit_logs is append-only';

END;
$$;


CREATE TRIGGER trg_audit_logs_immutable

BEFORE UPDATE OR DELETE
ON audit_logs

FOR EACH ROW

EXECUTE FUNCTION prevent_audit_log_mutation();