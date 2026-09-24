-- Tabela de timeline para eventos de security incidents (somente insert, sem delete)

CREATE TABLE IF NOT EXISTS security_incident_events (
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


-- V17 creates the legacy shape. Preserve the immutable timeline.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
        AND table_name = 'security_incident_events' AND column_name = 'event_type')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
        AND table_name = 'security_incident_events' AND column_name = 'type') THEN
        ALTER TABLE security_incident_events RENAME COLUMN event_type TO type;
    END IF;
END $$;
ALTER TABLE security_incident_events ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE security_incident_events ALTER COLUMN description TYPE TEXT;

CREATE INDEX idx_security_incident_event_incident
    ON security_incident_events(incident_id, occurred_at);


CREATE INDEX idx_security_incident_event_type
    ON security_incident_events(type, occurred_at);


-- V17's immutable trigger raises on mutation; do not mask rejected writes
-- with DO INSTEAD NOTHING rules that report apparent success.
