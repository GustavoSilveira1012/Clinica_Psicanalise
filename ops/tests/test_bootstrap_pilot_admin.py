"""Safety checks for the one-time first-administrator bootstrap."""

import importlib.util
import unittest
from pathlib import Path
from unittest.mock import MagicMock


SCRIPT = Path(__file__).resolve().parents[1] / "bootstrap_pilot_admin.py"
spec = importlib.util.spec_from_file_location("bootstrap_pilot_admin", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def valid_env():
    return {
        "PILOT_ADMIN_BOOTSTRAP_CONFIRMATION": module.CONFIRMATION,
        "PSICOGEST_SUPABASE_DB_PASSWORD": "db-password-unique-12345",
        "PILOT_ADMIN_PASSWORD": "user-password-unique-12345",
        "PILOT_ADMIN_NAME": "Operador Nominal",
        "PILOT_ADMIN_EMAIL": "operator@example.invalid",
    }


class BootstrapPilotAdminTest(unittest.TestCase):
    def test_confirmation_and_private_distinct_passwords_are_required(self):
        env = valid_env()
        env.pop("PILOT_ADMIN_BOOTSTRAP_CONFIRMATION")
        with self.assertRaises(module.BootstrapError):
            module.validate(env)
        env = valid_env()
        env["PILOT_ADMIN_PASSWORD"] = env["PSICOGEST_SUPABASE_DB_PASSWORD"]
        with self.assertRaises(module.BootstrapError):
            module.validate(env)

    def test_rejects_bcrypt_truncation_and_non_nominal_identity(self):
        env = valid_env()
        env["PILOT_ADMIN_PASSWORD"] = "a" * 73
        with self.assertRaises(module.BootstrapError):
            module.validate(env)
        env = valid_env()
        env["PILOT_ADMIN_NAME"] = " "
        with self.assertRaises(module.BootstrapError):
            module.validate(env)

    def test_refuses_nonempty_database_without_insert(self):
        connection = MagicMock()
        cursor = connection.__enter__.return_value.cursor.return_value.__enter__.return_value
        cursor.fetchone.return_value = (1,)
        psycopg = MagicMock()
        psycopg.connect.return_value = connection
        bcrypt = MagicMock()
        bcrypt.hashpw.return_value = b"$2b$12$synthetic-hash"

        with self.assertRaises(module.BootstrapError):
            module.bootstrap(psycopg, bcrypt, valid_env())
        self.assertEqual(cursor.execute.call_count, 2)
        self.assertEqual(psycopg.connect.call_args.kwargs["sslmode"], "verify-full")

    def test_inserts_only_when_database_empty(self):
        connection = MagicMock()
        cursor = connection.__enter__.return_value.cursor.return_value.__enter__.return_value
        cursor.fetchone.side_effect = [(0,), (42,)]
        psycopg = MagicMock()
        psycopg.connect.return_value = connection
        bcrypt = MagicMock()
        bcrypt.hashpw.return_value = b"$2b$12$synthetic-hash"

        self.assertEqual(module.bootstrap(psycopg, bcrypt, valid_env()), 42)
        self.assertEqual(cursor.execute.call_count, 3)
        query, params = cursor.execute.call_args.args
        self.assertIn("'SYSTEM_ADMIN'::public.user_role", query)
        self.assertEqual(params[0:2], ("Operador Nominal", "operator@example.invalid"))
        self.assertEqual(params[2], "$2b$12$synthetic-hash")


if __name__ == "__main__":
    unittest.main()
