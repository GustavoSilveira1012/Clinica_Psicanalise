-- Active: 1788478731533@@127.0.0.1@5432@psicogest
CREATE TABLE security_events (

    id UUID PRIMARY KEY,

    user_id BIGINT,

    session_id UUID,

    event_type VARCHAR(60) NOT NULL,

    severity VARCHAR(20) NOT NULL,

    outcome VARCHAR(20) NOT NULL,

    occurred_at TIMESTAMP NOT NULL,

    source_ip VARCHAR(45),

    user_agent_hash VARCHAR(64),

    request_method VARCHAR(10),

    request_path VARCHAR(500),

    resource_type VARCHAR(100),

    resource_id VARCHAR(100),

    correlation_id VARCHAR(100),

    metadata JSONB,

    CONSTRAINT fk_security_event_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_security_event_session
        FOREIGN KEY (session_id)
        REFERENCES user_sessions(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_security_event_severity
        CHECK (
            severity IN (
                'INFO',
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT chk_security_event_outcome
        CHECK (
            outcome IN (
                'SUCCESS',
                'FAILURE',
                'BLOCKED',
                'DETECTED'
            )
        )
);


CREATE INDEX idx_security_event_occurred_at
    ON security_events(occurred_at);


CREATE INDEX idx_security_event_user_time
    ON security_events(
        user_id,
        occurred_at
    );


CREATE INDEX idx_security_event_type_time
    ON security_events(
        event_type,
        occurred_at
    );


CREATE INDEX idx_security_event_severity
    ON security_events(
        severity,
        occurred_at
    );