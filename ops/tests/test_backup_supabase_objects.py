import hashlib
import io
import json
import tarfile
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path
from unittest.mock import patch

from ops.backup_supabase_objects import (
    ConfigurationError,
    MAX_OBJECT_BYTES,
    SYNTHETIC_BUCKET,
    create_encrypted_backup,
    list_objects,
    validate_configuration,
    validate_object_key,
)


def valid_environment():
    project_ref = "a" * 20
    return {
        "BACKUP_DATA_CLASSIFICATION": "SYNTHETIC_ONLY",
        "BACKUP_SUPABASE_PROJECT_REF": project_ref,
        "BACKUP_SUPABASE_S3_ENDPOINT": (
            f"https://{project_ref}.storage.supabase.co/storage/v1/s3"
        ),
        "BACKUP_SUPABASE_S3_BUCKET": SYNTHETIC_BUCKET,
        "BACKUP_SUPABASE_S3_REGION": "sa-east-1",
        "BACKUP_SUPABASE_S3_ACCESS_KEY_ID": "synthetic-access-key",
        "BACKUP_SUPABASE_S3_SECRET_ACCESS_KEY": "synthetic-secret-key",
        "BACKUP_AGE_RECIPIENT": "age1" + "a" * 24,
    }


class FakePaginator:
    def __init__(self, pages):
        self.pages = pages

    def paginate(self, **_kwargs):
        return iter(self.pages)


class FakeS3Client:
    def __init__(self, pages, bodies=None):
        self.pages = pages
        self.bodies = bodies or {}

    def get_paginator(self, _operation):
        return FakePaginator(self.pages)

    def get_object(self, Bucket, Key):
        self.assert_bucket = Bucket
        return {"Body": io.BytesIO(self.bodies[Key]), "ContentType": "application/octet-stream"}


class FakeAgeInput(io.BytesIO):
    def __init__(self, output_path):
        super().__init__()
        self.output_path = Path(output_path)

    def close(self):
        if not self.closed:
            self.output_path.write_bytes(self.getvalue())
        super().close()


class FakeAgeProcess:
    def __init__(self, command, **_kwargs):
        output_path = command[command.index("--output") + 1]
        self.stdin = FakeAgeInput(output_path)
        self.stderr = io.BytesIO()
        self.returncode = 0

    def poll(self):
        return self.returncode

    def wait(self, timeout=None):
        return self.returncode

    def terminate(self):
        self.returncode = -1


class SupabaseSyntheticBackupValidationTest(unittest.TestCase):
    def test_accepts_only_exact_synthetic_target(self):
        config = validate_configuration(valid_environment())
        self.assertEqual(config["bucket"], SYNTHETIC_BUCKET)

    def test_rejects_non_synthetic_classification(self):
        env = valid_environment()
        env["BACKUP_DATA_CLASSIFICATION"] = "PRODUCTION"
        with self.assertRaises(ConfigurationError):
            validate_configuration(env)

    def test_rejects_endpoint_not_bound_to_configured_project(self):
        env = valid_environment()
        env["BACKUP_SUPABASE_S3_ENDPOINT"] = (
            "https://" + "b" * 20 + ".storage.supabase.co/storage/v1/s3"
        )
        with self.assertRaises(ConfigurationError):
            validate_configuration(env)

    def test_rejects_other_bucket_even_when_endpoint_is_synthetic(self):
        env = valid_environment()
        env["BACKUP_SUPABASE_S3_BUCKET"] = "clinical-exports"
        with self.assertRaises(ConfigurationError):
            validate_configuration(env)

    def test_accepts_only_tenant_scoped_uuid_object_names(self):
        validate_object_key("00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002")
        for key in (
            "../secret",
            "/absolute/path",
            "patient-name/document.pdf",
            "00000000-0000-0000-0000-000000000001\\object",
        ):
            with self.subTest(key=key), self.assertRaises(ValueError):
                validate_object_key(key)

    def test_lists_only_valid_nonempty_objects(self):
        key = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002"
        result = list_objects(
            FakeS3Client([{"Contents": [{"Key": key, "Size": 12}]}]),
            SYNTHETIC_BUCKET,
        )
        self.assertEqual(result, [{"key": key, "size": 12}])

    def test_fails_closed_when_bucket_contains_unexpected_object(self):
        client = FakeS3Client([{"Contents": [{"Key": "unexpected-name.pdf", "Size": 12}]}])
        with self.assertRaises(ValueError):
            list_objects(client, SYNTHETIC_BUCKET)

    def test_rejects_empty_objects(self):
        key = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002"
        client = FakeS3Client([{"Contents": [{"Key": key, "Size": 0}]}])
        with self.assertRaises(ValueError):
            list_objects(client, SYNTHETIC_BUCKET)

    def test_rejects_objects_over_restore_memory_limit(self):
        key = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002"
        client = FakeS3Client([{"Contents": [{"Key": key, "Size": MAX_OBJECT_BYTES + 1}]}])
        with self.assertRaisesRegex(ValueError, "oversized"):
            list_objects(client, SYNTHETIC_BUCKET)

    def test_streams_objects_to_age_and_writes_a_verifiable_manifest(self):
        key = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002"
        content = b"synthetic encrypted export bytes"
        client = FakeS3Client(
            [{"Contents": [{"Key": key, "Size": len(content)}]}],
            {key: content},
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            archive_path = Path(temp_dir) / "synthetic.tar.age"
            with patch("ops.backup_supabase_objects.subprocess.Popen", FakeAgeProcess):
                count, total_bytes = create_encrypted_backup(
                    client,
                    bucket=SYNTHETIC_BUCKET,
                    project_ref="a" * 20,
                    recipient="age1" + "a" * 24,
                    output_path=archive_path,
                    now=datetime(2026, 9, 28, tzinfo=timezone.utc),
                )

            self.assertEqual((count, total_bytes), (1, len(content)))
            with tarfile.open(archive_path, mode="r:") as archive:
                stored_object = archive.extractfile(f"objects/{key}")
                self.assertEqual(stored_object.read(), content)
                manifest = json.load(archive.extractfile("manifest.json"))
            self.assertEqual(manifest["classification"], "SYNTHETIC_ONLY")
            self.assertEqual(manifest["project_ref"], "a" * 20)
            self.assertEqual(manifest["objects"][0]["sha256"], hashlib.sha256(content).hexdigest())
            self.assertEqual(manifest["objects"][0]["content_type"], "application/octet-stream")


if __name__ == "__main__":
    unittest.main()
