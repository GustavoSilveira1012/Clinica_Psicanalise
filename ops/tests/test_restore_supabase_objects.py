import hashlib
import io
import json
import tarfile
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from ops.backup_supabase_objects import SYNTHETIC_BUCKET
from ops.restore_supabase_objects import (
    RestoreValidationError,
    apply_restore,
    existing_objects,
    inspect_archive,
    validate_restore_configuration,
    verify_ciphertext_checksum,
)


SOURCE_REF = "a" * 20
DESTINATION_REF = "b" * 20
OBJECT_KEY = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002"
OBJECT_BYTES = b"synthetic encrypted export object"


class FakeAgeDecryptProcess:
    archive_bytes = b""

    def __init__(self, _command, **_kwargs):
        self.stdout = io.BytesIO(self.archive_bytes)
        self.returncode = 0

    def wait(self, timeout=None):
        return self.returncode

    def poll(self):
        return self.returncode

    def terminate(self):
        self.returncode = -1


class SyntheticDestinationClient:
    def __init__(self, objects=None):
        self.objects = dict(objects or {})
        self.put_calls = []
        self.put_content_types = []
        self.before_put = None

    def get_paginator(self, _operation):
        client = self

        class Paginator:
            def paginate(self, **_kwargs):
                return iter([{"Contents": [
                    {"Key": key, "Size": len(value)} for key, value in client.objects.items()
                ]}])

        return Paginator()

    def get_object(self, Bucket, Key):
        return {"Body": io.BytesIO(self.objects[Key])}

    def head_object(self, Bucket, Key):
        if Key not in self.objects:
            error = RuntimeError("missing")
            error.response = {"ResponseMetadata": {"HTTPStatusCode": 404}}
            raise error
        return {"ContentLength": len(self.objects[Key])}

    def put_object(self, Bucket, Key, Body, IfNoneMatch=None, **_kwargs):
        if IfNoneMatch != "*":
            raise AssertionError("Restore must use an atomic create-only upload")
        if self.before_put is not None:
            self.before_put(self, Key)
        if Key in self.objects:
            error = RuntimeError("precondition failed")
            error.response = {"ResponseMetadata": {"HTTPStatusCode": 412}}
            raise error
        self.objects[Key] = bytes(Body)
        self.put_calls.append(Key)
        self.put_content_types.append(_kwargs.get("ContentType"))


def write_tar_archive(path: Path, *, classification="SYNTHETIC_ONLY") -> dict[str, object]:
    sha = hashlib.sha256(OBJECT_BYTES).hexdigest()
    manifest = {
        "format": "psicogest-supabase-objects-v1",
        "classification": classification,
        "project_ref": SOURCE_REF,
        "bucket": SYNTHETIC_BUCKET,
        "created_at": "2026-09-28T00:00:00+00:00",
        "objects": [{
            "key": OBJECT_KEY,
            "size": len(OBJECT_BYTES),
            "content_type": "application/octet-stream",
            "sha256": sha,
        }],
    }
    with tarfile.open(path, mode="w") as archive:
        obj = tarfile.TarInfo(f"objects/{OBJECT_KEY}")
        obj.size = len(OBJECT_BYTES)
        archive.addfile(obj, io.BytesIO(OBJECT_BYTES))
        encoded_manifest = json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode()
        entry = tarfile.TarInfo("manifest.json")
        entry.size = len(encoded_manifest)
        archive.addfile(entry, io.BytesIO(encoded_manifest))
    checksum = hashlib.sha256(path.read_bytes()).hexdigest()
    Path(str(path) + ".sha256").write_text(f"{checksum} *{path.name}\n", encoding="ascii")
    FakeAgeDecryptProcess.archive_bytes = path.read_bytes()
    return manifest


def restore_environment(identity_file: Path):
    return {
        "RESTORE_DATA_CLASSIFICATION": "SYNTHETIC_ONLY",
        "RESTORE_SUPABASE_PROJECT_REF": DESTINATION_REF,
        "RESTORE_SUPABASE_S3_ENDPOINT": (
            f"https://{DESTINATION_REF}.storage.supabase.co/storage/v1/s3"
        ),
        "RESTORE_SUPABASE_S3_BUCKET": SYNTHETIC_BUCKET,
        "RESTORE_SUPABASE_S3_REGION": "sa-east-1",
        "RESTORE_SUPABASE_S3_ACCESS_KEY_ID": "synthetic-access-key",
        "RESTORE_SUPABASE_S3_SECRET_ACCESS_KEY": "synthetic-secret-key",
        "RESTORE_AGE_IDENTITY_FILE": str(identity_file),
        "RESTORE_CONFIRMATION": "",
    }


