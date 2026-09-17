-- v1.0 BACKFILL phase. This fixed bootstrap tenant keeps existing single-clinic
-- installations available while administrators migrate data to real tenants.
-- It must be replaced by an explicit mapping before CONTRACT is enabled.

INSERT INTO organizations (
    id, name, slug, type, status, timezone, owner_user_id,
    created_at, updated_at, version
)
SELECT
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Organização legada',
    'organizacao-legada',
    'CLINIC',
    'ACTIVE',
    'America/Sao_Paulo',
    COALESCE((SELECT MIN(id) FROM users), 1),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE EXISTS (SELECT 1 FROM users)
  AND NOT EXISTS (
      SELECT 1 FROM organizations
      WHERE id = '00000000-0000-0000-0000-000000000001'::uuid
  );

INSERT INTO organization_memberships (
    id, organization_id, user_id, role, status, joined_at, created_at, updated_at, version
)
SELECT
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000001'::uuid,
    MIN(id),
    'OWNER',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM organization_memberships
    WHERE organization_id = '00000000-0000-0000-0000-000000000001'::uuid
      AND role = 'OWNER'
)
HAVING COUNT(*) > 0;

UPDATE clinics SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE patients SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE psychoanalysts SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE clinic_memberships SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE clinic_user_memberships SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE appointments SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE availability SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE therapeutic_relationships SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE medical_records SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE medical_record_revisions SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE medical_record_addendums SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE receivables SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE payments SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE payment_allocations SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE refunds SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE refund_allocations SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE credit_accounts SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE credit_entries SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE patient_packages SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE patient_subscriptions SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE subscription_cycles SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE service_invoices SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE fiscal_documents SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE notifications SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE notification_recipients SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE notification_deliveries SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE notification_preferences SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE notification_provider_configurations SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE domain_event_outbox SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE data_subject_requests SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE privacy_export_jobs SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE privacy_contacts SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE audit_logs SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
UPDATE security_events SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid WHERE organization_id IS NULL;
