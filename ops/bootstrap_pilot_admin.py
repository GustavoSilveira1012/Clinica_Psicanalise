"""Create the first nominal SYSTEM_ADMIN in an empty PsicoGest database.

This is a one-shot operator task, never part of application startup. Credentials
are read from private environment variables and never printed or written to disk.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

PROJECT_REF = "gdorfdcvajeczzaesjjq"
POOLER_HOST = "aws-0-us-east-1.pooler.supabase.com"
ROOT_CERT = Path(__file__).resolve().parents[1] / "backend/psicogest/psicogest/certs/supabase-prod-ca-2021.crt"
CONFIRMATION = "BOOTSTRAP-PSICOGEST-FIRST-ADMIN"


class BootstrapError(ValueError):
    """Safe-to-print error that never contains a credential."""


def validate(environ: dict[str, str]) -> tuple[str, str, str, str]:
    if environ.get("PILOT_ADMIN_BOOTSTRAP_CONFIRMATION") != CONFIRMATION:
        raise BootstrapError("One-time bootstrap confirmation is missing")
    admin_db_password = environ.get("PSICOGEST_SUPABASE_DB_PASSWORD", "")
    initial_password = environ.get("PILOT_ADMIN_PASSWORD", "")
    name = environ.get("PILOT_ADMIN_NAME", "").strip()
    email = environ.get("PILOT_ADMIN_EMAIL", "").strip().lower()
    if not admin_db_password or not initial_password:
        raise BootstrapError("Required private credentials are missing")
    if initial_password == admin_db_password:
        raise BootstrapError("Application and database credentials must differ")
    if len(initial_password) < 16 or len(initial_password.encode("utf-8")) > 72:
        raise BootstrapError("Initial password must contain 16–72 UTF-8 bytes")
    if len(name) < 3 or len(name) > 150:
        raise BootstrapError("Nominal administrator name is invalid")
    if len(email) > 150 or not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", email):
        raise BootstrapError("Nominal administrator email is invalid")
    return admin_db_password, initial_password, name, email


def bootstrap(psycopg, bcrypt, environ: dict[str, str]) -> int:
    admin_db_password, initial_password, name, email = validate(environ)
    password_hash = bcrypt.hashpw(
        initial_password.encode("utf-8"), bcrypt.gensalt(rounds=12)
    ).decode("ascii")
    with psycopg.connect(
        host=POOLER_HOST,
        port=5432,
        dbname="postgres",
        user=f"postgres.{PROJECT_REF}",
        password=admin_db_password,
        sslmode="verify-full",
        sslrootcert=str(ROOT_CERT),
        connect_timeout=15,
    ) as connection:
        with connection.cursor() as cursor:
            cursor.execute("LOCK TABLE public.users IN EXCLUSIVE MODE")
            cursor.execute("SELECT count(*) FROM public.users")
            if cursor.fetchone()[0] != 0:
                raise BootstrapError("Bootstrap refused: the user table is not empty")
            cursor.execute(
                """INSERT INTO public.users
                   (name, email, password_hash, role, active, created_at, updated_at)
                   VALUES (%s, %s, %s, 'SYSTEM_ADMIN'::public.user_role, TRUE, now(), now())
                   RETURNING id""",
                (name, email, password_hash),
            )
            new_id = cursor.fetchone()[0]
    return new_id


def main() -> int:
    try:
        import bcrypt
        import psycopg

        new_id = bootstrap(psycopg, bcrypt, os.environ)
    except BootstrapError as exc:
        print(f"Bootstrap refused: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        # Driver errors may embed connection parameters or values; do not print
        # the exception text or traceback in CI logs.
        print(f"Bootstrap failed ({type(exc).__name__}); inspect privately.", file=sys.stderr)
        return 1
    print(f"Created first nominal administrator, id={new_id}; MFA enrollment is required at login.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
