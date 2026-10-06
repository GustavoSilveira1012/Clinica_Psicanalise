import importlib.util
from pathlib import Path
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "configure_supabase_role_passwords.py"
spec = importlib.util.spec_from_file_location("role_passwords", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class RolePasswordTests(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
