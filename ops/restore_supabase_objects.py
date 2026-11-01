"""Restore an encrypted synthetic Supabase Storage archive to a different empty test project."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tarfile
from pathlib import Path
from typing import BinaryIO

from ops.backup_supabase_objects import (
    MAX_OBJECTS,
    MAX_OBJECT_BYTES,
    MAX_TOTAL_BYTES,
    SYNTHETIC_BUCKET,
    PROJECT_REF_RE,
    validate_object_key,
)


MAX_MANIFEST_BYTES = 16_000_000
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


class RestoreValidationError(ValueError):
    pass


def validate_restore_configuration(env: dict[str, str], source_project_ref: str) -> dict[str, str]:
    if env.get("RESTORE_DATA_CLASSIFICATION") != "SYNTHETIC_ONLY":
        raise RestoreValidationError("RESTORE_DATA_CLASSIFICATION must be SYNTHETIC_ONLY")
    if not PROJECT_REF_RE.fullmatch(source_project_ref):
        raise RestoreValidationError("Source project ref is missing or invalid")

    project_ref = env.get("RESTORE_SUPABASE_PROJECT_REF", "")
    if not PROJECT_REF_RE.fullmatch(project_ref) or project_ref == source_project_ref:
        raise RestoreValidationError("Restore destination must be a different synthetic project")
    endpoint = env.get("RESTORE_SUPABASE_S3_ENDPOINT", "").rstrip("/")
    expected_endpoint = f"https://{project_ref}.storage.supabase.co/storage/v1/s3"
    if endpoint != expected_endpoint:
        raise RestoreValidationError("S3 endpoint does not match the configured destination project")
    bucket = env.get("RESTORE_SUPABASE_S3_BUCKET", "")
    if bucket != SYNTHETIC_BUCKET:
        raise RestoreValidationError("Only the fixed synthetic bucket is allowed")
    region = env.get("RESTORE_SUPABASE_S3_REGION", "")
    access_key = env.get("RESTORE_SUPABASE_S3_ACCESS_KEY_ID", "")
    secret_key = env.get("RESTORE_SUPABASE_S3_SECRET_ACCESS_KEY", "")
    identity_file = env.get("RESTORE_AGE_IDENTITY_FILE", "")
    if not all((region, access_key, secret_key, identity_file)):
        raise RestoreValidationError("Destination credentials and age identity file are required")
    if not Path(identity_file).is_file():
        raise RestoreValidationError("Age identity file is unavailable")
    confirmation = env.get("RESTORE_CONFIRMATION", "")
    if confirmation not in ("", "RESTORE-SYNTHETIC"):
        raise RestoreValidationError("Restore confirmation is invalid")
    return {
        "project_ref": project_ref,
        "endpoint": endpoint,
        "bucket": bucket,
        "region": region,
        "access_key": access_key,
        "secret_key": secret_key,
        "identity_file": identity_file,
        "confirmation": confirmation,
    }


def verify_ciphertext_checksum(archive_path: Path) -> None:
    if archive_path.suffix != ".age" or not archive_path.is_file():
        raise RestoreValidationError("Input must be an encrypted .age archive")
    checksum_path = Path(str(archive_path) + ".sha256")
    if not checksum_path.is_file():
        raise RestoreValidationError("Encrypted archive checksum is missing")
    parts = checksum_path.read_text(encoding="ascii").strip().split()
    if len(parts) != 2 or not SHA256_RE.fullmatch(parts[0]):
        raise RestoreValidationError("Encrypted archive checksum manifest is malformed")
    if parts[1].lstrip("*") != archive_path.name:
        raise RestoreValidationError("Encrypted archive checksum names a different file")
    digest = hashlib.sha256()
    with archive_path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    if digest.hexdigest() != parts[0]:
        raise RestoreValidationError("Encrypted archive checksum does not match")


def _decrypted_archive(
    archive_path: Path,
    identity_file: str,
    *,
    age_executable: str,
):
    process = subprocess.Popen(
        [age_executable, "--decrypt", "--identity", identity_file, str(archive_path)],
        stdin=subprocess.DEVNULL,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )
    if process.stdout is None:
        process.terminate()
        process.wait(timeout=10)
        raise RuntimeError("age decryption did not provide a stream")
    return process


def inspect_archive(
    archive_path: Path,
    *,
    identity_file: str,
    expected_source_project_ref: str,
    age_executable: str = "age",
    expected_classification: str = "SYNTHETIC_ONLY",
    expected_bucket: str = SYNTHETIC_BUCKET,
) -> dict[str, object]:
    verify_ciphertext_checksum(archive_path)
    process = _decrypted_archive(archive_path, identity_file, age_executable=age_executable)
    assert process.stdout is not None
    observed: list[dict[str, object]] = []
    manifest_data: bytes | None = None
    total_bytes = 0
    try:
        with tarfile.open(fileobj=process.stdout, mode="r|") as archive:
            for member in archive:
                if not member.isfile() or member.issym() or member.islnk():
                    raise RestoreValidationError("Archive contains a non-regular member")
                if member.name == "manifest.json":
                    if manifest_data is not None or member.size > MAX_MANIFEST_BYTES:
                        raise RestoreValidationError("Archive manifest is duplicate or too large")
                    source = archive.extractfile(member)
                    if source is None:
                        raise RestoreValidationError("Archive manifest cannot be read")
                    manifest_data = source.read(MAX_MANIFEST_BYTES + 1)
                    if len(manifest_data) != member.size:
                        raise RestoreValidationError("Archive manifest length is inconsistent")
                    continue
                if manifest_data is not None or not member.name.startswith("objects/"):
                    raise RestoreValidationError("Archive contains an unexpected path")
                key = member.name.removeprefix("objects/")
                validate_object_key(key)
                if member.size < 1 or member.size > MAX_OBJECT_BYTES:
                    raise RestoreValidationError("Archive object has an invalid size")
                if any(item["key"] == key for item in observed):
                    raise RestoreValidationError("Archive contains a duplicate object")
                total_bytes += member.size
                if len(observed) >= MAX_OBJECTS or total_bytes > MAX_TOTAL_BYTES:
                    raise RestoreValidationError("Archive exceeds the configured safety limits")
                stream = archive.extractfile(member)
                if stream is None:
                    raise RestoreValidationError("Archive object cannot be read")
                digest = hashlib.sha256()
                bytes_read = 0
                with stream:
                    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                        digest.update(chunk)
                        bytes_read += len(chunk)
                if bytes_read != member.size:
                    raise RestoreValidationError("Archive object length is inconsistent")
                observed.append({"key": key, "size": bytes_read, "sha256": digest.hexdigest()})
                observed[-1]["content_type"] = "application/octet-stream"

        process.stdout.close()
        if process.wait(timeout=30) != 0:
            raise RestoreValidationError("age could not authenticate/decrypt the archive")
    except BaseException:
        if process.stdout and not process.stdout.closed:
            process.stdout.close()
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=10)
        raise

    if manifest_data is None:
        raise RestoreValidationError("Archive manifest is missing")
    try:
        manifest = json.loads(manifest_data)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise RestoreValidationError("Archive manifest is invalid") from exc
    if (
        manifest.get("format") != "psicogest-supabase-objects-v1"
        or manifest.get("classification") != expected_classification
        or manifest.get("project_ref") != expected_source_project_ref
        or manifest.get("bucket") != expected_bucket
        or manifest.get("objects") != observed
    ):
        raise RestoreValidationError("Archive identity, classification, or object hashes do not match")
    return manifest


def _hash_stream(stream: BinaryIO) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
        digest.update(chunk)
        size += len(chunk)
    return digest.hexdigest(), size


def existing_objects(s3_client, bucket: str, manifest: dict[str, object]) -> set[str]:
    expected = {str(item["key"]): item for item in manifest["objects"]}
    found: set[str] = set()
    total_bytes = 0
    paginator = s3_client.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket):
        for row in page.get("Contents", []):
            key = str(row.get("Key", ""))
            validate_object_key(key)
            item = expected.get(key)
            if item is None or row.get("Size") != item["size"]:
                raise RestoreValidationError("Destination contains an object outside the verified archive")
            total_bytes += int(row["Size"])
            if len(found) >= MAX_OBJECTS or total_bytes > MAX_TOTAL_BYTES:
                raise RestoreValidationError("Destination exceeds the configured safety limits")
            response = s3_client.get_object(Bucket=bucket, Key=key)
            body = response["Body"]
            try:
                digest, size = _hash_stream(body)
            finally:
                body.close()
            if digest != item["sha256"] or size != item["size"]:
                raise RestoreValidationError("Destination contains an object that differs from the archive")
            found.add(key)
    return found


def apply_restore(
    s3_client,
    *,
    bucket: str,
    manifest: dict[str, object],
    archive_path: Path,
    identity_file: str,
    age_executable: str = "age",
    expected_classification: str = "SYNTHETIC_ONLY",
    expected_bucket: str = SYNTHETIC_BUCKET,
) -> tuple[int, int]:
    # Re-authenticate and re-check the archive immediately before any writes.
    verified = inspect_archive(
        archive_path,
        identity_file=identity_file,
        expected_source_project_ref=str(manifest["project_ref"]),
        age_executable=age_executable,
        expected_classification=expected_classification,
        expected_bucket=expected_bucket,
    )
    existing = existing_objects(s3_client, bucket, verified)
    missing = {str(item["key"]): item for item in verified["objects"] if item["key"] not in existing}
    if not missing:
        return 0, 0

    process = _decrypted_archive(archive_path, identity_file, age_executable=age_executable)
    assert process.stdout is not None
    restored_count = 0
    restored_bytes = 0
    try:
        with tarfile.open(fileobj=process.stdout, mode="r|") as archive:
            for member in archive:
                if member.name == "manifest.json":
                    continue
                key = member.name.removeprefix("objects/")
                expected = missing.get(key)
                if expected is None:
                    continue
                body_stream = archive.extractfile(member)
                if body_stream is None:
                    raise RestoreValidationError("Archive object cannot be read during restore")
                body = body_stream.read(MAX_OBJECT_BYTES + 1)
                if len(body) != expected["size"] or hashlib.sha256(body).hexdigest() != expected["sha256"]:
                    raise RestoreValidationError("Archive object changed after preflight")
                # Atomic conditional write closes the check-then-put race. Supabase Storage
                # documents If-None-Match support; fail closed if the endpoint rejects it.
                s3_client.put_object(
                    Bucket=bucket,
                    Key=key,
                    Body=body,
                    ContentLength=len(body),
                    ContentType=str(expected["content_type"]),
                    IfNoneMatch="*",
                )
                restored_count += 1
                restored_bytes += len(body)
        process.stdout.close()
        if process.wait(timeout=30) != 0:
            raise RestoreValidationError("age could not authenticate/decrypt the archive")
    except BaseException:
        if process.stdout and not process.stdout.closed:
            process.stdout.close()
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=10)
        raise
    return restored_count, restored_bytes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--source-project-ref", required=True)
    parser.add_argument("--apply", action="store_true", help="Upload only missing objects after full preflight")
    args = parser.parse_args()

    try:
        env = dict(os.environ)
        config = validate_restore_configuration(env, args.source_project_ref)
        manifest = inspect_archive(
            args.archive,
            identity_file=config["identity_file"],
            expected_source_project_ref=args.source_project_ref,
        )
        import boto3

        client = boto3.client(
            "s3",
            endpoint_url=config["endpoint"],
            region_name=config["region"],
            aws_access_key_id=config["access_key"],
            aws_secret_access_key=config["secret_key"],
            verify=True,
            config=boto3.session.Config(s3={"addressing_style": "path"}),
        )
        existing = existing_objects(client, config["bucket"], manifest)
        missing_count = len(manifest["objects"]) - len(existing)
        if not args.apply:
            print(f"Dry run valid: objects={len(manifest['objects'])}; already present={len(existing)}; to restore={missing_count}")
            return 0
        if config["confirmation"] != "RESTORE-SYNTHETIC":
            raise RestoreValidationError("RESTORE_CONFIRMATION must be RESTORE-SYNTHETIC for writes")
        count, size = apply_restore(
            client,
            bucket=config["bucket"],
            manifest=manifest,
            archive_path=args.archive,
            identity_file=config["identity_file"],
        )
        print(f"Synthetic Storage restore complete: objects_added={count}; bytes_added={size}")
        return 0
    except Exception as exc:
        print(f"Synthetic Storage restore failed ({type(exc).__name__})", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
