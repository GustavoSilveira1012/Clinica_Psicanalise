import tempfile
import unittest
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import patch

from ops import kv_encrypted_backup as backup


class MemoryKv:
    def __init__(self):
        self.values = {}
        self.metadata = {}
        self.writes = []

    def list_keys(self):
        return [{"name": key, "metadata": value} for key, value in self.metadata.items()]

    def put(self, key, value, size):
        self.values[key] = value
        self.metadata[key] = {"size": size}
        self.writes.append(key)

    def get(self, key):
        return self.values[key]


class EncryptedKvBackupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.db = self.root / "database.age"
        self.objects = self.root / "objects.age"
        self.db.write_bytes(b"age-encryption.org/v1\nciphertext-database")
        self.objects.write_bytes(b"age-encryption.org/v1\nciphertext-objects")
        self.client = MemoryKv()
        self.started = datetime.now(timezone.utc) - timedelta(minutes=3)

    def upload(self):
        return backup.upload(self.client, self.db, self.objects,
                             project_ref="gdorfdcvajeczzaesjjq",
                             snapshot_started_at=self.started)

    def test_round_trip_keeps_both_archives_and_publishes_manifest_last(self):
        with patch.object(backup, "CHUNK_BYTES", 12):
            key = self.upload()
            self.assertEqual(key, self.client.writes[-1])
            self.assertTrue(key.endswith("/manifest"))
            manifest = backup.download(self.client, key, self.root / "restore")
        self.assertEqual(manifest["project_ref"], "gdorfdcvajeczzaesjjq")
        self.assertEqual((self.root / "restore/database.age").read_bytes(), self.db.read_bytes())
        self.assertEqual((self.root / "restore/objects.age").read_bytes(), self.objects.read_bytes())
        self.assertTrue((self.root / "restore/database.age.sha256").is_file())
        self.assertTrue((self.root / "restore/objects.age.sha256").is_file())

    def test_fails_closed_before_upload_when_capacity_is_exhausted(self):
        self.client.metadata["other"] = {"size": backup.MAX_NAMESPACE_BYTES}
        with self.assertRaisesRegex(backup.BackupError, "safety budget"):
            self.upload()
        self.assertEqual(self.client.writes, [])

    def test_fails_closed_on_unknown_existing_key_size(self):
        self.client.metadata["other"] = {}
        with self.assertRaisesRegex(backup.BackupError, "unknown size"):
            self.upload()

    def test_refuses_plaintext_even_if_renamed_age(self):
        self.db.write_bytes(b"plaintext")
        with self.assertRaisesRegex(backup.BackupError, "not an age"):
            self.upload()

    def test_rejects_too_many_chunk_writes(self):
        with patch.object(backup, "CHUNK_BYTES", 1):
            with self.assertRaisesRegex(backup.BackupError, "too many KV writes"):
                self.upload()
        self.assertEqual(self.client.writes, [])

    def test_failed_remote_verification_never_publishes_manifest(self):
        def broken_get(key):
            return b"tampered"
        self.client.get = broken_get
        with patch.object(backup, "time") as timer:
            with self.assertRaisesRegex(backup.BackupError, "not readable"):
                self.upload()
        self.assertFalse(any(key.endswith("/manifest") for key in self.client.writes))
        self.assertEqual(timer.sleep.call_count, 34)

    def test_rejects_tampered_ciphertext_on_restore(self):
        key = self.upload()
        part_key = next(k for k in self.client.values if k.endswith("/database/000"))
        self.client.values[part_key] = b"tampered"
        with patch.object(backup, "time"):
            with self.assertRaisesRegex(backup.BackupError, "not readable"):
                backup.download(self.client, key, self.root / "restore")

    def test_restore_refuses_another_project(self):
        key = self.upload()
        with self.assertRaisesRegex(backup.BackupError, "different Supabase project"):
            backup.download(self.client, key, self.root / "restore",
                            expected_project_ref="aaaaaaaaaaaaaaaaaaaa")

    def test_http_write_uses_multipart_metadata_required_for_capacity_accounting(self):
        client = backup.CloudflareKv("a" * 32, "b" * 32, "test-token")
        with patch.object(client, "_request", return_value=b'{"success":true}') as request:
            client.put("psicogest/v1/test", b"encrypted", 9)
        method, suffix, body, content_type = request.call_args.args
        self.assertEqual(method, "PUT")
        self.assertIn("expiration_ttl=172800", suffix)
        self.assertTrue(content_type.startswith("multipart/form-data; boundary="))
        self.assertIn(b'name="metadata"', body)
        self.assertIn(json.dumps({"size": 9}, separators=(",", ":")).encode(), body)
        self.assertIn(b"encrypted", body)


if __name__ == "__main__":
    unittest.main()
