-- Apply only after Flyway V96 finishes. Creating a policy on the history
-- table inside Flyway can block on Flyway's own lock.
BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.flyway_schema_history
        WHERE version = '96' AND type = 'SQL' AND success
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'psicogest_runtime' AND NOT rolbypassrls
    ) THEN
        RAISE EXCEPTION 'Flyway V96 and a non-BYPASSRLS runtime role are required';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'flyway_schema_history'
          AND policyname = 'flyway_runtime_read'
    ) THEN
        CREATE POLICY flyway_runtime_read ON public.flyway_schema_history
            FOR SELECT TO psicogest_runtime USING (true);
    END IF;
END $$;

GRANT SELECT ON public.flyway_schema_history TO psicogest_runtime;
REVOKE INSERT, UPDATE, DELETE ON public.flyway_schema_history FROM psicogest_runtime;

COMMIT;
