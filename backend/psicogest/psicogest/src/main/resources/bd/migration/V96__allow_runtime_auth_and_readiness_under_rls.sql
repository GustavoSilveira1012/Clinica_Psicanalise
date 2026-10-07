-- Supabase's event trigger enables RLS on every new public table. Auth and
-- readiness run before a tenant context exists, so those global tables need
-- policies scoped to the dedicated non-BYPASSRLS server role only. Data API
-- roles have no table grants after V94 and receive none of these policies.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'psicogest_runtime') THEN
        CREATE ROLE psicogest_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOREPLICATION NOBYPASSRLS;
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'psicogest_runtime'
          AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
    ) THEN
        RAISE EXCEPTION 'Unsafe PsicoGest runtime role';
    END IF;
END $$;

DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'users', 'user_sessions', 'refresh_tokens',
        'authentication_challenges', 'mfa_methods', 'mfa_recovery_codes',
        'auth_action_tokens', 'email_verification_tokens',
        'password_reset_tokens', 'audit_chain_state', 'security_alerts'
    ] LOOP
        IF to_regclass(format('public.%I', table_name)) IS NULL THEN
            RAISE EXCEPTION 'Missing auth table: %', table_name;
        END IF;
        EXECUTE format(
            'CREATE POLICY %I ON public.%I FOR ALL TO psicogest_runtime USING (true) WITH CHECK (true)',
            table_name || '_runtime_auth', table_name
        );
    END LOOP;
END $$;

-- Flyway holds a lock on its history table while a migration executes.
-- Provision its SELECT policy separately after Flyway completes.
