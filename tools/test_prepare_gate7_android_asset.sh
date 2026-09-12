#!/usr/bin/env bash
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
RUN_ID='run_test_gate7_asset'
GATE6="$TMP/gate6-bootstrap.txt"
OUT="$TMP/gate7-launch.properties"
cat > "$GATE6" <<EOF
schema_version=gate6-bootstrap.v1
gate=gate6
gate_status=PASS
pipeline_run_id=$RUN_ID
source_commit=0123456789abcdef0123456789abcdef01234567
implementation_commit=89abcdef0123456789abcdef0123456789abcdef
runtime_id=debian
runtime_registry_sha256=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
gate4_contract_sha256=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
profile_sha256=1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef
decision_id=decision-test
request_id=request-test
authorization_status=AUTHORIZED
bootstrap_status=PASS
EOF
ALFA_GATE6_BOOTSTRAP="$GATE6" ALFA_GATE7_ASSET_OUT="$OUT" RUNTIME_ID=debian bash "$ROOT/tools/prepare_gate7_android_asset.sh" "$RUN_ID"
grep -Fxq 'schema_version=gate7-launch.v1' "$OUT"
grep -Fxq 'gate=gate7' "$OUT"
grep -Fxq 'gate_status=READY' "$OUT"
grep -Fxq 'launch_status=AUTHORIZED' "$OUT"
grep -Fxq 'launch_source=gate6-bootstrap' "$OUT"
grep -Fxq 'authorization_status=AUTHORIZED' "$OUT"
grep -Fxq "pipeline_run_id=$RUN_ID" "$OUT"
grep -Fxq 'runtime_id=debian' "$OUT"
grep -Fxq 'decision_id=decision-test' "$OUT"
grep -Fxq 'request_id=request-test' "$OUT"
echo PREPARE_GATE7_ANDROID_ASSET_TEST=PASS
