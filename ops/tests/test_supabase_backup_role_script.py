import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "provision-supabase-backup-role.sql"


class SupabaseBackupRoleScriptTest(unittest.TestCase):
    def test_requires_version_94_and_has_no_credential(self):
        sql = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("version = '94'", sql)
        self.assertIn("CREATE ROLE psicogest_backup NOLOGIN NOSUPERUSER", sql)
        self.assertIn("NOREPLICATION BYPASSRLS", sql)
        self.assertNotIn("PASSWORD '", sql)

    def test_backup_role_cannot_write_clinical_tables(self):
        sql = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("GRANT SELECT ON ALL TABLES IN SCHEMA public, app", sql)
        self.assertIn("GRANT INSERT, UPDATE ON TABLE app.clinical_backup_checkpoints", sql)
        self.assertIn("has_table_privilege('psicogest_backup', 'public.patients', 'UPDATE')", sql)
        self.assertIn("BEGIN;", sql)
        self.assertIn("COMMIT;", sql)


if __name__ == "__main__":
    unittest.main()
