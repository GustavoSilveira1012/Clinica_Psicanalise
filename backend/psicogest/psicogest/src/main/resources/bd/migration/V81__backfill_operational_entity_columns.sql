-- Flyway executes this migration atomically. The exclusive lock prevents any
-- concurrent ledger writes while ONLY its update guard is temporarily disabled.
-- No financial amounts, quantities, event types or timestamps are rewritten.
LOCK TABLE session_credit_entries IN ACCESS EXCLUSIVE MODE;
ALTER TABLE session_credit_entries DISABLE TRIGGER trg_session_credit_update;
UPDATE session_credit_entries e SET patient_id = p.patient_id
FROM patient_packages p WHERE e.patient_package_id = p.id AND e.patient_id IS NULL;
ALTER TABLE session_credit_entries ENABLE TRIGGER trg_session_credit_update;

UPDATE package_consumptions SET updated_at = COALESCE(reversed_at, created_at) WHERE updated_at IS NULL;
UPDATE payment_webhook_inbox SET updated_at = received_at WHERE updated_at IS NULL;
UPDATE receivables r SET clinic_id = m.clinic_id
FROM appointments a JOIN clinic_memberships m ON m.id = a.clinic_membership_id
WHERE r.appointment_id = a.id AND r.clinic_id IS NULL AND r.organization_id = a.organization_id;
