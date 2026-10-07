"""Encrypt the approved PsicoGest production Storage bucket; never emit plaintext files."""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

from ops.backup_supabase_objects import (
    AGE_RECIPIENT_RE,
    ConfigurationError,
    create_encrypted_backup,
)

PROJECT_REF = "gdorfdcvajeczzaesjjq"
BUCKET = "psicogest-clinical-exports"


def validate_configuration(env: dict[str, str]) -> dict[str, str]:
    if env.get("BACKUP_DATA_CLASSIFICATION") != "PRODUCTION_CLINICAL":
        raise ConfigurationError("Production classification is required")
    if env.get("BACKUP_SUPABASE_PROJECT_REF") != PROJECT_REF:
        raise ConfigurationError("Production project ref does not match the approved project")
    expected = f"https://{PROJECT_REF}.storage.supabase.co/storage/v1/s3"
    if env.get("BACKUP_SUPABASE_S3_ENDPOINT", "").rstrip("/") != expected:
        raise ConfigurationError("Production S3 endpoint does not match the approved project")
    if env.get("BACKUP_SUPABASE_S3_BUCKET") != BUCKET:
        raise ConfigurationError("Production bucket does not match the approved bucket")
    for key in ("BACKUP_SUPABASE_S3_REGION", "BACKUP_SUPABASE_S3_ACCESS_KEY_ID",
                "BACKUP_SUPABASE_S3_SECRET_ACCESS_KEY"):
        if not env.get(key):
            raise ConfigurationError("Production Storage credentials and region are required")
    if not AGE_RECIPIENT_RE.fullmatch(env.get("BACKUP_AGE_RECIPIENT", "")):
        raise ConfigurationError("A valid public age recipient is required")
    return env


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        env = validate_configuration(dict(os.environ))
        import boto3

        client = boto3.client(
            "s3",
            endpoint_url=env["BACKUP_SUPABASE_S3_ENDPOINT"],
            region_name=env["BACKUP_SUPABASE_S3_REGION"],
            aws_access_key_id=env["BACKUP_SUPABASE_S3_ACCESS_KEY_ID"],
            aws_secret_access_key=env["BACKUP_SUPABASE_S3_SECRET_ACCESS_KEY"],
            verify=True,
            config=boto3.session.Config(s3={"addressing_style": "path"}),
        )
        count, size = create_encrypted_backup(
            client, bucket=BUCKET, project_ref=PROJECT_REF,
            recipient=env["BACKUP_AGE_RECIPIENT"], output_path=args.output,
            classification="PRODUCTION_CLINICAL",
        )
        print(f"Encrypted production Storage archive created; objects={count}; bytes={size}")
        return 0
    except Exception as exc:
        print(f"Production Storage backup failed ({type(exc).__name__})", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
