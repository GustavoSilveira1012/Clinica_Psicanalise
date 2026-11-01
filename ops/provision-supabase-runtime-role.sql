-- Apply only after Flyway V93 succeeds in project gdorfdcvajeczzaesjjq.
-- No password is stored here. The role remains NOLOGIN until its owner sets a
-- strong credential through a private secret workflow.
BEGIN;

DO $$
BEGIN
    IF to_regclass('public.flyway_schema_history') IS NULL
       OR to_regclass('app.clinical_backup_checkpoints') IS NULL THEN
        RAISE EXCEPTION 'PsicoGest Flyway V93 is required before runtime role provisioning';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'psicogest_runtime') THEN
        CREATE ROLE psicogest_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOREPLICATION NOBYPASSRLS;
    END IF;
END $$;

-- Supabase's postgres role is not a PostgreSQL SUPERUSER and cannot run
-- ALTER ROLE ... NOSUPERUSER, even when the target was created safely above.
-- An existing role with unsafe attributes is rejected by the check below.
GRANT CONNECT ON DATABASE postgres TO psicogest_runtime;
GRANT USAGE ON SCHEMA public, app TO psicogest_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO psicogest_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO psicogest_runtime;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO psicogest_runtime;
GRANT SELECT ON TABLE app.clinical_backup_checkpoints TO psicogest_runtime;

-- The serving process checks Flyway history and backup freshness but cannot
-- forge either one. Future schema changes must keep these exceptions.
REVOKE INSERT, UPDATE, DELETE ON TABLE public.flyway_schema_history FROM psicogest_runtime;
REVOKE INSERT, UPDATE, DELETE ON TABLE app.clinical_backup_checkpoints FROM psicogest_runtime;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO psicogest_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO psicogest_runtime;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'psicogest_runtime'
          AND (rolcanlogin OR rolsuper OR rolcreatedb OR rolcreaterole
               OR rolreplication OR rolbypassrls)
    ) OR has_schema_privilege('psicogest_runtime', 'public', 'CREATE')
      OR has_schema_privilege('psicogest_runtime', 'app', 'CREATE')
      OR has_table_privilege('psicogest_runtime', 'app.clinical_backup_checkpoints', 'INSERT')
      OR has_table_privilege('psicogest_runtime', 'app.clinical_backup_checkpoints', 'UPDATE')
      OR has_table_privilege('psicogest_runtime', 'public.flyway_schema_history', 'UPDATE') THEN
        RAISE EXCEPTION 'Runtime role failed least-privilege validation';
    END IF;
END $$;

COMMIT;
