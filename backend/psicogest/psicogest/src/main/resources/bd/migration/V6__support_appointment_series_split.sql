ALTER TABLE appointment_series
    ADD COLUMN previous_series_id UUID,

    ADD COLUMN superseded_at TIMESTAMP,

    ADD COLUMN superseded_from DATE;


ALTER TABLE appointment_series
    ADD CONSTRAINT fk_appointment_series_previous
        FOREIGN KEY (previous_series_id)
        REFERENCES appointment_series(id)
        ON DELETE RESTRICT;


CREATE INDEX idx_appointment_series_previous
    ON appointment_series(previous_series_id);


ALTER TABLE appointment_series
    DROP CONSTRAINT chk_appointment_series_status;


ALTER TABLE appointment_series
    ADD CONSTRAINT chk_appointment_series_status
        CHECK (
            status IN (
                'ACTIVE',
                'CANCELLED',
                'COMPLETED',
                'SUPERSEDED'
            )
        );