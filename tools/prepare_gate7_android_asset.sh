#!/usr/bin/env bash
# Prepare the exact Gate 6 provenance consumed by the Android runtime session.
# Usage: ALFA_GATE7_ASSET_OUT=<generated asset path> bash tools/prepare_gate7_android_asset.sh <pipeline_run_id>
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-}"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
GATE6="${ALFA_GATE6_BOOTSTRAP:-$RUN_DIR/gate6/bootstrap.txt}"
OUT="${ALFA_GATE7_ASSET_OUT:-$ROOT/app/build/generated/assets/gate7-launch.properties}"
RUNTIME=${RUNTIME_ID:-debian}

[ -n "$RUN_ID" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=PIPELINE_RUN_ID_REQUIRED'; exit 20; }
[ -f "$GATE6" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=GATE6_BOOTSTRAP_MISSING'; exit 20; }

field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,"", $0); print; exit}' "$GATE6"; }
RUN=$(field pipeline_run_id)
STATUS=$(field gate_status)
BOOTSTRAP_STATUS=$(field bootstrap_status)
SOURCE=$(field source_commit)
CONTRACT=$(field gate4_contract_sha256)
PROFILE=$(field profile_sha256)
IMPL=$(field implementation_commit)
REGISTRY=$(field runtime_registry_sha256)
DECISION=$(field decision_id)
REQUEST=$(field request_id)
AUTH=$(field authorization_status)

[ "$RUN" = "$RUN_ID" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=RUN_ID_MISMATCH'; exit 20; }
[ "$STATUS" = PASS ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=GATE6_NOT_PASS'; exit 20; }
[ "$BOOTSTRAP_STATUS" = PASS ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=GATE6_BOOTSTRAP_NOT_PASS'; exit 20; }
[ "$RUNTIME" = debian ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=RUNTIME_ID_NOT_DEBIAN'; exit 20; }
[ "$AUTH" = AUTHORIZED ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=GATE6_AUTHORIZATION_NOT_AUTHORIZED'; exit 20; }
printf '%s\n' "$SOURCE" | grep -Eq '^[0-9a-fA-F]{40}$'
printf '%s\n' "$IMPL" | grep -Eq '^[0-9a-fA-F]{40}$'
printf '%s\n' "$CONTRACT" | grep -Eq '^[0-9a-fA-F]{64}$'
printf '%s\n' "$PROFILE" | grep -Eq '^[0-9a-fA-F]{64}$'
printf '%s\n' "$REGISTRY" | grep -Eq '^[0-9a-fA-F]{64}$'
[ -n "$DECISION" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=DECISION_ID_MISSING'; exit 20; }
[ -n "$REQUEST" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=REQUEST_ID_MISSING'; exit 20; }

mkdir -p "$(dirname "$OUT")"
tmp="$OUT.part.$$"
{
  printf 'schema_version=gate7-launch.v1\n'
  printf 'gate=gate7\n'
  printf 'gate_status=READY\n'
  printf 'launch_status=AUTHORIZED\n'
  printf 'launch_source=gate6-bootstrap\n'
  printf 'pipeline_run_id=%s\n' "$RUN_ID"
  printf 'runtime_id=%s\n' "$RUNTIME"
  printf 'runtime_registry_sha256=%s\n' "$REGISTRY"
  printf 'source_commit=%s\n' "$SOURCE"
  printf 'gate4_contract_sha256=%s\n' "$CONTRACT"
  printf 'profile_sha256=%s\n' "$PROFILE"
  printf 'implementation_commit=%s\n' "$IMPL"
  printf 'decision_id=%s\n' "$DECISION"
  printf 'request_id=%s\n' "$REQUEST"
  printf 'authorization_status=AUTHORIZED\n'
} > "$tmp"
mv "$tmp" "$OUT"
printf 'GATE7_ASSET_STATUS=PASS\n'
printf 'GATE7_ASSET=%s\n' "$OUT"
printf 'PIPELINE_RUN_ID=%s\n' "$RUN_ID"
