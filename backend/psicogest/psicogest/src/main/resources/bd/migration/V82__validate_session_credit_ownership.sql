-- Fail closed if a legacy row could not be attributed. No guessed patient IDs.
ALTER TABLE session_credit_entries ALTER COLUMN patient_id SET NOT NULL;
ALTER TABLE package_consumptions ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE payment_webhook_inbox ALTER COLUMN updated_at SET DEFAULT now();
ALTER TABLE payment_webhook_inbox ALTER COLUMN updated_at SET NOT NULL;

CREATE FUNCTION app.validate_session_credit_owner() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM patients p WHERE p.id = NEW.patient_id
                   AND p.organization_id = NEW.organization_id) THEN
        RAISE EXCEPTION 'Session credit patient outside organization' USING ERRCODE = '23514';
    END IF;
    IF NEW.patient_package_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM patient_packages p WHERE p.id = NEW.patient_package_id
        AND p.patient_id = NEW.patient_id AND p.organization_id = NEW.organization_id
    ) THEN
        RAISE EXCEPTION 'Session credit package ownership mismatch' USING ERRCODE = '23514';
    END IF;
    IF NEW.appointment_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM appointments a WHERE a.id = NEW.appointment_id
        AND a.patient_id = NEW.patient_id AND a.organization_id = NEW.organization_id
    ) THEN
        RAISE EXCEPTION 'Session credit appointment ownership mismatch' USING ERRCODE = '23514';
    END IF;
    IF NEW.package_item_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM patient_packages p JOIN package_plan_items i
        ON i.package_plan_version_id = p.package_plan_version_id
        WHERE p.id = NEW.patient_package_id AND i.id = NEW.package_item_id
    ) THEN
        RAISE EXCEPTION 'Session credit item ownership mismatch' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
-- Alphabetically after trg_current_organization so the tenant default is assigned first.
CREATE TRIGGER trg_validate_session_credit_owner BEFORE INSERT ON session_credit_entries
FOR EACH ROW EXECUTE FUNCTION app.validate_session_credit_owner();
