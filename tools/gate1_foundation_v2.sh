#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
OUT_DIR="${G1_EVIDENCE_DIR:-$ROOT/artifacts/gates/g1}"
OUT="$OUT_DIR/foundation-contract.txt"
mkdir -p "$OUT_DIR" || exit 1

ARCH="$ROOT/docs/ARCHITECTURE.md"
SPEC="$ROOT/docs/MULTI_DISTRO_RUNTIME_FOUNDATION_V1.md"
REGISTRY="$ROOT/runtime/runtimes.v1.json"

[ -f "$ARCH" ] || { printf '%s\n' 'G1_STATUS=RED' 'G1_REASON=architecture-document-missing'; exit 1; }
[ -f "$SPEC" ] || { printf '%s\n' 'G1_STATUS=RED' 'G1_REASON=foundation-spec-missing'; exit 1; }
[ -f "$REGISTRY" ] || { printf '%s\n' 'G1_STATUS=RED' 'G1_REASON=runtime-registry-missing'; exit 1; }

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

# G1 must prove that CI/local execution is against a complete, internally
# readable Git source tree. A missing/corrupt object must never be allowed to
# produce GREEN evidence.
fsck = subprocess.run(
    ["git", "-C", str(root), "fsck", "--full", "--no-progress"],
    text=True,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
)
if fsck.returncode != 0 or any(
    token in fsck.stdout.lower()
    for token in ("missing blob", "missing tree", "missing commit", "corrupt", "error:")
):
    errors.append("git-object-integrity-failed")

head = subprocess.run(
    ["git", "-C", str(root), "rev-parse", "HEAD"],
    text=True,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
)
if head.returncode != 0:
    errors.append("git-head-unreadable")
    source_commit = "UNKNOWN"
else:
    source_commit = head.stdout.strip()

for marker in [
    "Multi-Distro Linux Runtime Foundation",
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
]:
    if marker not in arch:
        errors.append(f"architecture-missing:{marker}")

if "without depending on Alpine Linux" in arch:
    errors.append("legacy-alpine-exclusion-remains")
if "EXECUTION ENVIRONMENT:\nUbuntu UserLAnd" in arch:
    errors.append("userland-still-described-as-product-runtime")

# The foundation specification expresses the fail-closed sequence as
# "G1 GREEN → ... → G19 GREEN". Validate that exact contract wording rather
# than requiring a stale shorthand that is not present in the specification.
for marker in [
    "G1 Contract",
    "distribution-neutral",
    "source_commit",
    "G1 GREEN",
    "G1 does **not** claim",
    "G1 GREEN → G2 GREEN → G3 GREEN → ... → G19 GREEN",
    "Debian",
    "Ubuntu",
    "Alpine",
    "Kali",
]:
    if marker not in spec:
        errors.append(f"spec-missing:{marker}")

if registry.get("schema_version") != "runtime-registry.v1":
    errors.append("registry-schema-invalid")
if registry.get("engine_contract") != "multi-distro-linux-runtime.v1":
    errors.append("registry-engine-contract-invalid")
if registry.get("target_architecture") != "arm64-v8a":
    errors.append("registry-target-abi-invalid")

runtimes = registry.get("runtimes")
expected = ["debian", "ubuntu", "alpine", "kali"]
if not isinstance(runtimes, list) or [r.get("runtime_id") for r in runtimes] != expected:
    errors.append("registry-order-invalid")
else:
    for order, item in enumerate(runtimes, 1):
        if item.get("acceptance_status") != "SUPPORTED":
            errors.append(f"runtime-not-supported:{item.get('runtime_id')}")
        if item.get("acceptance_order") != order:
            errors.append(f"runtime-order-invalid:{item.get('runtime_id')}")
        if item.get("architecture") != "aarch64":
            errors.append(f"runtime-arch-invalid:{item.get('runtime_id')}")
        if item.get("shell_path") != "/bin/sh":
            errors.append(f"runtime-shell-invalid:{item.get('runtime_id')}")

status = "GREEN" if not errors else "RED"
out = root / "artifacts/gates/g1/foundation-contract.txt"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join([
    "schema_version=g1-foundation-contract.v2",
    "gate=G1",
    f"gate_status={status}",
    f"source_commit={source_commit}",
    "contract_identity=multi-distro-linux-runtime.v1",
    "architecture_contract=MULTI_DISTRO_LINUX_RUNTIME_FOUNDATION",
    "runtime_engine_contract=DISTRIBUTION_NEUTRAL",
    "gate_sequence=G1>G2>G3>G4>G5>G6>G7>G8>G9>G10>G11>G12>G13>G14>G15>G16>G17>G18>G19",
    "acceptance_order=debian>ubuntu>alpine>kali",
    "stale_evidence_policy=REJECT",
    "unknown_policy=REJECT_AS_PASS",
    "apk_release_before_g19=FORBIDDEN",
    *( ["reason=" + ";".join(errors)] if errors else [] ),
]) + "\n", encoding="utf-8")
print(f"G1_STATUS={status}")
print(f"G1_SOURCE_COMMIT={source_commit}")
print(f"G1_EVIDENCE={out}")
if errors:
    print("G1_REASON=" + ";".join(errors))
    raise SystemExit(1)
PY

cat "$OUT"
