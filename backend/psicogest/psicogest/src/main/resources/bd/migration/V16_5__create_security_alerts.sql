-- Criar tabela security_alerts (deve existir antes de security_incidents)

CREATE TABLE security_alerts (
    id UUID PRIMARY KEY,
    
    alert_type VARCHAR(60) NOT NULL,
    
    severity VARCHAR(20) NOT NULL,
    
    status VARCHAR(30) NOT NULL,
    
    title VARCHAR(255) NOT NULL,
    
    description TEXT,
    
    source_event_id UUID,
    
    anomaly_score DECIMAL(5, 2),
    
    detected_at TIMESTAMPTZ NOT NULL,
    
    acknowledged_at TIMESTAMPTZ,
    
    dismissed_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    
    CONSTRAINT fk_security_alert_event
        FOREIGN KEY (source_event_id)
        REFERENCES security_events(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_security_alert_severity
        CHECK (
            severity IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),
    
    CONSTRAINT chk_security_alert_status
        CHECK (
            status IN (
                'NEW',
                'ACKNOWLEDGED',
                'INVESTIGATING',
                'DISMISSED',
                'ESCALATED'
            )
        )
);


CREATE INDEX idx_security_alert_status
    ON security_alerts(status, severity);


CREATE INDEX idx_security_alert_detected
    ON security_alerts(detected_at);


CREATE INDEX idx_security_alert_type
    ON security_alerts(alert_type, detected_at);
