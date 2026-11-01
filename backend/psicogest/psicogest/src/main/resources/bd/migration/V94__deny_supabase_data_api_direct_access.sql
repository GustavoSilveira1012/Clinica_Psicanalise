-- PsicoGest serves clinical data only through its authenticated Java API.
-- Supabase auto-grants every new public table to Data API roles; tenant RLS
-- policies use a server-set session context and are not an API auth boundary.
-- Preserve postgres and psicogest_runtime access for migrations and serving.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon')
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated')
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public
            FROM anon, authenticated, service_role;
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public
            FROM anon, authenticated, service_role;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public
            REVOKE ALL PRIVILEGES ON TABLES FROM anon, authenticated, service_role;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public
            REVOKE ALL PRIVILEGES ON SEQUENCES FROM anon, authenticated, service_role;
    END IF;
END $$;
