"""Local restore drill must select the approved backup and avoid source writes."""

import json
import unittest
from unittest.mock import Mock, patch

from ops import production_restore_drill_form as form


class RestoreDrillFormTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
