-- v1.0: close the remaining tenant gaps in notification and privacy history.
-- Templates are copied per organization before being edited or published;
-- privacy decisions inherit the organization of their request.

ALTER TABLE IF EXISTS notification_templates
    ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE IF EXISTS data_subject_request_decisions
    ADD COLUMN IF NOT EXISTS organization_id UUID;

UPDATE notification_templates
SET organization_id = '00000000-0000-0000-0000-000000000001'::uuid
WHERE organization_id IS NULL;

UPDATE data_subject_request_decisions d
SET organization_id = r.organization_id
FROM data_subject_requests r
WHERE d.request_id = r.id
  AND d.organization_id IS NULL;

DO $$
BEGIN
    IF to_regclass('notification_templates') IS NOT NULL THEN
        ALTER TABLE notification_templates
            ALTER COLUMN organization_id SET NOT NULL;
        ALTER TABLE notification_templates
            ADD CONSTRAINT fk_notification_template_organization
            FOREIGN KEY (organization_id) REFERENCES organizations(id)
            ON DELETE RESTRICT;
        CREATE INDEX IF NOT EXISTS idx_notification_template_organization
            ON notification_templates(organization_id, template_key, version DESC);
    END IF;

    IF to_regclass('data_subject_request_decisions') IS NOT NULL THEN
        ALTER TABLE data_subject_request_decisions
            ALTER COLUMN organization_id SET NOT NULL;
        ALTER TABLE data_subject_request_decisions
            ADD CONSTRAINT fk_subject_request_decision_organization
            FOREIGN KEY (organization_id) REFERENCES organizations(id)
            ON DELETE RESTRICT;
        CREATE INDEX IF NOT EXISTS idx_subject_request_decision_organization
            ON data_subject_request_decisions(organization_id, decided_at DESC);
    END IF;
END $$;

DO $$
DECLARE
    table_name TEXT;
    table_names CONSTANT TEXT[] := ARRAY[
        'notification_templates', 'data_subject_request_decisions'
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
