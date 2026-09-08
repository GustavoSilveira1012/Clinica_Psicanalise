CREATE TABLE security_incidents (

    id UUID PRIMARY KEY,

    source_alert_id UUID,

    category VARCHAR(50) NOT NULL,

    severity VARCHAR(20) NOT NULL,

    status VARCHAR(30) NOT NULL,

    summary VARCHAR(500) NOT NULL,

    suspected_data_breach BOOLEAN
        NOT NULL DEFAULT FALSE,

    suspected_clinical_data_exposure BOOLEAN
        NOT NULL DEFAULT FALSE,

    owner_user_id BIGINT,

    detected_at TIMESTAMPTZ NOT NULL,

    contained_at TIMESTAMPTZ,

    eradicated_at TIMESTAMPTZ,

    recovery_started_at TIMESTAMPTZ,

    resolved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    metadata JSONB
        NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_security_incident_alert
        FOREIGN KEY (source_alert_id)
        REFERENCES security_alerts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_security_incident_owner
        FOREIGN KEY (owner_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_security_incident_severity
        CHECK (
            severity IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT chk_security_incident_status
        CHECK (
            status IN (
                'OPEN',
                'INVESTIGATING',
                'CONTAINED',
                'ERADICATING',
                'RECOVERING',
                'RESOLVED',
                'FALSE_POSITIVE'
            )
        )
);


CREATE INDEX idx_security_incident_status
    ON security_incidents(
        status,
        severity
    );


CREATE INDEX idx_security_incident_detected
    ON security_incidents(
        detected_at
    );

    CREATE TABLE security_incident_events (

    id UUID PRIMARY KEY,

    incident_id UUID NOT NULL,

    actor_user_id BIGINT,

    event_type VARCHAR(60) NOT NULL,

    occurred_at TIMESTAMPTZ NOT NULL,

    description VARCHAR(1000),

    metadata JSONB
        NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_incident_event_incident
        FOREIGN KEY (incident_id)
        REFERENCES security_incidents(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_incident_event_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);


CREATE INDEX idx_incident_event_incident
    ON security_incident_events(
        incident_id,
        occurred_at
    );

CREATE OR REPLACE FUNCTION
prevent_security_incident_event_mutation()

RETURNS TRIGGER
LANGUAGE plpgsql

AS $$

BEGIN

    RAISE EXCEPTION
        'security_incident_events is append-only';

END;

$$;


CREATE TRIGGER
trg_security_incident_events_immutable

BEFORE UPDATE OR DELETE
ON security_incident_events

FOR EACH ROW

EXECUTE FUNCTION
prevent_security_incident_event_mutation();