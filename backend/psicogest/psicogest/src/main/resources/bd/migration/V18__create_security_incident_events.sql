-- Tabela de timeline para eventos de security incidents (somente insert, sem delete)

CREATE TABLE security_incident_events (
    id UUID PRIMARY KEY,
    
    incident_id UUID NOT NULL,
    
    type VARCHAR(50) NOT NULL,
    
    occurred_at TIMESTAMPTZ NOT NULL,
    
    description TEXT,
    
    actor_user_id BIGINT,
    
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_security_incident_event_incident
        FOREIGN KEY (incident_id)
        REFERENCES security_incidents(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_security_incident_event_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_security_incident_event_type
        CHECK (
            type IN (
                'CREATED',
                'ACKNOWLEDGED',
                'INVESTIGATION_STARTED',
                'CONTAINED',
                'ERADICATION_STARTED',
                'ERADICATION_COMPLETED',
                'RECOVERY_STARTED',
                'RECOVERY_COMPLETED',
                'RESOLVED',
                'ESCALATED',
                'DISMISSED',
                'COMMENTED'
            )
        )
);


CREATE INDEX idx_security_incident_event_incident
    ON security_incident_events(incident_id, occurred_at);


CREATE INDEX idx_security_incident_event_type
    ON security_incident_events(type, occurred_at);


-- Garantir que a tabela seja append-only (sem updates/deletes)
CREATE RULE security_incident_events_no_update AS
    ON UPDATE TO security_incident_events
    DO INSTEAD NOTHING;


CREATE RULE security_incident_events_no_delete AS
    ON DELETE TO security_incident_events
    DO INSTEAD NOTHING;
