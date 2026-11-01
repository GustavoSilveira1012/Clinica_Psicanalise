-- Provision on project gdorfdcvajeczzaesjjq only after Flyway V94.
-- No password is embedded. Keep NOLOGIN until a private credential is set.
BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.flyway_schema_history
        WHERE version = '94' AND type = 'SQL' AND success
    ) OR to_regclass('app.clinical_backup_checkpoints') IS NULL THEN
        RAISE EXCEPTION 'PsicoGest Flyway V94 is required before backup role provisioning';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'psicogest_backup') THEN
        CREATE ROLE psicogest_backup NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOREPLICATION BYPASSRLS;
    END IF;
END $$;

GRANT CONNECT ON DATABASE postgres TO psicogest_backup;
GRANT USAGE ON SCHEMA public, app TO psicogest_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA public, app TO psicogest_backup;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public, app TO psicogest_backup;
GRANT INSERT, UPDATE ON TABLE app.clinical_backup_checkpoints TO psicogest_backup;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT ON TABLES TO psicogest_backup;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO psicogest_backup;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'psicogest_backup'
          AND (rolcanlogin OR rolsuper OR rolcreatedb OR rolcreaterole
               OR rolreplication OR NOT rolbypassrls)
    ) OR has_schema_privilege('psicogest_backup', 'public', 'CREATE')
      OR has_schema_privilege('psicogest_backup', 'app', 'CREATE')
      OR has_table_privilege('psicogest_backup', 'public.patients', 'UPDATE')
      OR NOT has_table_privilege('psicogest_backup', 'app.clinical_backup_checkpoints', 'UPDATE') THEN
        RAISE EXCEPTION 'Backup role failed least-privilege validation';
    END IF;
END $$;

COMMIT;
