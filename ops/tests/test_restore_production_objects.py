import tempfile
import unittest
from pathlib import Path

from ops.backup_production_objects import BUCKET, PROJECT_REF
from ops.restore_production_objects import validate_configuration
from ops.restore_supabase_objects import RestoreValidationError


class ProductionRestoreConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.identity = Path(self.temp.name) / "identity.txt"
        self.identity.write_text("test-only", encoding="ascii")
        self.destination = "aaaaaaaaaaaaaaaaaaaa"
        self.env = {
            "RESTORE_DATA_CLASSIFICATION": "PRODUCTION_ISOLATED_RESTORE",
            "RESTORE_SUPABASE_PROJECT_REF": self.destination,
            "RESTORE_SUPABASE_S3_ENDPOINT": (
                f"https://{self.destination}.storage.supabase.co/storage/v1/s3"
            ),
            "RESTORE_SUPABASE_S3_BUCKET": BUCKET,
            "RESTORE_SUPABASE_S3_REGION": "us-east-1",
            "RESTORE_SUPABASE_S3_ACCESS_KEY_ID": "test-access-key",
            "RESTORE_SUPABASE_S3_SECRET_ACCESS_KEY": "test-secret-key",
            "RESTORE_AGE_IDENTITY_FILE": str(self.identity),
        }

    def test_accepts_a_different_isolated_project(self):
        self.assertEqual(validate_configuration(self.env), self.env)

    def test_rejects_production_target_and_wrong_endpoint(self):
        for name, value in (
            ("RESTORE_SUPABASE_PROJECT_REF", PROJECT_REF),
            ("RESTORE_SUPABASE_S3_ENDPOINT", "https://other.example"),
            ("RESTORE_DATA_CLASSIFICATION", "SYNTHETIC_ONLY"),
        ):
            with self.subTest(name=name):
                env = dict(self.env)
                env[name] = value
                with self.assertRaises(RestoreValidationError):
                    validate_configuration(env)


if __name__ == "__main__":
    unittest.main()
