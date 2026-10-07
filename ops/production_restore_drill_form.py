"""One-use loopback form for an isolated restore of the approved encrypted backup.

The operator enters the Cloudflare token and age identity in the local browser.
Neither credential is logged, written to the repository, nor passed on a command line.
The only plaintext database lives in a network-disabled, RAM-backed PostgreSQL
container that is removed after the drill. The source Supabase project is never
used as a restore destination.
"""

from __future__ import annotations

import html
import json
import os
import re
import secrets
import subprocess
import tempfile
import threading
import time
import urllib.parse
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from ops.kv_encrypted_backup import CloudflareKv, download
from ops.restore_supabase_objects import inspect_archive

HOST = "127.0.0.1"
PROJECT_REF = "gdorfdcvajeczzaesjjq"
ACCOUNT_ID = "e4dc2a91f588f8e64fbbea799baec394"
NAMESPACE_ID = "5a70c006ca9c4eeca83d89ec26d78031"
AGE_RECIPIENT = "age1gus20cmsen65q7mqxzhkf0wtp03ng4m0g2yglpqp3265hejhjsmswnvnww"
BUCKET = "psicogest-clinical-exports"
CONFIRMATION = "RESTORE-ISOLATED-LOCAL"
MANIFEST_RE = re.compile(r"^psicogest/v1/[a-f0-9]{32}/manifest$")
AGE_SECRET_RE = re.compile(r"AGE-SECRET-KEY-[A-Z0-9]{40,100}")
SAFE_RUNTIME_ERRORS = frozenset({
    "Nenhum manifesto cifrado válido foi encontrado no KV aprovado",
    "Não foi possível criar o banco local isolado",
    "Banco local isolado não ficou pronto",
    "Restore local excedeu dez minutos",
    "Chave age não descriptografou o banco",
    "pg_restore não conseguiu aplicar o dump no PostgreSQL 17 local",
    "Não foi possível verificar o banco restaurado",
    "O banco temporário precisa ser removido manualmente",
    "Programa age verificado não está disponível",
    "A chave privada age não está em formato válido",
    "A chave age é válida, mas pertence a outro par",
})


def allowed_origin(origin: str | None, expected_origin: str) -> bool:
    # The Codex in-app browser may submit a loopback form with Origin: null.
    # Host pinning and the one-use nonce remain mandatory independently.
    return origin in (expected_origin, "null", None)


def safe_error_description(exc: Exception) -> str:
    value = str(exc)
    if type(exc) is RuntimeError and value in SAFE_RUNTIME_ERRORS:
        return value
    return type(exc).__name__


def extract_age_secret_line(text: str) -> str:
    matches = AGE_SECRET_RE.findall(text)
    if len(matches) != 1:
        raise RuntimeError("A chave privada age não está em formato válido")
    # age v1.3.2 emits 74 characters. A password-manager copy can append one
    # alphanumeric character; deriving and comparing the public recipient below
    # authenticates the 74-character candidate before any backup is opened.
    if len(matches[0]) not in (74, 75):
        raise RuntimeError("A chave privada age não está em formato válido")
    return matches[0][:74]


def select_latest_manifest(client: CloudflareKv) -> str:
    candidates: list[tuple[datetime, str]] = []
    for entry in client.list_keys():
        key = entry.get("name", "")
        if not isinstance(key, str) or not MANIFEST_RE.fullmatch(key):
            continue
        manifest = json.loads(client.get(key))
        if manifest.get("format") != "psicogest-encrypted-backup-v1":
            continue
        if manifest.get("project_ref") != PROJECT_REF:
            continue
        started = datetime.fromisoformat(manifest["snapshot_started_at"])
        if started.tzinfo is None:
            continue
        candidates.append((started, key))
    if not candidates:
        raise RuntimeError("Nenhum manifesto cifrado válido foi encontrado no KV aprovado")
    return max(candidates)[1]


def checked_run(args: list[str], *, timeout: int = 60) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(args, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, timeout=timeout, check=False)


