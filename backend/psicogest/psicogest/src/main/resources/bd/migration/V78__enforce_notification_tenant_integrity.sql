-- v1.0: make notification tenant isolation enforceable at the database
-- boundary. V75 enabled RLS, but these child tables still accepted nullable
-- tenant columns and several of them had no tenant foreign key.

UPDATE notification_recipients r
SET organization_id = n.organization_id
FROM notifications n
WHERE r.notification_id = n.id
  AND r.organization_id IS DISTINCT FROM n.organization_id;

UPDATE notification_deliveries d
SET organization_id = n.organization_id
FROM notifications n
WHERE d.notification_id = n.id
  AND d.organization_id IS DISTINCT FROM n.organization_id;

UPDATE notification_preferences p
SET organization_id = patient.organization_id
FROM patients patient
WHERE p.patient_id = patient.id
  AND p.organization_id IS NULL;

UPDATE notification_provider_configurations c
SET organization_id = entity.organization_id
FROM financial_entities entity
WHERE c.financial_entity_id = entity.id
  AND c.organization_id IS DISTINCT FROM entity.organization_id;

DO $$
DECLARE
    table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'notifications', 'notification_recipients',
        'notification_deliveries', 'notification_preferences',
        'notification_provider_configurations', 'domain_event_outbox'
    ] LOOP
        IF to_regclass(table_name) IS NOT NULL THEN
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN organization_id SET NOT NULL',
                table_name
            );
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    constraint_name TEXT;
    table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'notifications', 'notification_recipients',
        'notification_deliveries', 'notification_preferences',
        'notification_provider_configurations', 'domain_event_outbox'
    ] LOOP
        IF to_regclass(table_name) IS NULL THEN CONTINUE; END IF;

        constraint_name := 'fk_' || table_name || '_organization';
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = constraint_name
              AND conrelid = to_regclass(table_name)
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT NOT VALID',
                table_name, constraint_name
            );
        END IF;

        EXECUTE format(
            'ALTER TABLE %I VALIDATE CONSTRAINT %I',
            table_name, constraint_name
        );
    END LOOP;
END $$;

DO $$
DECLARE
    table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'notification_recipients', 'notification_deliveries',
        'notification_preferences', 'notification_provider_configurations'
    ] LOOP
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

CREATE INDEX IF NOT EXISTS idx_notification_recipients_organization
    ON notification_recipients(organization_id, notification_id);
CREATE INDEX IF NOT EXISTS idx_notification_deliveries_organization
    ON notification_deliveries(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_preferences_organization
    ON notification_preferences(organization_id, notification_type, channel);
CREATE INDEX IF NOT EXISTS idx_notification_provider_config_organization
    ON notification_provider_configurations(organization_id, channel, active);
