import unittest

from ops import backup_production_objects as production
from ops.backup_supabase_objects import ConfigurationError


class ProductionObjectConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.env = {
            "BACKUP_DATA_CLASSIFICATION": "PRODUCTION_CLINICAL",
            "BACKUP_SUPABASE_PROJECT_REF": production.PROJECT_REF,
            "BACKUP_SUPABASE_S3_ENDPOINT": (
                f"https://{production.PROJECT_REF}.storage.supabase.co/storage/v1/s3"
            ),
            "BACKUP_SUPABASE_S3_BUCKET": production.BUCKET,
            "BACKUP_SUPABASE_S3_REGION": "us-east-1",
            "BACKUP_SUPABASE_S3_ACCESS_KEY_ID": "test-access-key",
            "BACKUP_SUPABASE_S3_SECRET_ACCESS_KEY": "test-secret-key",
            "BACKUP_AGE_RECIPIENT": "age1" + "q" * 58,
        }

    def test_approved_production_identifiers(self):
        self.assertEqual(production.validate_configuration(self.env), self.env)

    def test_rejects_other_project_bucket_endpoint_or_classification(self):
        for name, value in (
            ("BACKUP_DATA_CLASSIFICATION", "SYNTHETIC_ONLY"),
            ("BACKUP_SUPABASE_PROJECT_REF", "aaaaaaaaaaaaaaaaaaaa"),
            ("BACKUP_SUPABASE_S3_BUCKET", "other"),
            ("BACKUP_SUPABASE_S3_ENDPOINT", "https://other.example"),
        ):
            with self.subTest(name=name):
                env = dict(self.env)
                env[name] = value
                with self.assertRaises(ConfigurationError):
                    production.validate_configuration(env)


if __name__ == "__main__":
    unittest.main()
