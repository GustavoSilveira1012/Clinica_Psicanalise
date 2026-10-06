"""Set the two PsicoGest database role passwords from private CI secrets.

Only the SCRAM verifiers cross the SQL connection. Password values are never
printed or included in a command line or a versioned file.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import os
import sys
import time
from pathlib import Path

PROJECT_REF = "gdorfdcvajeczzaesjjq"
POOLER_HOST = "aws-0-us-east-1.pooler.supabase.com"
ROOT_CERT = Path(__file__).resolve().parents[1] / "backend/psicogest/psicogest/certs/supabase-prod-ca-2021.crt"
ROLE_ENV = {
    "psicogest_runtime": "PSICOGEST_RUNTIME_DB_PASSWORD",
    "psicogest_backup": "BACKUP_SUPABASE_DATABASE_PASSWORD",
}


class ConfigurationError(ValueError):
    """A safe-to-print validation failure with no credential values."""


class RoleConnectionError(ConfigurationError):
    """A database login failure summarized without connection details."""


def connection_failure_category(exc: Exception) -> str:
    message = str(exc).lower()
    if "password authentication failed" in message or "authentication failed" in message:
        return "authentication"
    if "certificate verify failed" in message or "ssl" in message or "certificate" in message:
        return "tls"
    if "timed out" in message or "timeout" in message:
        return "timeout"
    if "could not translate host name" in message or "name or service not known" in message:
        return "dns"
    return "other"


def scram_verifier(password: str, salt: bytes) -> str:
    """Build PostgreSQL's SCRAM-SHA-256 verifier for an ASCII password."""
    salted = hashlib.pbkdf2_hmac("sha256", password.encode("ascii"), salt, 4096)
    client_key = hmac.digest(salted, b"Client Key", "sha256")
    stored_key = hashlib.sha256(client_key).digest()
    server_key = hmac.digest(salted, b"Server Key", "sha256")
    b64 = lambda value: base64.b64encode(value).decode("ascii")
    return f"SCRAM-SHA-256$4096:{b64(salt)}${b64(stored_key)}:{b64(server_key)}"


def read_secrets(environ: dict[str, str]) -> tuple[str, dict[str, str]]:
    admin = environ.get("PSICOGEST_SUPABASE_DB_PASSWORD", "")
    values = {role: environ.get(name, "") for role, name in ROLE_ENV.items()}
    missing = [
        name for name in ("PSICOGEST_SUPABASE_DB_PASSWORD", *ROLE_ENV.values())
        if not environ.get(name)
    ]
    if missing:
        raise ConfigurationError(f"Missing repository secrets: {', '.join(missing)}")
    named = {"PSICOGEST_SUPABASE_DB_PASSWORD": admin}
    named.update({ROLE_ENV[role]: value for role, value in values.items()})
    items = list(named.items())
    duplicates = [
        f"{first} and {second}"
        for position, (first, first_value) in enumerate(items)
        for second, second_value in items[position + 1:]
        if first_value == second_value
    ]
    if duplicates:
        raise ConfigurationError(f"Database secrets must be different: {', '.join(duplicates)}")
    invalid = [
        ROLE_ENV[role]
        for role, value in values.items()
        if len(value) < 16 or any(ord(ch) < 33 or ord(ch) > 126 for ch in value)
    ]
    if invalid:
        raise ConfigurationError(
            f"These secrets must each have 16+ printable ASCII characters: {', '.join(invalid)}"
        )
    return admin, values


def connect(psycopg, username: str, password: str):
    try:
        return psycopg.connect(
            host=POOLER_HOST,
            port=5432,
            dbname="postgres",
            user=f"{username}.{PROJECT_REF}",
            password=password,
            sslmode="verify-full",
            sslrootcert=str(ROOT_CERT),
            connect_timeout=15,
        )
    except psycopg.OperationalError as exc:
        raise RoleConnectionError(
            f"{username} connection failed: {connection_failure_category(exc)}"
        ) from None


def configure(psycopg, sql, admin: str, passwords: dict[str, str]) -> None:
    with connect(psycopg, "postgres", admin) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """SELECT rolname, rolcanlogin, rolsuper, rolcreatedb,
                          rolcreaterole, rolreplication, rolbypassrls
                   FROM pg_roles WHERE rolname IN (%s, %s)""",
                tuple(ROLE_ENV),
            )
            found = {row[0]: row[1:] for row in cur.fetchall()}
            expected = {
                "psicogest_runtime": (True, False, False, False, False, False),
                "psicogest_backup": (True, False, False, False, False, True),
            }
            if found != expected:
                raise ConfigurationError("Database role attributes differ from the approved design")
            for role, password in passwords.items():
                verifier = scram_verifier(password, os.urandom(16))
                cur.execute(
                    sql.SQL("ALTER ROLE {} PASSWORD {}").format(
                        sql.Identifier(role), sql.Literal(verifier)
                    )
                )

    # Supavisor may briefly cache old credentials after a rotation.
    for role, password in passwords.items():
        for attempt in range(5):
            try:
                with connect(psycopg, role, password) as conn:
                    with conn.cursor() as cur:
                        cur.execute("SELECT current_user")
                        if cur.fetchone()[0] != role:
                            raise ConfigurationError("Connected with an unexpected database role")
                break
            except RoleConnectionError:
                if attempt == 4:
                    raise
                time.sleep(5)
        print(f"Authenticated and verified role: {role}")


def main() -> int:
    try:
        admin, passwords = read_secrets(os.environ)
        import psycopg
        from psycopg import sql

        configure(psycopg, sql, admin, passwords)
    except ConfigurationError as exc:
        print(f"Role configuration rejected: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        # Driver errors can include connection parameters or SQL. Never print
        # exception text or a traceback in a CI log containing private secrets.
        print(f"Role configuration failed ({type(exc).__name__}); inspect the target and secrets privately.", file=sys.stderr)
        return 1
    print("Both approved roles have working, distinct password logins.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
