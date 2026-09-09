-- Tabela append-only de audit logs (sem delete, sem update)

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    
    user_id BIGINT NOT NULL,
    
    session_id UUID NOT NULL,
    
    action VARCHAR(60) NOT NULL,
    
    resource_type VARCHAR(100) NOT NULL,
    
    resource_id VARCHAR(255) NOT NULL,
    
    patient_id BIGINT,
    
    description TEXT,
    
    outcome VARCHAR(30) NOT NULL,
    
    correlation_id VARCHAR(255) NOT NULL,
    
    source_ip VARCHAR(45),
    
    user_agent_hash VARCHAR(64),
    
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    
    occurred_at TIMESTAMPTZ NOT NULL,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_audit_log_session
        FOREIGN KEY (session_id)
        REFERENCES user_sessions(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_audit_log_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_audit_log_outcome
        CHECK (
            outcome IN (
                'SUCCESS',
                'FAILURE',
                'DENIED'
            )
        )
);


CREATE INDEX idx_audit_log_user_time
    ON audit_logs(
        user_id,
        occurred_at DESC
    );


CREATE INDEX idx_audit_log_resource
    ON audit_logs(
        resource_type,
        resource_id,
        occurred_at DESC
    );


CREATE INDEX idx_audit_log_patient_time
    ON audit_logs(
        patient_id,
        occurred_at DESC
    );


CREATE INDEX idx_audit_log_action
    ON audit_logs(
        action,
        occurred_at DESC
    );


CREATE INDEX idx_audit_log_correlation
    ON audit_logs(
        correlation_id
    );


-- Garantir que a tabela seja append-only (sem updates/deletes)
CREATE RULE audit_logs_no_update AS
    ON UPDATE TO audit_logs
    DO INSTEAD NOTHING;


CREATE RULE audit_logs_no_delete AS
    ON DELETE TO audit_logs
    DO INSTEAD NOTHING;
