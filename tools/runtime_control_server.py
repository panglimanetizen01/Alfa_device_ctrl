#!/usr/bin/env python3
"""Local Alfa Device Ctrl command server.

Bind to 127.0.0.1 only. The server is intentionally local and runs under the
same UserLAnd user that owns the project. Every command gets a separate Gate 5
request -> decision -> authorization -> execution record.
"""
from __future__ import annotations

from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_RUN_ID = "run_20260823_171233_30513"
MAX_COMMAND = 4096
MAX_OUTPUT = 65536
TIMEOUT_SECONDS = 30


def now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def field(path: Path, key: str) -> str:
    try:
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            if line.startswith(key + "="):
                return line.split("=", 1)[1]
    except OSError:
        pass
    return ""


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()
    except OSError:
        return ""


def baseline(run_id: str) -> dict[str, str]:
    run = ROOT / "artifacts" / "pipeline" / run_id
    contract = run / "gate4" / "environment_contract.txt"
    request = run / "gate5" / "requests" / "gate5-self-test.txt"
    authorization = run / "gate5" / "authorizations" / "gate5-self-test.txt"
    return {
        "pipeline_run_id": run_id,
        "source_commit": field(request, "source_commit"),
        "gate4_contract_sha256": sha256(contract),
        "profile_sha256": field(request, "profile_sha256"),
        "policy_version": "gate5-controller-v1",
        "gate4_result": field(contract, "contract_result"),
        "authorization_status": field(authorization, "authorization_status"),
    }


def safe_id(value: str) -> str:
    cleaned = "".join(ch if ch.isalnum() or ch in "-_" else "-" for ch in value)
    return cleaned[:80] or "request"


def write_record(path: Path, values: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + ".partial-" + uuid.uuid4().hex)
    with temp.open("w", encoding="utf-8") as stream:
        for key, value in values.items():
            text = str(value).replace("\n", "\\n").replace("\r", "\\r")
            stream.write(key + "=" + text + "\n")
    os.replace(temp, path)


