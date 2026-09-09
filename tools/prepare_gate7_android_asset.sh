#!/usr/bin/env bash
# Prepare the exact Gate 6 provenance consumed by the Android runtime session.
# Usage: bash tools/prepare_gate7_android_asset.sh <pipeline_run_id>
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-}"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
GATE6="$RUN_DIR/gate6/bootstrap.txt"
OUT="$ROOT/app/src/main/assets/gate7-launch.properties"

[ -n "$RUN_ID" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=PIPELINE_RUN_ID_REQUIRED'; exit 20; }
[ -f "$GATE6" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=GATE6_BOOTSTRAP_MISSING'; exit 20; }

field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,""); print; exit}' "$GATE6"; }
RUN=$(field pipeline_run_id)
STATUS=$(field gate_status)
RUNTIME=${RUNTIME_ID:-debian}
SOURCE=$(field source_commit)
CONTRACT=$(field gate4_contract_sha256)
PROFILE=$(field profile_sha256)
IMPL=$(field implementation_commit)

[ "$RUN" = "$RUN_ID" ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=RUN_ID_MISMATCH'; exit 20; }
[ "$STATUS" = PASS ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=GATE6_NOT_PASS'; exit 20; }
[ "$RUNTIME" = debian ] || { echo 'GATE7_ASSET_STATUS=BLOCKED'; echo 'GATE7_ASSET_REASON=RUNTIME_ID_NOT_DEBIAN'; exit 20; }
printf '%s\n' "$SOURCE" | grep -Eq '^[0-9a-fA-F]{40}$'
printf '%s\n' "$IMPL" | grep -Eq '^[0-9a-fA-F]{40}$'
printf '%s\n' "$CONTRACT" | grep -Eq '^[0-9a-fA-F]{64}$'
printf '%s\n' "$PROFILE" | grep -Eq '^[0-9a-fA-F]{64}$'

mkdir -p "$(dirname "$OUT")"
tmp="$OUT.part.$$"
{
  printf 'pipeline_run_id=%s\n' "$RUN_ID"
  printf 'runtime_id=%s\n' "$RUNTIME"
  printf 'source_commit=%s\n' "$SOURCE"
  printf 'gate4_contract_sha256=%s\n' "$CONTRACT"
  printf 'profile_sha256=%s\n' "$PROFILE"
  printf 'implementation_commit=%s\n' "$IMPL"
} > "$tmp"
mv "$tmp" "$OUT"
printf 'GATE7_ASSET_STATUS=PASS\n'
printf 'GATE7_ASSET=%s\n' "$OUT"
printf 'PIPELINE_RUN_ID=%s\n' "$RUN_ID"
