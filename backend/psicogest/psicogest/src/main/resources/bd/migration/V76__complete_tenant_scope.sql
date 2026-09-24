-- v1.0: close tenant coverage for legacy child tables created after the first
-- SaaS migration. Every operational row receives the same organization as
-- its owning patient, clinic, receivable, package or subscription.

ALTER TABLE IF EXISTS appointment_series ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS availability_exceptions ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS clinical_exports ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS clinic_membership_periods ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS financial_entities ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS package_plans ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS package_plan_versions ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS package_plan_items ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS patient_package_items ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS patient_package_cancellations ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS package_consumptions ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS session_credit_entries ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS receivable_cancellations ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS receivable_adjustments ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS subscription_payment_mandates ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS subscription_charge_attempts ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE IF EXISTS bank_statement_imports ADD COLUMN IF NOT EXISTS organization_id UUID;

UPDATE financial_entities f
SET organization_id = c.organization_id
FROM clinics c
WHERE f.clinic_id = c.id AND f.organization_id IS NULL;

UPDATE financial_entities f
SET organization_id = p.organization_id
FROM psychoanalysts p
WHERE f.psychoanalyst_id = p.id AND f.organization_id IS NULL;

UPDATE appointment_series s SET organization_id = p.organization_id
FROM patients p WHERE s.patient_id = p.id AND s.organization_id IS NULL;
UPDATE availability_exceptions e SET organization_id = p.organization_id
FROM psychoanalysts p WHERE e.psychoanalyst_id = p.id AND e.organization_id IS NULL;
UPDATE clinical_exports e SET organization_id = p.organization_id
FROM patients p WHERE e.patient_id = p.id AND e.organization_id IS NULL;
UPDATE clinic_membership_periods p SET organization_id = c.organization_id
FROM clinic_memberships m JOIN clinics c ON c.id = m.clinic_id
WHERE p.clinic_membership_id = m.id AND p.organization_id IS NULL;
UPDATE package_plans p SET organization_id = f.organization_id
FROM financial_entities f WHERE p.financial_entity_id = f.id AND p.organization_id IS NULL;
UPDATE package_plan_versions v SET organization_id = p.organization_id
FROM package_plans p WHERE v.package_plan_id = p.id AND v.organization_id IS NULL;
UPDATE package_plan_items i SET organization_id = v.organization_id
FROM package_plan_versions v WHERE i.package_plan_version_id = v.id AND i.organization_id IS NULL;
UPDATE patient_package_items i SET organization_id = p.organization_id
FROM patient_packages p WHERE i.patient_package_id = p.id AND i.organization_id IS NULL;
UPDATE patient_package_cancellations c SET organization_id = p.organization_id
FROM patient_packages p WHERE c.patient_package_id = p.id AND c.organization_id IS NULL;
UPDATE package_consumptions c SET organization_id = p.organization_id
FROM patient_packages p WHERE c.patient_package_id = p.id AND c.organization_id IS NULL;
UPDATE session_credit_entries e SET organization_id = p.organization_id
FROM patient_packages p WHERE e.patient_package_id = p.id AND e.organization_id IS NULL;
UPDATE receivable_cancellations c SET organization_id = r.organization_id
FROM receivables r WHERE c.receivable_id = r.id AND c.organization_id IS NULL;
UPDATE receivable_adjustments a SET organization_id = r.organization_id
FROM receivables r WHERE a.receivable_id = r.id AND a.organization_id IS NULL;
UPDATE subscription_payment_mandates m SET organization_id = s.organization_id
FROM patient_subscriptions s WHERE m.subscription_id = s.id AND m.organization_id IS NULL;
UPDATE subscription_charge_attempts a SET organization_id = s.organization_id
FROM subscription_cycles s WHERE a.subscription_cycle_id = s.id AND a.organization_id IS NULL;
UPDATE bank_statement_imports i SET organization_id = a.organization_id
FROM bank_accounts a WHERE i.bank_account_id = a.id AND i.organization_id IS NULL;

DO $$
DECLARE
    table_name TEXT;
    table_names CONSTANT TEXT[] := ARRAY[
        'appointment_series', 'availability_exceptions', 'clinical_exports',
        'clinic_membership_periods', 'financial_entities', 'package_plans',
        'package_plan_versions', 'package_plan_items', 'patient_package_items',
        'patient_package_cancellations', 'package_consumptions',
        'session_credit_entries', 'receivable_cancellations',
        'receivable_adjustments', 'subscription_payment_mandates',
        'subscription_charge_attempts', 'bank_statement_imports'
    ];
BEGIN
    FOREACH table_name IN ARRAY table_names LOOP
        IF to_regclass(table_name) IS NULL THEN CONTINUE; END IF;
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('DROP TRIGGER IF EXISTS trg_current_organization ON %I', table_name);
        EXECUTE format(
            'CREATE TRIGGER trg_current_organization BEFORE INSERT OR UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION app.assign_current_organization()',
            table_name
        );
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I', table_name || '_current_organization', table_name);
        EXECUTE format(
            'CREATE POLICY %I ON %I USING (organization_id = app.current_organization_id()) WITH CHECK (organization_id = app.current_organization_id())',
            table_name || '_current_organization', table_name
        );
    END LOOP;
END $$;