class SyntheticObjectRestoreSafetyTest(unittest.TestCase):
    def test_requires_a_different_destination_project_and_exact_endpoint(self):
        with tempfile.TemporaryDirectory() as directory:
            identity = Path(directory) / "identity.txt"
            identity.write_text("AGE-SECRET-KEY-TEST-ONLY", encoding="ascii")
            env = restore_environment(identity)
            self.assertEqual(validate_restore_configuration(env, SOURCE_REF)["project_ref"], DESTINATION_REF)

            env["RESTORE_SUPABASE_PROJECT_REF"] = SOURCE_REF
            env["RESTORE_SUPABASE_S3_ENDPOINT"] = (
                f"https://{SOURCE_REF}.storage.supabase.co/storage/v1/s3"
            )
            with self.assertRaises(RestoreValidationError):
                validate_restore_configuration(env, SOURCE_REF)

    def test_requires_synthetic_classification_and_confirmation_vocabulary(self):
        with tempfile.TemporaryDirectory() as directory:
            identity = Path(directory) / "identity.txt"
            identity.write_text("AGE-SECRET-KEY-TEST-ONLY", encoding="ascii")
            env = restore_environment(identity)
            env["RESTORE_DATA_CLASSIFICATION"] = "PRODUCTION"
            with self.assertRaises(RestoreValidationError):
                validate_restore_configuration(env, SOURCE_REF)
            env["RESTORE_DATA_CLASSIFICATION"] = "SYNTHETIC_ONLY"
            env["RESTORE_CONFIRMATION"] = "DELETE-ORIGINAL"
            with self.assertRaises(RestoreValidationError):
                validate_restore_configuration(env, SOURCE_REF)

    def test_preflight_checks_checksum_source_project_and_object_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory) / "synthetic.tar.age"
            manifest = write_tar_archive(archive)
            with patch("ops.restore_supabase_objects.subprocess.Popen", FakeAgeDecryptProcess):
                validated = inspect_archive(
                    archive,
                    identity_file="synthetic-identity",
                    expected_source_project_ref=SOURCE_REF,
                )
                self.assertEqual(validated, manifest)

                with self.assertRaises(RestoreValidationError):
                    inspect_archive(
                        archive,
                        identity_file="synthetic-identity",
                        expected_source_project_ref=DESTINATION_REF,
                    )

    def test_checksum_tampering_is_rejected_before_decryption(self):
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory) / "synthetic.tar.age"
            write_tar_archive(archive)
            archive.write_bytes(archive.read_bytes() + b"tampered")
            with self.assertRaises(RestoreValidationError):
                verify_ciphertext_checksum(archive)

    def test_destination_must_be_empty_or_match_a_partial_prior_restore(self):
        manifest = {
            "objects": [{"key": OBJECT_KEY, "size": len(OBJECT_BYTES),
                         "content_type": "application/octet-stream",
                         "sha256": hashlib.sha256(OBJECT_BYTES).hexdigest()}]
        }
        client = SyntheticDestinationClient()
        self.assertEqual(existing_objects(client, SYNTHETIC_BUCKET, manifest), set())
        client.objects[OBJECT_KEY] = b"different bytes"
        with self.assertRaises(RestoreValidationError):
            existing_objects(client, SYNTHETIC_BUCKET, manifest)

    def test_restore_adds_only_missing_objects_and_never_deletes(self):
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory) / "synthetic.tar.age"
            manifest = write_tar_archive(archive)
            client = SyntheticDestinationClient()
            with patch("ops.restore_supabase_objects.subprocess.Popen", FakeAgeDecryptProcess):
                restored = apply_restore(
                    client,
                    bucket=SYNTHETIC_BUCKET,
                    manifest=manifest,
                    archive_path=archive,
                    identity_file="synthetic-identity",
                )
            self.assertEqual(restored, (1, len(OBJECT_BYTES)))
            self.assertEqual(client.put_calls, [OBJECT_KEY])
            self.assertEqual(client.objects, {OBJECT_KEY: OBJECT_BYTES})
            self.assertEqual(client.put_content_types, ["application/octet-stream"])

            # An identical partial restore is idempotent and performs no second write.
            with patch("ops.restore_supabase_objects.subprocess.Popen", FakeAgeDecryptProcess):
                restored_again = apply_restore(
                    client,
                    bucket=SYNTHETIC_BUCKET,
                    manifest=manifest,
                    archive_path=archive,
                    identity_file="synthetic-identity",
                )
            self.assertEqual(restored_again, (0, 0))
            self.assertEqual(client.put_calls, [OBJECT_KEY])

    def test_conditional_put_does_not_overwrite_a_concurrent_writer(self):
        with tempfile.TemporaryDirectory() as directory:
            archive = Path(directory) / "synthetic.tar.age"
            manifest = write_tar_archive(archive)
            client = SyntheticDestinationClient()

            def concurrent_write(destination, key):
                destination.objects[key] = b"written concurrently"

            client.before_put = concurrent_write
            with patch("ops.restore_supabase_objects.subprocess.Popen", FakeAgeDecryptProcess):
                with self.assertRaisesRegex(RuntimeError, "precondition failed"):
                    apply_restore(
                        client,
                        bucket=SYNTHETIC_BUCKET,
                        manifest=manifest,
                        archive_path=archive,
                        identity_file="synthetic-identity",
                    )
            self.assertEqual(client.objects[OBJECT_KEY], b"written concurrently")
            self.assertEqual(client.put_calls, [])


if __name__ == "__main__":
    unittest.main()
