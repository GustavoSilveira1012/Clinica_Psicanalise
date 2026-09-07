CREATE TABLE therapeutic_relationships (

    id BIGSERIAL PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    psychoanalyst_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    started_at TIMESTAMP NOT NULL,

    ended_at TIMESTAMP,

    end_reason VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_therapeutic_relationship_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_therapeutic_relationship_psychoanalyst
        FOREIGN KEY (psychoanalyst_id)
        REFERENCES psychoanalysts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_therapeutic_relationship_status
        CHECK (
            status IN (
                'ACTIVE',
                'SUSPENDED',
                'ENDED'
            )
        ),

    CONSTRAINT chk_therapeutic_relationship_dates
        CHECK (
            ended_at IS NULL
            OR ended_at >= started_at
        ),

    CONSTRAINT chk_ended_relationship
        CHECK (
            (
                status = 'ENDED'
                AND ended_at IS NOT NULL
            )
            OR
            (
                status <> 'ENDED'
                AND ended_at IS NULL
            )
        )
);

CREATE UNIQUE INDEX ux_therapeutic_relationship_current_pair
    ON therapeutic_relationships(
        patient_id,
        psychoanalyst_id
    )
    WHERE status IN (
        'ACTIVE',
        'SUSPENDED'
    );

CREATE UNIQUE INDEX ux_therapeutic_relationship_primary_patient
    ON therapeutic_relationships(patient_id)
    WHERE is_primary = TRUE
      AND status = 'ACTIVE';