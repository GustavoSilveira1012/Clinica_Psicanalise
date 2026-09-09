CREATE TABLE medical_record_addendums (

    id UUID PRIMARY KEY,

    medical_record_id UUID NOT NULL,

    author_psychoanalyst_id BIGINT NOT NULL,

    encrypted_content BYTEA NOT NULL,

    content_iv BYTEA NOT NULL,

    encrypted_dek BYTEA NOT NULL,

    crypto_version INTEGER NOT NULL,

    crypto_algorithm VARCHAR(30) NOT NULL,

    key_id VARCHAR(255) NOT NULL,

    reason_code VARCHAR(50) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_addendum_medical_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_addendum_author
        FOREIGN KEY (author_psychoanalyst_id)
        REFERENCES psychoanalysts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_addendum_crypto_version
        CHECK (
            crypto_version >= 1
        ),

    CONSTRAINT chk_addendum_reason_code
        CHECK (
            reason_code IN (
                'CLARIFICATION',
                'CORRECTION',
                'COMPLEMENT',
                'OTHER'
            )
        ),

    CONSTRAINT chk_addendum_version
        CHECK (
            version >= 0
        )
);


CREATE INDEX idx_addendum_record
    ON medical_record_addendums(
        medical_record_id,
        created_at DESC
    );


CREATE INDEX idx_addendum_author
    ON medical_record_addendums(
        author_psychoanalyst_id,
        created_at DESC
    );

CREATE OR REPLACE FUNCTION prevent_medical_record_addendum_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'medical record addendums are immutable';

END;
$$;


CREATE TRIGGER trg_medical_record_addendum_update

BEFORE UPDATE
ON medical_record_addendums

FOR EACH ROW

EXECUTE FUNCTION
prevent_medical_record_addendum_mutation();


CREATE TRIGGER trg_medical_record_addendum_delete

BEFORE DELETE
ON medical_record_addendums

FOR EACH ROW

EXECUTE FUNCTION
prevent_medical_record_addendum_mutation();