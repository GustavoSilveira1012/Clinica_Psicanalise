-- Expand only. Existing values are preserved; backfill runs separately in V81.
-- Reverting the application does not require dropping these columns.
ALTER TABLE appointments
    ADD COLUMN confirmed_at TIMESTAMP,
    ADD COLUMN completed_at TIMESTAMP,
    ADD COLUMN no_show_at TIMESTAMP,
    ADD COLUMN rescheduled_at TIMESTAMP;
ALTER TABLE clinics ADD COLUMN cnpj VARCHAR(18);
ALTER TABLE package_consumptions
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE patient_package_cancellations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_webhook_inbox ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE receivable_cancellations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE receivables ADD COLUMN clinic_id BIGINT REFERENCES clinics(id) ON DELETE RESTRICT;
ALTER TABLE refunds ADD COLUMN description VARCHAR(255);
ALTER TABLE security_alerts ADD COLUMN risk_score NUMERIC;
ALTER TABLE receivable_adjustments ALTER COLUMN reason_code TYPE VARCHAR(500);
ALTER TABLE session_credit_entries
    ADD COLUMN patient_id BIGINT REFERENCES patients(id) ON DELETE RESTRICT,
    ALTER COLUMN quantity TYPE BIGINT,
    ALTER COLUMN reason_code TYPE VARCHAR(500),
    ALTER COLUMN patient_package_id DROP NOT NULL,
    ALTER COLUMN package_item_id DROP NOT NULL;
