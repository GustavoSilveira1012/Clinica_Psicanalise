import importlib.util
from pathlib import Path
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "configure_supabase_role_passwords.py"
spec = importlib.util.spec_from_file_location("role_passwords", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class RolePasswordTests(unittest.TestCase):
    def test_missing_secret_error_names_only_the_missing_key(self):
        with self.assertRaises(module.ConfigurationError) as error:
            module.read_secrets({
                "PSICOGEST_RUNTIME_DB_PASSWORD": "A" * 32,
                "BACKUP_SUPABASE_DATABASE_PASSWORD": "B" * 32,
            })
        self.assertEqual(
            str(error.exception),
            "Missing repository secrets: PSICOGEST_SUPABASE_DB_PASSWORD",
        )

    def test_scram_verifier_is_deterministic_for_given_salt(self):
        verifier = module.scram_verifier("A" * 32, bytes(range(16)))
        self.assertEqual(
            verifier,
            "SCRAM-SHA-256$4096:AAECAwQFBgcICQoLDA0ODw==$"
            "355HF2woQrDOhRxNERGHA3h4zRZKzRmQ0VrawXpeRxQ=:"
            "scGvEICb0PIcJDvEa8hxDlCgyBzb2cWy3JRZ0MSW52Q=",
        )

    def test_missing_or_reused_password_is_rejected(self):
        env = {
            "PSICOGEST_SUPABASE_DB_PASSWORD": "admin-password",
            "PSICOGEST_RUNTIME_DB_PASSWORD": "A" * 32,
            "BACKUP_SUPABASE_DATABASE_PASSWORD": "A" * 32,
        }
        with self.assertRaises(ValueError):
            module.read_secrets(env)
        env["BACKUP_SUPABASE_DATABASE_PASSWORD"] = "B" * 32
        _, values = module.read_secrets(env)
        self.assertEqual(set(values), set(module.ROLE_ENV))

    def test_short_or_nonascii_password_is_rejected(self):
        env = {
            "PSICOGEST_SUPABASE_DB_PASSWORD": "admin-password",
            "PSICOGEST_RUNTIME_DB_PASSWORD": "A" * 32,
            "BACKUP_SUPABASE_DATABASE_PASSWORD": "B" * 15,
        }
        with self.assertRaises(ValueError):
            module.read_secrets(env)
        env["BACKUP_SUPABASE_DATABASE_PASSWORD"] = "é" * 32
        with self.assertRaises(ValueError):
            module.read_secrets(env)

    def test_all_invalid_role_secrets_are_reported_without_values(self):
        env = {
            "PSICOGEST_SUPABASE_DB_PASSWORD": "admin-password",
            "PSICOGEST_RUNTIME_DB_PASSWORD": "too short",
            "BACKUP_SUPABASE_DATABASE_PASSWORD": "é" * 32,
        }
        with self.assertRaises(module.ConfigurationError) as error:
            module.read_secrets(env)
        message = str(error.exception)
        self.assertIn("PSICOGEST_RUNTIME_DB_PASSWORD", message)
        self.assertIn("BACKUP_SUPABASE_DATABASE_PASSWORD", message)
        self.assertNotIn("too short", message)
        self.assertNotIn("é", message)


if __name__ == "__main__":
    unittest.main()
