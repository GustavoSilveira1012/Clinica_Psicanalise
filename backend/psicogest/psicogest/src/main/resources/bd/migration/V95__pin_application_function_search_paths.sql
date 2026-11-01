-- Pin lookup order for application-owned functions. public precedes app to
-- preserve the unqualified table references in existing trigger bodies;
-- pg_temp is last so a caller cannot shadow those relations.
DO $$
DECLARE
    function_signature regprocedure;
BEGIN
    FOR function_signature IN
        SELECT p.oid::regprocedure
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname IN ('public', 'app')
          AND p.proowner = (SELECT oid FROM pg_roles WHERE rolname = current_user)
          AND p.proconfig IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM pg_depend d
              WHERE d.objid = p.oid AND d.deptype = 'e'
          )
    LOOP
        EXECUTE format(
            'ALTER FUNCTION %s SET search_path = pg_catalog, public, app, pg_temp',
            function_signature
        );
    END LOOP;
END $$;
