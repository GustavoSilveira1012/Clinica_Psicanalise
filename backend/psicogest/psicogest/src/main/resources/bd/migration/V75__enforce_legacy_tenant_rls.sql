-- v1.0: enforce tenant isolation for the legacy clinical, financial and
-- communication tables after the application has a transaction-local tenant.
-- The catalog tables (plans/features) intentionally remain global.

CREATE OR REPLACE FUNCTION app.assign_current_organization() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    current_organization UUID;
BEGIN
    current_organization := app.current_organization_id();
    IF NEW.organization_id IS NULL AND current_organization IS NOT NULL THEN
        NEW.organization_id := current_organization;
    END IF;
    RETURN NEW;
END $$;

DO $$
DECLARE
    table_name TEXT;
    table_names CONSTANT TEXT[] := ARRAY[
        'clinics', 'patients', 'psychoanalysts', 'clinic_memberships',
        'clinic_user_memberships', 'appointments', 'availability',
        'therapeutic_relationships', 'medical_records',
        'medical_record_revisions', 'medical_record_addendums', 'receivables',
        'payments', 'payment_allocations', 'refunds', 'refund_allocations',
        'credit_accounts', 'credit_entries', 'patient_packages',
        'patient_subscriptions', 'subscription_cycles', 'service_invoices',
        'fiscal_documents', 'notifications', 'notification_recipients',
        'notification_deliveries', 'notification_preferences',
        'notification_provider_configurations', 'domain_event_outbox',
        'data_subject_requests', 'privacy_export_jobs', 'privacy_contacts',
        'audit_logs', 'security_events', 'bank_accounts', 'bank_transactions',
        'bank_reconciliation_allocations', 'provider_settlements',
        'provider_settlement_items', 'fiscal_providers', 'fiscal_issuers',
        'fiscal_configurations', 'service_invoice_items', 'fiscal_operations',
        'service_invoice_tax_snapshots', 'fiscal_events'
    ];
BEGIN
    FOREACH table_name IN ARRAY table_names LOOP
        IF to_regclass(table_name) IS NULL THEN
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);

        EXECUTE format('DROP TRIGGER IF EXISTS trg_current_organization ON %I', table_name);
        EXECUTE format(
            'CREATE TRIGGER trg_current_organization BEFORE INSERT OR UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION app.assign_current_organization()',
            table_name
        );

        IF NOT EXISTS (
            SELECT 1 FROM pg_policies
            WHERE schemaname = current_schema()
              AND tablename = table_name
              AND policyname = table_name || '_current_organization'
        ) THEN
            EXECUTE format(
                'CREATE POLICY %I ON %I USING (organization_id = app.current_organization_id()) WITH CHECK (organization_id = app.current_organization_id())',
                table_name || '_current_organization', table_name
            );
        END IF;
    END LOOP;
END $$;

-- Members must be able to resolve the organization they belong to before a
-- tenant header is selected. This policy still exposes only active memberships
-- for the authenticated user, never every organization in the database.
DROP POLICY IF EXISTS organizations_tenant_isolation ON organizations;
CREATE POLICY organizations_tenant_isolation ON organizations
    USING (
        id = app.current_organization_id()
        OR owner_user_id = app.current_user_id()
        OR EXISTS (
            SELECT 1
            FROM organization_memberships membership
            WHERE membership.organization_id = organizations.id
              AND membership.user_id = app.current_user_id()
              AND membership.status = 'ACTIVE'
        )
    )
    WITH CHECK (owner_user_id = app.current_user_id());

-- Login/security and audit records can be created before a tenant is selected
-- (for example, a failed login). Those global records are intentionally
-- readable only through their dedicated, protected operational views.
DO $$
BEGIN
    IF to_regclass('audit_logs') IS NOT NULL THEN
        DROP POLICY IF EXISTS audit_logs_current_organization ON audit_logs;
        CREATE POLICY audit_logs_current_organization ON audit_logs
            USING (organization_id = app.current_organization_id() OR organization_id IS NULL)
            WITH CHECK (organization_id = app.current_organization_id() OR organization_id IS NULL);
    END IF;
    IF to_regclass('security_events') IS NOT NULL THEN
        DROP POLICY IF EXISTS security_events_current_organization ON security_events;
        CREATE POLICY security_events_current_organization ON security_events
            USING (organization_id = app.current_organization_id() OR organization_id IS NULL)
            WITH CHECK (organization_id = app.current_organization_id() OR organization_id IS NULL);
    END IF;
END $$;
