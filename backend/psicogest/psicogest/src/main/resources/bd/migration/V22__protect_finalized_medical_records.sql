CREATE OR REPLACE FUNCTION protect_finalized_medical_record()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF OLD.status = 'FINALIZED' THEN

        IF
            NEW.encrypted_content IS DISTINCT FROM OLD.encrypted_content
            OR
            NEW.content_iv IS DISTINCT FROM OLD.content_iv
            OR
            NEW.encrypted_dek IS DISTINCT FROM OLD.encrypted_dek
            OR
            NEW.patient_id IS DISTINCT FROM OLD.patient_id
            OR
            NEW.author_psychoanalyst_id IS DISTINCT FROM OLD.author_psychoanalyst_id
            OR
            NEW.therapeutic_relationship_id IS DISTINCT FROM OLD.therapeutic_relationship_id
            OR
            NEW.appointment_id IS DISTINCT FROM OLD.appointment_id
        THEN

            RAISE EXCEPTION
                'finalized medical record is immutable';

        END IF;

    END IF;

    RETURN NEW;

END;
$$;


CREATE TRIGGER
trg_protect_finalized_medical_record

BEFORE UPDATE
ON medical_records

FOR EACH ROW

EXECUTE FUNCTION
protect_finalized_medical_record();

CREATE OR REPLACE FUNCTION prevent_medical_record_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'medical records cannot be deleted';

END;
$$;


CREATE TRIGGER
trg_prevent_medical_record_delete

BEFORE DELETE
ON medical_records

FOR EACH ROW

EXECUTE FUNCTION prevent_medical_record_delete();