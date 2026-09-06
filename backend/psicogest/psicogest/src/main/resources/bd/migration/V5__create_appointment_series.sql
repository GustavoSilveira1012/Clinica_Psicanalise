CREATE TABLE appointment_series (

    id UUID PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    psychoanalyst_id BIGINT NOT NULL,

    clinic_membership_id BIGINT,

    frequency VARCHAR(30) NOT NULL,

    recurrence_interval INTEGER NOT NULL DEFAULT 1,

    day_of_week VARCHAR(20) NOT NULL,

    start_time TIME NOT NULL,

    duration_minutes INTEGER NOT NULL,

    starts_on DATE NOT NULL,

    ends_on DATE,

    total_occurrences INTEGER,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointment_series_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointment_series_psychoanalyst
        FOREIGN KEY (psychoanalyst_id)
        REFERENCES psychoanalysts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointment_series_membership
        FOREIGN KEY (clinic_membership_id)
        REFERENCES clinic_memberships(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_appointment_series_frequency
        CHECK (
            frequency IN ('WEEKLY')
        ),

    CONSTRAINT chk_appointment_series_status
        CHECK (
            status IN (
                'ACTIVE',
                'CANCELLED',
                'COMPLETED'
            )
        ),

    CONSTRAINT chk_recurrence_interval
        CHECK (recurrence_interval > 0),

    CONSTRAINT chk_duration_minutes
        CHECK (
            duration_minutes > 0
            AND duration_minutes <= 480
        ),

    CONSTRAINT chk_total_occurrences
        CHECK (
            total_occurrences IS NULL
            OR total_occurrences >= 2
        ),

    CONSTRAINT chk_series_dates
        CHECK (
            ends_on IS NULL
            OR ends_on >= starts_on
        )
);

CREATE INDEX idx_appointment_series_patient
    ON appointment_series(patient_id);

CREATE INDEX idx_appointment_series_psychoanalyst
    ON appointment_series(psychoanalyst_id);

CREATE INDEX idx_appointment_series_status
    ON appointment_series(status);

    ALTER TABLE appointments
    ADD COLUMN appointment_series_id UUID,

    ADD COLUMN occurrence_number INTEGER;
    
    ALTER TABLE appointments
    ADD CONSTRAINT fk_appointment_series
        FOREIGN KEY (appointment_series_id)
        REFERENCES appointment_series(id)
        ON DELETE RESTRICT;

        CREATE INDEX idx_appointments_series
    ON appointments(appointment_series_id);

    ALTER TABLE appointments
    ADD CONSTRAINT chk_occurrence_number
        CHECK (
            occurrence_number IS NULL
            OR occurrence_number > 0
        );