"""Store and recover encrypted database and object archives in a dedicated Workers KV namespace.

Only ciphertext is accepted. The manifest is published after every chunk has been read
back and checked. The caller may advance the database checkpoint only after upload()
returns. This tool never deletes older backups; expiry is set on each unique key.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path

CHUNK_BYTES = 20 * 1024 * 1024
MAX_NAMESPACE_BYTES = 800_000_000
MAX_WRITES_PER_RUN = 10
TTL_SECONDS = 48 * 60 * 60
MAX_ARCHIVE_BYTES = 300_000_000
PROJECT_REF_RE = re.compile(r"^[a-z0-9]{20}$")
ID_RE = re.compile(r"^[a-f0-9]{32}$")
SHA_RE = re.compile(r"^[a-f0-9]{64}$")


class BackupError(RuntimeError):
    pass


class CloudflareKv:
    def __init__(self, account_id: str, namespace_id: str, token: str):
        if not all(re.fullmatch(r"[a-f0-9]{32}", value or "")
                   for value in (account_id, namespace_id)) or not token:
            raise BackupError("Cloudflare KV account, namespace or token is invalid")
        self.base = ("https://api.cloudflare.com/client/v4/accounts/"
                     f"{account_id}/storage/kv/namespaces/{namespace_id}")
        self.token = token

    def _request(self, method: str, suffix: str, body: bytes | None = None,
                 content_type: str = "application/octet-stream") -> bytes:
        request = urllib.request.Request(
            self.base + suffix,
            data=body,
            method=method,
            headers={
                "Authorization": f"Bearer {self.token}",
                "Content-Type": content_type,
                "Accept": "application/json" if suffix.startswith("/keys") else "*/*",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                return response.read()
        except (urllib.error.URLError, TimeoutError) as exc:
            # The response may contain URLs or secrets; do not print provider bodies.
            raise BackupError(f"Cloudflare KV request failed ({type(exc).__name__})") from None

    def list_keys(self) -> list[dict]:
        result = []
        cursor = None
        while True:
            query = "?limit=1000"
            if cursor:
                query += "&cursor=" + urllib.parse.quote(cursor, safe="")
            data = json.loads(self._request("GET", "/keys" + query))
            if data.get("success") is not True or not isinstance(data.get("result"), list):
                raise BackupError("Cloudflare KV key listing failed")
            result.extend(data["result"])
            next_cursor = data.get("result_info", {}).get("cursor")
            if not next_cursor:
                return result
            if next_cursor == cursor:
                raise BackupError("Cloudflare KV pagination did not advance")
            cursor = next_cursor

    def put(self, key: str, value: bytes, size: int) -> None:
        boundary = "psicogest" + uuid.uuid4().hex
        body = (
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"value\"\r\n"
            "Content-Type: application/octet-stream\r\n\r\n".encode()
            + value
            + f"\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n"
              "Content-Type: application/json\r\n\r\n"
              f"{{\"size\":{size}}}\r\n--{boundary}--\r\n".encode()
        )
        suffix = "/values/" + urllib.parse.quote(key, safe="") + f"?expiration_ttl={TTL_SECONDS}"
        data = json.loads(self._request("PUT", suffix, body,
                                        f"multipart/form-data; boundary={boundary}"))
        if data.get("success") is not True:
            raise BackupError("Cloudflare KV write failed")

    def get(self, key: str) -> bytes:
        return self._request("GET", "/values/" + urllib.parse.quote(key, safe=""))


def _sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _read_verified(client: CloudflareKv, key: str, digest: str, *, retries: int = 35) -> bytes:
    for attempt in range(retries):
        try:
            data = client.get(key)
            if _sha(data) == digest:
                return data
        except BackupError:
            pass
        if attempt + 1 < retries:
            time.sleep(2)
    raise BackupError("Encrypted Cloudflare KV chunk was not readable with its checksum")


def _archive_parts(path: Path, prefix: str) -> tuple[list[tuple[str, bytes]], dict]:
    if path.suffix != ".age" or not path.is_file():
        raise BackupError("Both backup inputs must be existing .age ciphertext files")
    size = path.stat().st_size
    if size < 1 or size > MAX_ARCHIVE_BYTES:
        raise BackupError("Encrypted archive is empty or above the per-archive limit")
    with path.open("rb") as source:
        if source.read(len(b"age-encryption.org/v1\n")) != b"age-encryption.org/v1\n":
            raise BackupError("Input is not an age v1 encrypted archive")
    parts = []
    whole_digest = hashlib.sha256()
    with path.open("rb") as source:
        for index in range(0, (size + CHUNK_BYTES - 1) // CHUNK_BYTES):
            chunk = source.read(CHUNK_BYTES)
            whole_digest.update(chunk)
            parts.append((f"{prefix}/{index:03d}", chunk))
    return parts, {"bytes": size, "sha256": whole_digest.hexdigest(), "chunks": [
        {"key": key, "bytes": len(chunk), "sha256": _sha(chunk)} for key, chunk in parts
    ]}


def upload(client: CloudflareKv, database_age: Path, objects_age: Path,
           *, project_ref: str, snapshot_started_at: datetime) -> str:
    if not PROJECT_REF_RE.fullmatch(project_ref):
        raise BackupError("Supabase project ref is invalid")
    started = snapshot_started_at.astimezone(timezone.utc)
    if started > datetime.now(timezone.utc):
        raise BackupError("Backup snapshot start cannot be in the future")
    backup_id = uuid.uuid4().hex
    prefix = f"psicogest/v1/{backup_id}"
    db_parts, db = _archive_parts(database_age, prefix + "/database")
    object_parts, objects = _archive_parts(objects_age, prefix + "/objects")
    parts = db_parts + object_parts
    if len(parts) + 1 > MAX_WRITES_PER_RUN:
        raise BackupError("Backup requires too many KV writes for the free plan")
    manifest = {
        "format": "psicogest-encrypted-backup-v1",
        "project_ref": project_ref,
        "snapshot_started_at": started.isoformat(),
        "database": db,
        "objects": objects,
    }
    manifest_bytes = json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode()
    existing = client.list_keys()
    used = 0
    for item in existing:
        metadata = item.get("metadata")
        size = metadata.get("size") if isinstance(metadata, dict) else None
        if not isinstance(size, int) or size < 0:
            raise BackupError("KV has a key with unknown size; refuse to exceed free storage")
        used += size
    if used + sum(len(chunk) for _, chunk in parts) + len(manifest_bytes) > MAX_NAMESPACE_BYTES:
        raise BackupError("KV free storage safety budget would be exceeded")
    for key, chunk in parts:
        client.put(key, chunk, len(chunk))
        _read_verified(client, key, _sha(chunk))
    manifest_key = prefix + "/manifest"
    client.put(manifest_key, manifest_bytes, len(manifest_bytes))
    _read_verified(client, manifest_key, _sha(manifest_bytes))
    return manifest_key


def download(client: CloudflareKv, manifest_key: str, output_dir: Path,
             *, expected_project_ref: str | None = None) -> dict:
    if not re.fullmatch(r"psicogest/v1/[a-f0-9]{32}/manifest", manifest_key):
        raise BackupError("Backup manifest key is invalid")
    manifest = json.loads(client.get(manifest_key))
    if manifest.get("format") != "psicogest-encrypted-backup-v1" or not PROJECT_REF_RE.fullmatch(
            str(manifest.get("project_ref", ""))):
        raise BackupError("Backup manifest format or project is invalid")
    if expected_project_ref is not None and manifest["project_ref"] != expected_project_ref:
        raise BackupError("Backup belongs to a different Supabase project")
    if output_dir.exists() and any(output_dir.iterdir()):
        raise BackupError("Restore destination must be empty")
    output_dir.mkdir(parents=True, exist_ok=True)
    for name in ("database", "objects"):
        item = manifest.get(name)
        if not isinstance(item, dict) or not SHA_RE.fullmatch(str(item.get("sha256", ""))):
            raise BackupError("Backup manifest archive is invalid")
        chunks = item.get("chunks")
        if not isinstance(chunks, list) or not chunks or len(chunks) > MAX_WRITES_PER_RUN:
            raise BackupError("Backup manifest chunks are invalid")
        destination = output_dir / f"{name}.age"
        digest = hashlib.sha256()
        size = 0
        with destination.open("xb") as target:
            for index, chunk in enumerate(chunks):
                if not isinstance(chunk, dict):
                    raise BackupError("Backup manifest chunk is invalid")
                key = chunk.get("key")
                if key != manifest_key.removesuffix("/manifest") + f"/{name}/{index:03d}":
                    raise BackupError("Backup chunk key is invalid")
                data = _read_verified(client, key, chunk.get("sha256", ""), retries=2)
                if len(data) != chunk.get("bytes") or len(data) > CHUNK_BYTES:
                    raise BackupError("Backup chunk length is invalid")
                target.write(data)
                digest.update(data)
                size += len(data)
        if digest.hexdigest() != item["sha256"] or size != item.get("bytes"):
            raise BackupError("Restored encrypted archive checksum is invalid")
        (output_dir / f"{name}.age.sha256").write_text(
            f"{digest.hexdigest()}  {destination.name}\n", encoding="ascii"
        )
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("upload", "download"))
    parser.add_argument("--database-age", type=Path)
    parser.add_argument("--objects-age", type=Path)
    parser.add_argument("--project-ref")
    parser.add_argument("--snapshot-started-at")
    parser.add_argument("--manifest-key")
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--expected-project-ref")
    args = parser.parse_args()
    try:
        client = CloudflareKv(os.environ.get("CLOUDFLARE_ACCOUNT_ID", ""),
                              os.environ.get("CLOUDFLARE_KV_NAMESPACE_ID", ""),
                              os.environ.get("CLOUDFLARE_KV_TOKEN", ""))
        if args.mode == "upload":
            if not all((args.database_age, args.objects_age, args.project_ref,
                        args.snapshot_started_at)):
                raise BackupError("Upload requires both archives, project and snapshot time")
            key = upload(client, args.database_age, args.objects_age,
                         project_ref=args.project_ref,
                         snapshot_started_at=datetime.fromisoformat(args.snapshot_started_at))
            print(key)
        else:
            if not args.manifest_key or not args.output_dir:
                raise BackupError("Download requires manifest key and an empty output directory")
            if not args.expected_project_ref:
                raise BackupError("Download requires the expected source project ref")
            manifest = download(client, args.manifest_key, args.output_dir,
                                expected_project_ref=args.expected_project_ref)
            print(json.dumps({"project_ref": manifest["project_ref"],
                              "snapshot_started_at": manifest["snapshot_started_at"]}))
        return 0
    except (BackupError, OSError, ValueError, TypeError, KeyError) as exc:
        print(f"Encrypted KV backup failed ({type(exc).__name__})", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
