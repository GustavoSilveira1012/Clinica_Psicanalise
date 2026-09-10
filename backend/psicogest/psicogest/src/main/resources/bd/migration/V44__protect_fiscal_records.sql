CREATE OR REPLACE FUNCTION protect_authorized_service_invoice()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF OLD.status IN (
        'AUTHORIZED',
        'CANCEL_PENDING',
        'CANCELLED'
    ) THEN

        IF
            NEW.fiscal_issuer_id
                IS DISTINCT FROM OLD.fiscal_issuer_id

            OR NEW.fiscal_configuration_id
                IS DISTINCT FROM OLD.fiscal_configuration_id

            OR NEW.patient_id
                IS DISTINCT FROM OLD.patient_id

            OR NEW.competence_date
                IS DISTINCT FROM OLD.competence_date

            OR NEW.service_amount
                IS DISTINCT FROM OLD.service_amount

            OR NEW.taxable_amount
                IS DISTINCT FROM OLD.taxable_amount

            OR NEW.net_amount
                IS DISTINCT FROM OLD.net_amount

            OR NEW.dps_series
                IS DISTINCT FROM OLD.dps_series

            OR NEW.dps_number
                IS DISTINCT FROM OLD.dps_number

            OR NEW.access_key
                IS DISTINCT FROM OLD.access_key

        THEN

            RAISE EXCEPTION
                'authorized fiscal document is immutable';

        END IF;

    END IF;

    RETURN NEW;

END;
$$;

-----------------------------------

CREATE TRIGGER trg_service_invoice_immutability

BEFORE UPDATE
ON service_invoices

FOR EACH ROW

EXECUTE FUNCTION protect_authorized_service_invoice();

--------------------------------

CREATE OR REPLACE FUNCTION prevent_service_invoice_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'service invoices cannot be deleted';

END;
$$;


CREATE TRIGGER trg_service_invoice_delete

BEFORE DELETE
ON service_invoices

FOR EACH ROW

EXECUTE FUNCTION prevent_service_invoice_delete();

-----------------------------------------

CREATE TABLE fiscal_events (

    id UUID PRIMARY KEY,

    invoice_id UUID NOT NULL,

    event_type VARCHAR(40) NOT NULL,

    status VARCHAR(40) NOT NULL,

    provider_event_id VARCHAR(255),

    reason_code VARCHAR(100),

    encrypted_justification BYTEA,

    justification_iv BYTEA,

    encrypted_justification_dek BYTEA,

    justification_key_id VARCHAR(255),

    justification_crypto_version INTEGER,

    justification_crypto_algorithm VARCHAR(30),

    requested_at TIMESTAMPTZ NOT NULL,

    accepted_at TIMESTAMPTZ,

    rejected_at TIMESTAMPTZ,

    rejection_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_fiscal_event_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_fiscal_event_type
        CHECK (
            event_type IN (
                'CANCELLATION',
                'SUBSTITUTION'
            )
        ),

    CONSTRAINT chk_fiscal_event_status
        CHECK (
            status IN (
                'PENDING_PROVIDER',
                'PENDING_AUTHORITY',
                'ACCEPTED',
                'REJECTED',
                'ERROR'
            )
        )
);