"""Create an encrypted, synthetic-only archive of the PsicoGest Supabase test bucket."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import tarfile
from datetime import datetime, timezone
from pathlib import Path
from typing import BinaryIO


SYNTHETIC_BUCKET = "psicogest-clinical-exports-synthetic"
PROJECT_REF_RE = re.compile(r"^[a-z0-9]{20}$")
AGE_RECIPIENT_RE = re.compile(r"^age1[023456789acdefghjklmnpqrstuvwxyz]{20,80}$")
OBJECT_KEY_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/"
    r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
)
MAX_OBJECTS = 20_000
MAX_TOTAL_BYTES = 700_000_000
MAX_OBJECT_BYTES = 64_000_000


class ConfigurationError(ValueError):
    pass


def validate_configuration(env: dict[str, str]) -> dict[str, str]:
    """Fail closed unless every target identifier proves this is the synthetic bucket."""
    if env.get("BACKUP_DATA_CLASSIFICATION") != "SYNTHETIC_ONLY":
        raise ConfigurationError("BACKUP_DATA_CLASSIFICATION must be SYNTHETIC_ONLY")

    project_ref = env.get("BACKUP_SUPABASE_PROJECT_REF", "")
    if not PROJECT_REF_RE.fullmatch(project_ref):
        raise ConfigurationError("Supabase synthetic project ref is missing or invalid")

    endpoint = env.get("BACKUP_SUPABASE_S3_ENDPOINT", "").rstrip("/")
    expected_endpoint = f"https://{project_ref}.storage.supabase.co/storage/v1/s3"
    if endpoint != expected_endpoint:
        raise ConfigurationError("S3 endpoint does not match the configured synthetic project")

    bucket = env.get("BACKUP_SUPABASE_S3_BUCKET", "")
    if bucket != SYNTHETIC_BUCKET:
        raise ConfigurationError("Only the fixed synthetic clinical-export bucket is allowed")

    region = env.get("BACKUP_SUPABASE_S3_REGION", "")
    access_key = env.get("BACKUP_SUPABASE_S3_ACCESS_KEY_ID", "")
    secret_key = env.get("BACKUP_SUPABASE_S3_SECRET_ACCESS_KEY", "")
    recipient = env.get("BACKUP_AGE_RECIPIENT", "")
    if not all((region, access_key, secret_key)):
        raise ConfigurationError("Supabase S3 credentials and region must be configured")
    if not AGE_RECIPIENT_RE.fullmatch(recipient):
        raise ConfigurationError("A valid age public recipient must be configured")

    return {
        "project_ref": project_ref,
        "endpoint": endpoint,
        "bucket": bucket,
        "region": region,
        "access_key": access_key,
        "secret_key": secret_key,
        "recipient": recipient,
    }


def validate_object_key(key: str) -> None:
    """Accept only UUID tenant/export keys emitted by the application adapter."""
    if not OBJECT_KEY_RE.fullmatch(key):
        raise ValueError("Storage contains an object key outside the synthetic adapter format")


def list_objects(s3_client, bucket: str) -> list[dict[str, object]]:
    objects: list[dict[str, object]] = []
    total_bytes = 0
    paginator = s3_client.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket):
        for item in page.get("Contents", []):
            key = str(item.get("Key", ""))
            validate_object_key(key)
            size = item.get("Size")
            if not isinstance(size, int) or size < 1 or size > MAX_OBJECT_BYTES:
                raise ValueError("Storage contains an empty, invalid, or oversized object")
            total_bytes += size
            if len(objects) >= MAX_OBJECTS or total_bytes > MAX_TOTAL_BYTES:
                raise ValueError("Synthetic object backup exceeds the configured safety limit")
            objects.append({"key": key, "size": size})
    return objects


class HashingReader:
    """Hash streamed object bytes without staging plaintext on disk."""

    def __init__(self, stream: BinaryIO):
        self.stream = stream
        self.digest = hashlib.sha256()
        self.bytes_read = 0

    def read(self, size: int = -1) -> bytes:
        chunk = self.stream.read(size)
        self.digest.update(chunk)
        self.bytes_read += len(chunk)
        return chunk


def _tar_info(name: str, size: int, created_at: int) -> tarfile.TarInfo:
    info = tarfile.TarInfo(name)
    info.size = size
    info.mode = 0o600
    info.uid = info.gid = 0
    info.uname = info.gname = ""
    info.mtime = created_at
    return info


def create_encrypted_backup(
    s3_client,
    *,
    bucket: str,
    project_ref: str,
    recipient: str,
    output_path: Path,
    age_executable: str = "age",
    now: datetime | None = None,
    classification: str = "SYNTHETIC_ONLY",
) -> tuple[int, int]:
    if classification not in ("SYNTHETIC_ONLY", "PRODUCTION_CLINICAL"):
        raise ConfigurationError("Unsupported backup classification")
    objects = list_objects(s3_client, bucket)
    created = now or datetime.now(timezone.utc)
    created = created.astimezone(timezone.utc)
    created_epoch = int(created.timestamp())
    manifest: list[dict[str, object]] = []

    output_path.parent.mkdir(parents=True, exist_ok=True)
    age_process = subprocess.Popen(
        [age_executable, "--recipient", recipient, "--output", str(output_path)],
        stdin=subprocess.PIPE,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    assert age_process.stdin is not None
    try:
        with tarfile.open(fileobj=age_process.stdin, mode="w|") as archive:
            for item in objects:
                key = str(item["key"])
                expected_size = int(item["size"])
                response = s3_client.get_object(Bucket=bucket, Key=key)
                content_type = response.get("ContentType")
                if content_type != "application/octet-stream":
                    response["Body"].close()
                    raise ValueError("Synthetic export has an unexpected media type")
                body = response["Body"]
                hashing_reader = HashingReader(body)
                try:
                    archive.addfile(_tar_info(f"objects/{key}", expected_size, created_epoch), hashing_reader)
                finally:
                    body.close()
                if hashing_reader.bytes_read != expected_size:
                    raise IOError("Supabase returned an object with an unexpected length")
                manifest.append({
                    "key": key,
                    "size": expected_size,
                    "content_type": content_type,
                    "sha256": hashing_reader.digest.hexdigest(),
                })

            manifest_bytes = json.dumps(
                {
                    "format": "psicogest-supabase-objects-v1",
                    "classification": classification,
                    "project_ref": project_ref,
                    "bucket": bucket,
                    "created_at": created.isoformat(),
                    "objects": manifest,
                },
                sort_keys=True,
                separators=(",", ":"),
            ).encode("utf-8")
            archive.addfile(_tar_info("manifest.json", len(manifest_bytes), created_epoch), io.BytesIO(manifest_bytes))

        if list_objects(s3_client, bucket) != objects:
            raise IOError("Storage object listing changed during backup")

        age_process.stdin.close()
        exit_code = age_process.wait()
        if exit_code != 0:
            raise RuntimeError(f"age encryption failed (exit {exit_code})")
        if not output_path.is_file() or output_path.stat().st_size == 0:
            raise IOError("age did not produce a non-empty encrypted archive")
        return len(objects), sum(int(item["size"]) for item in objects)
    except BaseException:
        if age_process.stdin and not age_process.stdin.closed:
            try:
                age_process.stdin.close()
            except OSError:
                pass
        if age_process.poll() is None:
            age_process.terminate()
            age_process.wait(timeout=10)
        output_path.unlink(missing_ok=True)
        raise


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-directory", type=Path, required=True)
    args = parser.parse_args()

    try:
        config = validate_configuration(dict(os.environ))
        import boto3  # Imported only after the target passed the synthetic-only checks.

        s3_client = boto3.client(
            "s3",
            endpoint_url=config["endpoint"],
            region_name=config["region"],
            aws_access_key_id=config["access_key"],
            aws_secret_access_key=config["secret_key"],
            verify=True,
            config=boto3.session.Config(s3={"addressing_style": "path"}),
        )
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        output_path = args.output_directory / f"psicogest-synthetic-objects-{timestamp}.tar.age"
        count, size = create_encrypted_backup(
            s3_client,
            bucket=config["bucket"],
            project_ref=config["project_ref"],
            recipient=config["recipient"],
            output_path=output_path,
        )
        print(f"Encrypted synthetic Storage backup created: {output_path.name}; objects={count}; bytes={size}")
        return 0
    except Exception as exc:
        # Provider exceptions can contain request details. Keep them out of CI logs.
        print(f"Synthetic Storage backup failed ({type(exc).__name__})", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
