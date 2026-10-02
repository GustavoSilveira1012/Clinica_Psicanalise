import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "provision-supabase-runtime-role.sql"


class SupabaseRuntimeRoleScriptTest(unittest.TestCase):
    def test_fails_closed_until_migrations_exist(self):
        sql = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("to_regclass('public.flyway_schema_history') IS NULL", sql)
        self.assertIn("to_regclass('app.clinical_backup_checkpoints') IS NULL", sql)
        self.assertIn("BEGIN;", sql)
        self.assertIn("COMMIT;", sql)

    def test_role_starts_without_login_or_backup_write_access(self):
        sql = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("CREATE ROLE psicogest_runtime NOLOGIN NOSUPERUSER", sql)
        self.assertIn("NOREPLICATION NOBYPASSRLS", sql)
        self.assertNotIn("ALTER ROLE psicogest_runtime", sql)
        self.assertIn("rolcanlogin OR rolsuper OR rolcreatedb", sql)
        self.assertIn("GRANT SELECT ON TABLE app.clinical_backup_checkpoints", sql)
        self.assertIn("REVOKE INSERT, UPDATE, DELETE ON TABLE app.clinical_backup_checkpoints", sql)
        self.assertIn("REVOKE INSERT, UPDATE, DELETE ON TABLE public.flyway_schema_history", sql)
        self.assertNotIn("PASSWORD '", sql)


if __name__ == "__main__":
    unittest.main()