def make_chain(payload: dict[str, object]) -> dict[str, object]:
    run_id = str(payload.get("pipeline_run_id") or DEFAULT_RUN_ID)
    request_id = safe_id(str(payload.get("request_id") or ("android-" + uuid.uuid4().hex[:12])))
    command = str(payload.get("command") or "")
    request_time = now()
    base = baseline(run_id)
    run = ROOT / "artifacts" / "pipeline" / run_id / "gate5"
    request_path = run / "requests" / (request_id + ".txt")
    decision_path = run / "decisions" / (request_id + ".txt")
    authorization_path = run / "authorizations" / (request_id + ".txt")
    execution_path = run / "executions" / (request_id + ".txt")
    identity = {
        "request_id": request_id,
        "pipeline_run_id": run_id,
        "source_commit": base["source_commit"],
        "gate4_contract_sha256": base["gate4_contract_sha256"],
        "profile_sha256": base["profile_sha256"],
        "policy_version": "gate5-controller-v1",
    }
    write_record(request_path, {"schema_version": "gate5-controller-request.v1", "created_at": request_time, "command": command, **identity})

    reason = ""
    if not command.strip():
        reason = "command is empty"
    elif len(command) > MAX_COMMAND:
        reason = "command exceeds maximum length"
    elif "\x00" in command:
        reason = "command contains NUL"
    elif base["gate4_result"] != "VALID":
        reason = "Gate 4 contract is not VALID"
    elif base["authorization_status"] != "AUTHORIZED":
        reason = "Gate 5 baseline authorization is not AUTHORIZED"
    elif not base["source_commit"] or not base["gate4_contract_sha256"] or not base["profile_sha256"]:
        reason = "Gate 5 baseline identity is incomplete"
    decision = "DENY" if reason else "ALLOW"
    write_record(decision_path, {"schema_version": "gate5-controller-decision.v1", "created_at": now(), "decision": decision, "decision_reason": reason or "Gate 4 VALID and controller policy matched", **identity})

    auth_reason = ""
    if decision != "ALLOW":
        auth_reason = "decision is not ALLOW"
    elif run_id != identity["pipeline_run_id"]:
        auth_reason = "pipeline_run_id mismatch"
    elif not all(identity[key] for key in ("source_commit", "gate4_contract_sha256", "profile_sha256")):
        auth_reason = "identity binding incomplete"
    authorization_status = "DENIED" if auth_reason else "AUTHORIZED"
    write_record(authorization_path, {"schema_version": "gate5-controller-authorization.v1", "created_at": now(), "authorization_status": authorization_status, "authorization_reason": auth_reason or "decision and all identity checks passed", "decision": decision, **identity})

    result: dict[str, object] = {"request_id": request_id, "pipeline_run_id": run_id, "decision": decision, "authorization_status": authorization_status, "request_artifact": str(request_path), "decision_artifact": str(decision_path), "authorization_artifact": str(authorization_path)}
    if authorization_status != "AUTHORIZED":
        write_record(execution_path, {"schema_version": "gate5-controller-execution.v1", "created_at": now(), "execution_status": "BLOCKED", "result_status": "DENIED", "reason": auth_reason or reason or "authorization denied", **identity})
        result.update({"execution_status": "BLOCKED", "result_status": "DENIED", "stdout": "", "stderr": auth_reason or reason or "authorization denied", "exit_code": None, "execution_artifact": str(execution_path)})
        return result

    started = time.monotonic()
    try:
        completed = subprocess.run(command, shell=True, cwd=str(ROOT), executable="/bin/bash", capture_output=True, text=True, timeout=TIMEOUT_SECONDS)
        stdout = completed.stdout[-MAX_OUTPUT:]
        stderr = completed.stderr[-MAX_OUTPUT:]
        exit_code = completed.returncode
        execution_status = "EXECUTED"
        result_status = "PASS" if exit_code == 0 else "FAIL"
    except subprocess.TimeoutExpired as error:
        stdout = (error.stdout or "")[-MAX_OUTPUT:] if isinstance(error.stdout, str) else ""
        stderr = ((error.stderr or "") if isinstance(error.stderr, str) else "") + "\ncommand timeout"
        exit_code = None
        execution_status = "EXECUTED"
        result_status = "FAIL"
    except Exception as error:
        stdout = ""
        stderr = str(error)
        exit_code = None
        execution_status = "EXECUTION_ERROR"
        result_status = "FAIL"
    duration_ms = int((time.monotonic() - started) * 1000)
    write_record(execution_path, {"schema_version": "gate5-controller-execution.v1", "created_at": now(), "execution_status": execution_status, "result_status": result_status, "exit_code": exit_code if exit_code is not None else "NONE", "duration_ms": duration_ms, "stdout": stdout, "stderr": stderr, "command": command, **identity})
    result.update({"execution_status": execution_status, "result_status": result_status, "stdout": stdout, "stderr": stderr, "exit_code": exit_code, "duration_ms": duration_ms, "execution_artifact": str(execution_path)})
    return result


class Handler(BaseHTTPRequestHandler):
    server_version = "AlfaDeviceCtrl/1.0"

    def log_message(self, format: str, *args: object) -> None:
        sys.stderr.write("[alfa-controller] " + (format % args) + "\n")

    def send_json(self, status: int, value: object) -> None:
        body = json.dumps(value, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        if self.path == "/health":
            self.send_json(200, {"controller_status": "READY", "pipeline_run_id": self.server.run_id, "bind": "127.0.0.1"})
        elif self.path == "/status":
            self.send_json(200, {"controller_status": "READY", "baseline": baseline(self.server.run_id)})
        else:
            self.send_json(404, {"error": "not found"})

    def do_POST(self) -> None:
        if self.path != "/execute":
            self.send_json(404, {"error": "not found"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 16384:
                self.send_json(400, {"error": "invalid body length"})
                return
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            if not isinstance(payload, dict):
                self.send_json(400, {"error": "JSON object required"})
                return
            payload["pipeline_run_id"] = self.server.run_id
            self.send_json(200, make_chain(payload))
        except Exception as error:
            self.send_json(500, {"error": type(error).__name__, "detail": str(error)})


def main() -> None:
    run_id = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_RUN_ID
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 8091
    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    server.run_id = run_id
    print("CONTROLLER_STATUS=READY")
    print("PIPELINE_RUN_ID=" + run_id)
    print("CONTROLLER_URL=http://127.0.0.1:" + str(port))
    server.serve_forever()


if __name__ == "__main__":
    main()
