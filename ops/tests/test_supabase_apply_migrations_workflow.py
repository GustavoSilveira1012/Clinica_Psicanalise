import unittest
from pathlib import Path


ROOT = Path(__file__).parents[2]
WORKFLOW = ROOT / ".github/workflows/supabase-apply-migrations.yml"


class SupabaseApplyMigrationsWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = WORKFLOW.read_text(encoding="utf-8")

    def test_target_and_secret_are_pinned(self):
        self.assertIn("branches:\n      - codex/render-homologacao", self.source)
        self.assertIn("jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=verify-full", self.source)
        self.assertIn("MIGRATION_DATABASE_USERNAME: postgres.gdorfdcvajeczzaesjjq", self.source)
        self.assertIn("secrets.PSICOGEST_SUPABASE_DB_PASSWORD", self.source)
        self.assertIn('if [ -z "$MIGRATION_DATABASE_PASSWORD" ]', self.source)
        self.assertNotIn("CLINICAL_DATA_ENABLED", self.source)

    def test_flyway_uses_separate_one_shot_cli_and_public_ca(self):
        self.assertIn("ProductionMigrationCli", self.source)
        self.assertIn("--read-only", self.source)
        self.assertIn("--cap-drop ALL", self.source)
        self.assertTrue((ROOT / "backend/psicogest/psicogest/certs/supabase-prod-ca-2021.crt").is_file())
        dockerfile = (ROOT / "backend/psicogest/psicogest/Dockerfile").read_text(encoding="utf-8")
        self.assertIn("COPY certs/supabase-prod-ca-2021.crt /home/psicogest/.postgresql/root.crt", dockerfile)


if __name__ == "__main__":
    unittest.main()
