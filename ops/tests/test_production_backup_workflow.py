import unittest
from pathlib import Path


WORKFLOW = (Path(__file__).parents[2] / ".github/workflows/production-encrypted-backup.yml")


class ProductionBackupWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = WORKFLOW.read_text(encoding="utf-8")

    def test_is_opt_in_and_uses_protected_environment(self):
        self.assertIn("vars.ENABLE_PRODUCTION_BACKUPS == 'true'", self.source)
        self.assertIn("github.ref == 'refs/heads/main'", self.source)
        self.assertIn("environment: production-backup", self.source)
        self.assertIn("permissions:\n  contents: read", self.source)

    def test_encrypted_database_and_objects_precede_checkpoint(self):
        positions = [self.source.index(text) for text in (
            "--format=custom --no-owner --no-privileges",
            "age --recipient",
            "python -m ops.backup_production_objects",
            "python -m ops.kv_encrypted_backup upload",
            "INSERT INTO app.clinical_backup_checkpoints",
        )]
        self.assertEqual(positions, sorted(positions))
        self.assertIn("PGSSLMODE: verify-full", self.source)
        self.assertEqual(self.source.count("PGSSLROOTCERT: ${{ github.workspace }}/backend/psicogest/psicogest/certs/supabase-prod-ca-2021.crt"), 2)
        self.assertTrue((WORKFLOW.parents[2] / "backend/psicogest/psicogest/certs/supabase-prod-ca-2021.crt").is_file())
        self.assertIn("--schema=public --schema=app", self.source)

    def test_no_live_clinical_release_or_billing_configuration(self):
        self.assertNotIn("CLINICAL_DATA_ENABLED: true", self.source)
        self.assertNotIn("CLINICAL_DATA_RELEASE_APPROVED: true", self.source)
        self.assertNotIn("r2", self.source.lower())


if __name__ == "__main__":
    unittest.main()
