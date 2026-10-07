"""Local restore drill must select the approved backup and avoid source writes."""

import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

from ops import production_restore_drill_form as form


class RestoreDrillFormTest(unittest.TestCase):
    def test_loopback_origin_accepts_in_app_browser_null_only_with_nonce_gate(self):
        expected = "http://127.0.0.1:59779"
        self.assertTrue(form.allowed_origin(expected, expected))
        self.assertTrue(form.allowed_origin("null", expected))
        self.assertFalse(form.allowed_origin("https://attacker.example", expected))

    def test_error_description_never_echoes_unknown_provider_text(self):
        self.assertEqual(form.safe_error_description(RuntimeError("A chave age é válida, mas pertence a outro par")),
                         "A chave age é válida, mas pertence a outro par")
        self.assertEqual(form.safe_error_description(RuntimeError("secret token from provider")), "RuntimeError")

    def test_restore_error_classifier_never_echoes_database_text(self):
        self.assertEqual(form.safe_pg_restore_failure(
            b'ERROR: role "sensitive-user" does not exist; patient secret'),
            "pg_restore falhou por role ausente no banco local")
        self.assertEqual(form.safe_pg_restore_failure(b'ERROR: unknown patient note'),
                         "pg_restore não conseguiu aplicar o dump no PostgreSQL 17 local")

    def test_private_key_line_is_extracted_without_comment_or_formatting(self):
        fake_key = "AGE-SECRET-KEY-" + "A" * 59
        self.assertEqual(form.extract_age_secret_line("# public key: age1example\n```\n" + fake_key + "\n```"), fake_key)
        self.assertEqual(form.extract_age_secret_line(fake_key + "Z"), fake_key)
        with self.assertRaises(RuntimeError):
            form.extract_age_secret_line(fake_key + "\n" + fake_key)

    def test_selects_latest_manifest_for_fixed_project(self):
        older = "psicogest/v1/" + "a" * 32 + "/manifest"
        newer = "psicogest/v1/" + "b" * 32 + "/manifest"
        unrelated = "psicogest/v1/" + "c" * 32 + "/manifest"
        records = {
            older: {"format": "psicogest-encrypted-backup-v1", "project_ref": form.PROJECT_REF,
                    "snapshot_started_at": "2026-10-07T20:00:00+00:00"},
            newer: {"format": "psicogest-encrypted-backup-v1", "project_ref": form.PROJECT_REF,
                    "snapshot_started_at": "2026-10-07T20:20:00+00:00"},
            unrelated: {"format": "psicogest-encrypted-backup-v1", "project_ref": "z" * 20,
                        "snapshot_started_at": "2026-10-07T20:40:00+00:00"},
        }
        client = Mock()
        client.list_keys.return_value = [{"name": key} for key in records] + [{"name": "unrelated"}]
        client.get.side_effect = lambda key: json.dumps(records[key]).encode()
        self.assertEqual(form.select_latest_manifest(client), newer)

    def test_never_builds_a_form_to_restore_to_supabase(self):
        content = form.page("Aguardando credenciais.", "nonce").decode()
        self.assertIn("PostgreSQL 17 temporário", content)
        self.assertIn("isolado da rede", content)
        self.assertNotIn("RESTORE_SUPABASE_S3_SECRET_ACCESS_KEY", content)

    def test_local_identity_form_does_not_request_a_copied_private_key(self):
        content = form.page("Aguardando credenciais.", "nonce", local_identity=True).decode()
        self.assertIn("arquivo local já validado", content)
        self.assertNotIn('name="identity"', content)
        self.assertIn('name="token"', content)

    def test_cached_replay_rejects_tampered_ciphertext_before_opening_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "database.age").write_bytes(b"corrupted")
            (root / "objects.age").write_bytes(b"ciphertext")
            (root / "manifest.json").write_text(json.dumps({
                "format": "psicogest-encrypted-backup-v1",
                "project_ref": form.PROJECT_REF,
                "database": {"sha256": "0" * 64, "bytes": 9},
                "objects": {"sha256": "0" * 64, "bytes": 10},
            }), encoding="utf-8")
            with self.assertRaisesRegex(RuntimeError, "verificação"):
                form.replay_cached_restore(root, root / "missing-identity", root / "age", root / "keygen")

    def test_restore_container_has_no_network_or_persistent_volume(self):
        calls = []

        def fake_run(args, **_kwargs):
            calls.append(args)
            if args[:2] == ["docker", "run"]:
                return Mock(returncode=1)
            return Mock(returncode=0)

        with patch.object(form, "checked_run", side_effect=fake_run):
            with self.assertRaisesRegex(RuntimeError, "banco local isolado"):
                form.restore_database_to_ram_container(Mock(), Mock(), Mock())
        command = calls[0]
        self.assertEqual(command[command.index("--network") + 1], "none")
        self.assertEqual(command.count("--tmpfs"), 2)
        self.assertNotIn("-p", command)
        self.assertNotIn("--mount", command)

    def test_restore_prepares_supabase_policy_roles_only_in_isolated_container(self):
        source = form.Path(form.__file__).read_text(encoding="utf-8")
        for name in ("psicogest_runtime", "psicogest_backup", "anon", "authenticated", "service_role"):
            self.assertIn(f"CREATE ROLE {name}", source)
        self.assertIn('"docker", "exec", name, "psql"', source)

    def test_restore_installs_missing_extension_between_archive_sections(self):
        source = form.Path(form.__file__).read_text(encoding="utf-8")
        self.assertIn('for section in ("pre-data", "data", "post-data")', source)
        self.assertIn('"CREATE EXTENSION IF NOT EXISTS btree_gist"', source)
        self.assertIn('"--section=" + section', source)
        self.assertIn('if section == "pre-data":', source)


if __name__ == "__main__":
    unittest.main()