def restore_database_to_ram_container(age_exe: Path, identity: Path, archive: Path) -> int:
    name = "psicogest-restore-" + secrets.token_hex(6)
    started = False
    try:
        result = checked_run([
            "docker", "run", "-d", "--rm", "--name", name, "--network", "none",
            "--tmpfs", "/var/lib/postgresql/data:rw,size=512m",
            "--tmpfs", "/var/run/postgresql:rw,size=16m",
            "-e", "POSTGRES_HOST_AUTH_METHOD=trust", "postgres:17-alpine",
        ], timeout=90)
        if result.returncode != 0:
            raise RuntimeError("Não foi possível criar o banco local isolado")
        started = True
        for _ in range(60):
            ready = checked_run(["docker", "exec", name, "pg_isready", "-U", "postgres"], timeout=8)
            if ready.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError("Banco local isolado não ficou pronto")

        with subprocess.Popen(
            [str(age_exe), "--decrypt", "--identity", str(identity), str(archive)],
            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
        ) as decrypt:
            assert decrypt.stdout is not None
            restore = subprocess.Popen([
                "docker", "exec", "-i", name, "pg_restore", "--username=postgres",
                "--dbname=postgres", "--no-owner", "--no-privileges", "--exit-on-error",
                "--single-transaction", "-",
            ], stdin=decrypt.stdout, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
            decrypt.stdout.close()
            try:
                _, restore_error = restore.communicate(timeout=600)
            except subprocess.TimeoutExpired:
                restore.kill()
                restore.communicate()
                raise RuntimeError("Restore local excedeu dez minutos") from None
            if decrypt.wait(timeout=30) != 0:
                raise RuntimeError("Chave age não descriptografou o banco")
            if restore.returncode != 0:
                # Never expose pg_restore stderr: it may include clinical values.
                raise RuntimeError("pg_restore não conseguiu aplicar o dump no PostgreSQL 17 local")
            del restore_error

        count = checked_run([
            "docker", "exec", name, "psql", "-U", "postgres", "-d", "postgres",
            "-At", "-c", "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
            "WHERE c.relkind='r' AND n.nspname IN ('public','app')",
        ], timeout=30)
        if count.returncode != 0:
            raise RuntimeError("Não foi possível verificar o banco restaurado")
        return int(count.stdout.strip())
    finally:
        if started:
            stopped = checked_run(["docker", "stop", name], timeout=45)
            if stopped.returncode != 0:
                removed = checked_run(["docker", "rm", "-f", name], timeout=45)
                if removed.returncode != 0:
                    raise RuntimeError("O banco temporário precisa ser removido manualmente")


def drill(token: str, identity_text: str, age_exe: Path, age_keygen: Path,
          progress=lambda _phase: None) -> dict[str, object]:
    progress("chave age")
    if not age_exe.is_file() or not age_keygen.is_file():
        raise RuntimeError("Programa age verificado não está disponível")
    with tempfile.TemporaryDirectory(prefix="psicogest-restore-") as directory:
        root = Path(directory)
        identity = root / "identity.txt"
        identity.write_text(extract_age_secret_line(identity_text) + "\n", encoding="ascii")
        try:
            recipient = checked_run([str(age_keygen), "-y", str(identity)], timeout=15)
            if recipient.returncode != 0:
                raise RuntimeError("A chave privada age não está em formato válido")
            if recipient.stdout.decode("ascii").strip() != AGE_RECIPIENT:
                raise RuntimeError("A chave age é válida, mas pertence a outro par")
            progress("manifesto Cloudflare KV")
            client = CloudflareKv(ACCOUNT_ID, NAMESPACE_ID, token)
            manifest_key = select_latest_manifest(client)
            progress("download cifrado")
            archive_dir = root / "download"
            manifest = download(client, manifest_key, archive_dir,
                                expected_project_ref=PROJECT_REF)
            progress("arquivo de objetos")
            object_manifest = inspect_archive(
                archive_dir / "objects.age", identity_file=str(identity),
                expected_source_project_ref=PROJECT_REF, age_executable=str(age_exe),
                expected_classification="PRODUCTION_CLINICAL", expected_bucket=BUCKET,
            )
            progress("banco local")
            table_count = restore_database_to_ram_container(
                age_exe, identity, archive_dir / "database.age")
            return {
                "snapshot_started_at": manifest["snapshot_started_at"],
                "database_tables": table_count,
                "storage_objects_verified": len(object_manifest["objects"]),
                "destination": "isolated-local-postgresql-17-ram",
            }
        finally:
            identity.unlink(missing_ok=True)


def page(message: str, nonce: str = "", *, refresh: bool = False) -> bytes:
    form = f"""
    <form method="post" action="/drill" autocomplete="off">
      <input type="hidden" name="nonce" value="{html.escape(nonce, quote=True)}">
      <label>Confirme digitando {CONFIRMATION}<br><input name="confirmation" required></label>
      <label>Token Cloudflare KV<br><input name="token" type="password" required autocomplete="off"></label>
      <label>Chave privada age completa<br><textarea name="identity" rows="4" required autocomplete="off"></textarea></label>
      <button type="submit">Restaurar em banco local isolado</button>
    </form>""" if nonce else ""
    meta = '<meta http-equiv="refresh" content="3;url=/status">' if refresh else ""
    return f"""<!doctype html><html lang="pt-BR"><meta charset="utf-8">{meta}
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>PsicoGest · restore isolado</title>
    <style>body{{font:16px system-ui;max-width:680px;margin:3rem auto;padding:0 1rem}}
    label{{display:block;margin:1.2rem 0}}input,textarea{{font:inherit;padding:.5rem;max-width:100%;width:95%}}
    button{{font:inherit;padding:.7rem 1rem}}</style>
    <h1>Ensaio local de recuperação</h1>
    <p>Origem: banco e bucket do projeto {PROJECT_REF}. Destino do banco: PostgreSQL 17 temporário,
    isolado da rede e armazenado apenas em memória. O Supabase original não será alterado.</p>
    <p>{html.escape(message)}</p>{form}
    <p>Credenciais usadas uma vez somente nesta máquina; não as envie no chat.</p></html>""".encode()


def serve(age_exe: Path, age_keygen: Path) -> None:
    nonce = secrets.token_urlsafe(32)
    state: dict[str, object] = {"used": False, "status": "Aguardando credenciais.", "running": False}
    lock = threading.Lock()

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *_args):
            pass

        def respond(self, code: int, body: bytes) -> None:
            self.send_response(code)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Referrer-Policy", "no-referrer")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'")
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self):
            if self.headers.get("Host") != f"{HOST}:{server.server_port}":
                self.respond(404, page("Página indisponível."))
                return
            with lock:
                used = bool(state["used"])
                running = bool(state["running"])
                status = str(state["status"])
            if self.path == "/":
                self.respond(200, page(status, "" if used else nonce))
            elif self.path == "/status":
                self.respond(200, page(status, refresh=running))
            else:
                self.respond(404, page("Página indisponível."))

        def do_POST(self):
            expected_origin = f"http://{HOST}:{server.server_port}"
            if (self.path != "/drill" or self.headers.get("Host") != f"{HOST}:{server.server_port}" or
                    not allowed_origin(self.headers.get("Origin"), expected_origin) or
                    self.headers.get("Content-Type", "").split(";", 1)[0] != "application/x-www-form-urlencoded"):
                self.respond(403, page("Pedido recusado."))
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
            except ValueError:
                length = 0
            if not 1 <= length <= 8192:
                self.respond(400, page("Formulário inválido."))
                return
            try:
                fields = urllib.parse.parse_qs(self.rfile.read(length).decode("utf-8"), strict_parsing=True)
            except (UnicodeDecodeError, ValueError):
                self.respond(400, page("Formulário inválido."))
                return
            if (fields.get("nonce") != [nonce] or fields.get("confirmation") != [CONFIRMATION] or
                    len(fields.get("token", [])) != 1 or len(fields.get("identity", [])) != 1):
                self.respond(403, page("Confirmação inválida."))
                return
            with lock:
                if state["used"]:
                    self.respond(403, page("Este ensaio já foi iniciado."))
                    return
                state["used"] = True
                state["running"] = True
                state["status"] = "Baixando arquivos cifrados e verificando a chave..."
            token = fields["token"][0]
            identity_text = fields["identity"][0]
            fields.clear()

            def work():
                phase = "início"

                def on_progress(new_phase: str):
                    nonlocal phase
                    phase = new_phase
                    with lock:
                        state["status"] = f"Verificando {phase}..."

                try:
                    result = drill(token, identity_text, age_exe, age_keygen, on_progress)
                    message = ("Recuperação local concluída: "
                               f"{result['database_tables']} tabelas no banco temporário; "
                               f"{result['storage_objects_verified']} objetos conferidos; "
                               f"snapshot {result['snapshot_started_at']}.")
                except Exception as exc:
                    # Exception text may contain provider details; show only category.
                    detail = safe_error_description(exc)
                    message = f"Ensaio falhou na etapa {phase} ({detail}). Os dados de origem não foram alterados."
                with lock:
                    state["status"] = message
                    state["running"] = False
                timer = threading.Timer(180, server.shutdown)
                timer.daemon = True
                timer.start()

            threading.Thread(target=work, daemon=True).start()
            self.respond(202, page("Ensaio em andamento; esta página atualiza automaticamente.", refresh=True))

    server = ThreadingHTTPServer((HOST, 0), Handler)
    print(f"http://{HOST}:{server.server_port}/", flush=True)
    try:
        server.serve_forever()
    finally:
        server.server_close()


if __name__ == "__main__":
    root = Path(os.environ.get("PSICOGEST_AGE_BIN_DIR", ""))
    serve(root / "age.exe", root / "age-keygen.exe")
