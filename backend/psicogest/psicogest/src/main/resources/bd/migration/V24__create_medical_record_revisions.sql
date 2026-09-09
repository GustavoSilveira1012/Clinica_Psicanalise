CREATE TABLE medical_record_revisions (

    id UUID PRIMARY KEY,

    medical_record_id UUID NOT NULL,

    revision_number BIGINT NOT NULL,

    author_psychoanalyst_id BIGINT NOT NULL,

    encrypted_content BYTEA NOT NULL,

    content_iv BYTEA NOT NULL,

    encrypted_dek BYTEA NOT NULL,

    crypto_version INTEGER NOT NULL,

    crypto_algorithm VARCHAR(30) NOT NULL,

    key_id VARCHAR(255) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_medical_record_revision_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_medical_record_revision_author
        FOREIGN KEY (author_psychoanalyst_id)
        REFERENCES psychoanalysts(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_medical_record_revision_number
        UNIQUE (
            medical_record_id,
            revision_number
        ),

    CONSTRAINT chk_medical_record_revision_number
        CHECK (
            revision_number >= 1
        ),

    CONSTRAINT chk_medical_record_revision_crypto_version
        CHECK (
            crypto_version >= 1
        )
);


CREATE INDEX idx_medical_record_revision_record
    ON medical_record_revisions(
        medical_record_id,
        revision_number DESC
    );

CREATE OR REPLACE FUNCTION prevent_medical_record_revision_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'medical record revisions are immutable';

END;
$$;


CREATE TRIGGER trg_medical_record_revision_update

BEFORE UPDATE
ON medical_record_revisions

FOR EACH ROW

EXECUTE FUNCTION
prevent_medical_record_revision_mutation();


CREATE TRIGGER trg_medical_record_revision_delete

BEFORE DELETE
ON medical_record_revisions

FOR EACH ROW

EXECUTE FUNCTION
prevent_medical_record_revision_mutation();

ALTER TABLE medical_records
ADD COLUMN current_revision_number BIGINT
    NOT NULL DEFAULT 1;

ALTER TABLE medical_records
ADD CONSTRAINT chk_medical_record_current_revision
CHECK (
    current_revision_number >= 1
);

ALTER TABLE medical_records
ADD COLUMN finalized_revision_number BIGINT;

ALTER TABLE medical_records
ADD CONSTRAINT chk_medical_record_finalized_revision
CHECK (
    (
        status = 'DRAFT'
        AND finalized_revision_number IS NULL
    )
    OR
    (
        status = 'FINALIZED'
        AND finalized_revision_number IS NOT NULL
    )
);