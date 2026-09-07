#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
OUT_DIR="${G1_EVIDENCE_DIR:-$ROOT/artifacts/gates/g1}"
OUT="$OUT_DIR/foundation-contract.txt"

mkdir -p "$OUT_DIR" || {
    printf '%s\n' 'G1_STATUS=RED' 'G1_REASON=cannot-create-evidence-directory'
    exit 1
}

fail() {
    local reason=$1
    {
        printf '%s\n' 'schema_version=g1-foundation-contract.v1'
        printf '%s\n' 'gate=G1'
        printf '%s\n' 'gate_status=RED'
        printf 'source_commit=%s\n' "$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf UNKNOWN)"
        printf 'contract_identity=%s\n' 'multi-distro-linux-runtime.v1'
        printf 'reason=%s\n' "$reason"
    } > "$OUT"
    cat "$OUT"
    exit 1
}

ARCH="$ROOT/docs/ARCHITECTURE.md"
SPEC="$ROOT/docs/MULTI_DISTRO_RUNTIME_FOUNDATION_V1.md"
REGISTRY="$ROOT/runtime/runtimes.v1.json"

[ -f "$ARCH" ] || fail 'architecture-document-missing'
[ -f "$SPEC" ] || fail 'foundation-spec-missing'
[ -f "$REGISTRY" ] || fail 'runtime-registry-missing'

python3 - "$ROOT" "$ARCH" "$SPEC" "$REGISTRY" <<'PY'
import json
import pathlib
import subprocess
import sys

root = pathlib.Path(sys.argv[1])
arch = pathlib.Path(sys.argv[2]).read_text(encoding="utf-8")
spec = pathlib.Path(sys.argv[3]).read_text(encoding="utf-8")
registry = json.loads(pathlib.Path(sys.argv[4]).read_text(encoding="utf-8"))

errors = []
required_architecture = [
    "MULTI-DISTRO",
    "Linux runtime foundation",
    "Android host integration",
    "Linux guest userspaces",
    "Debian",
    "Ubuntu",
    "Alpine",
    "Kali",
    "Generic compatible rootfs",
    "G1 → G2 → G3 → ... → G19",
    "Historical evidence from another source commit cannot satisfy the current gate.",
]
for marker in required_architecture:
    if marker not in arch:
        errors.append(f"architecture-missing:{marker}")

if "without depending on Alpine Linux" in arch:
    errors.append("legacy-alpine-exclusion-remains")
if "Ubuntu UserLAnd" in arch and "Historical Ubuntu UserLAnd is an execution aid only" not in arch:
    errors.append("userland-still-described-as-product-runtime")

required_spec = [
    "G1 Contract",
    "distribution-neutral",
    "source_commit",
    "G1 GREEN",
    "G1 does **not** claim",
    "G1 → G2 → G3 → ... → G19",
    "Debian",
    "Ubuntu",
    "Alpine",
    "Kali",
]
for marker in required_spec:
    if marker not in spec:
        errors.append(f"spec-missing:{marker}")

if registry.get("schema_version") != "runtime-registry.v1":
    errors.append("registry-schema-invalid")
if registry.get("engine_contract") != "multi-distro-linux-runtime.v1":
    errors.append("registry-engine-contract-invalid")
if registry.get("target_architecture") != "arm64-v8a":
    errors.append("registry-target-abi-invalid")

runtimes = registry.get("runtimes")
if not isinstance(runtimes, list) or len(runtimes) != 5:
    errors.append("registry-runtime-count-invalid")
else:
    expected = ["debian", "ubuntu", "alpine", "kali", "generic"]
    ids = [item.get("runtime_id") for item in runtimes]
    if ids != expected:
        errors.append(f"registry-order-invalid:{ids}")
    for index, item in enumerate(runtimes, start=1):
        if item.get("acceptance_status") != "PLANNED":
            errors.append(f"runtime-not-planned:{item.get('runtime_id')}")
        if item.get("acceptance_order") != index:
            errors.append(f"runtime-order-invalid:{item.get('runtime_id')}")
        if item.get("architecture") != "aarch64":
            errors.append(f"runtime-arch-invalid:{item.get('runtime_id')}")
        if item.get("shell_path") != "/bin/sh":
            errors.append(f"runtime-shell-invalid:{item.get('runtime_id')}")

try:
    source_commit = subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
    ).strip()
except subprocess.CalledProcessError:
    source_commit = "UNKNOWN"
    errors.append("source-commit-unavailable")

out = root / "artifacts/gates/g1/foundation-contract.txt"
out.parent.mkdir(parents=True, exist_ok=True)
status = "GREEN" if not errors else "RED"
lines = [
    "schema_version=g1-foundation-contract.v1",
    "gate=G1",
    f"gate_status={status}",
    f"source_commit={source_commit}",
    "contract_identity=multi-distro-linux-runtime.v1",
    "architecture_contract=MULTI_DISTRO_LINUX_RUNTIME_FOUNDATION",
    "runtime_engine_contract=DISTRIBUTION_NEUTRAL",
    "gate_sequence=G1>G2>G3>G4>G5>G6>G7>G8>G9>G10>G11>G12>G13>G14>G15>G16>G17>G18>G19",
    "acceptance_order=debian>ubuntu>alpine>kali>generic",
    "stale_evidence_policy=REJECT",
    "unknown_policy=REJECT_AS_PASS",
    "apk_release_before_g19=FORBIDDEN",
]
if errors:
    lines.append("reason=" + ";".join(errors))
out.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("G1_STATUS=" + status)
print("G1_EVIDENCE=" + str(out))
print("G1_SOURCE_COMMIT=" + source_commit)
if errors:
    print("G1_REASON=" + ";".join(errors))
    raise SystemExit(1)
PY

cat "$OUT"
