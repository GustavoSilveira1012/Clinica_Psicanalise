"""Restore verified production Storage ciphertext only into a different isolated project."""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

from ops.backup_production_objects import BUCKET, PROJECT_REF
from ops.restore_supabase_objects import (
    RestoreValidationError,
    apply_restore,
    existing_objects,
    inspect_archive,
)

PROJECT_REF_RE = re.compile(r"^[a-z0-9]{20}$")


def validate_configuration(env: dict[str, str]) -> dict[str, str]:
    if env.get("RESTORE_DATA_CLASSIFICATION") != "PRODUCTION_ISOLATED_RESTORE":
        raise RestoreValidationError("Isolated production restore classification is required")
    destination = env.get("RESTORE_SUPABASE_PROJECT_REF", "")
    if not PROJECT_REF_RE.fullmatch(destination) or destination == PROJECT_REF:
        raise RestoreValidationError("Destination must be a distinct Supabase project")
    expected = f"https://{destination}.storage.supabase.co/storage/v1/s3"
    if env.get("RESTORE_SUPABASE_S3_ENDPOINT", "").rstrip("/") != expected:
        raise RestoreValidationError("Destination S3 endpoint does not match the isolated project")
    if env.get("RESTORE_SUPABASE_S3_BUCKET") != BUCKET:
        raise RestoreValidationError("Destination bucket is not the approved empty restore bucket")
    for key in ("RESTORE_SUPABASE_S3_REGION", "RESTORE_SUPABASE_S3_ACCESS_KEY_ID",
                "RESTORE_SUPABASE_S3_SECRET_ACCESS_KEY", "RESTORE_AGE_IDENTITY_FILE"):
        if not env.get(key):
            raise RestoreValidationError("Restore credentials and identity file are required")
    if not Path(env["RESTORE_AGE_IDENTITY_FILE"]).is_file():
        raise RestoreValidationError("Restore identity file is unavailable")
    return env


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    try:
        env = validate_configuration(dict(os.environ))
        manifest = inspect_archive(
            args.archive, identity_file=env["RESTORE_AGE_IDENTITY_FILE"],
            expected_source_project_ref=PROJECT_REF,
            expected_classification="PRODUCTION_CLINICAL",
            expected_bucket=BUCKET,
        )
        import boto3

        client = boto3.client(
            "s3", endpoint_url=env["RESTORE_SUPABASE_S3_ENDPOINT"],
            region_name=env["RESTORE_SUPABASE_S3_REGION"],
            aws_access_key_id=env["RESTORE_SUPABASE_S3_ACCESS_KEY_ID"],
            aws_secret_access_key=env["RESTORE_SUPABASE_S3_SECRET_ACCESS_KEY"],
            verify=True,
            config=boto3.session.Config(s3={"addressing_style": "path"}),
        )
        existing = existing_objects(client, BUCKET, manifest)
        if existing:
            raise RestoreValidationError("Isolated destination bucket must be empty")
        if not args.apply:
            print(f"Isolated restore preflight valid; objects={len(manifest['objects'])}")
            return 0
        if env.get("RESTORE_CONFIRMATION") != "RESTORE-PRODUCTION-ISOLATED":
            raise RestoreValidationError("Explicit isolated restore confirmation is required")
        count, size = apply_restore(
            client, bucket=BUCKET, manifest=manifest, archive_path=args.archive,
            identity_file=env["RESTORE_AGE_IDENTITY_FILE"],
            expected_classification="PRODUCTION_CLINICAL", expected_bucket=BUCKET,
        )
        print(f"Isolated production objects restored; objects={count}; bytes={size}")
        return 0
    except Exception as exc:
        print(f"Isolated production object restore failed ({type(exc).__name__})", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
