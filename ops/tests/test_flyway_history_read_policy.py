import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "provision-flyway-history-read-policy.sql"


class FlywayHistoryReadPolicyTest(unittest.TestCase):
    def test_separate_read_only_policy_requires_completed_migration(self):
        sql = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("version = '96' AND type = 'SQL' AND success", sql)
        self.assertIn("FOR SELECT TO psicogest_runtime USING (true)", sql)
        self.assertIn("GRANT SELECT ON public.flyway_schema_history", sql)
        self.assertIn("REVOKE INSERT, UPDATE, DELETE", sql)
        self.assertIn("NOT rolbypassrls", sql)


if __name__ == "__main__":
    unittest.main()
