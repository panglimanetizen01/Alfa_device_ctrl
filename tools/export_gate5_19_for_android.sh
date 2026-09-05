#!/usr/bin/env bash
# Export one explicit Gate 5-19 run to the UserLAnd Android storage bridge.
# Usage: bash tools/export_gate5_19_for_android.sh <pipeline_run_id>
set +e
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-}"
SRC="$ROOT/artifacts/pipeline/$RUN_ID"
DEST="/storage/internal/AlfaDeviceCtrl/artifacts/pipeline/$RUN_ID"

if [ -z "$RUN_ID" ]; then
  printf 'EXPORT_STATUS=BLOCKED\n'
  printf 'EXPORT_REASON=explicit pipeline_run_id is required\n'
elif [ ! -d "$SRC" ]; then
  printf 'EXPORT_STATUS=BLOCKED\n'
  printf 'EXPORT_REASON=source run directory is missing\n'
  printf 'SOURCE=%s\n' "$SRC"
elif [ ! -d /storage/internal ] || [ ! -w /storage/internal ]; then
  printf 'EXPORT_STATUS=BLOCKED\n'
  printf 'EXPORT_REASON=/storage/internal bridge is not writable\n'
else
  mkdir -p "$DEST"
  cp -a "$SRC/." "$DEST/"
  COPY_RC=$?
  if [ "$COPY_RC" -eq 0 ]; then
    printf 'EXPORT_STATUS=EXPORTED\n'
    printf 'PIPELINE_RUN_ID=%s\n' "$RUN_ID"
    printf 'EXPORT_ROOT=%s\n' "/storage/internal/AlfaDeviceCtrl"
    printf 'EXPORT_SOURCE=%s\n' "$SRC"
    printf 'EXPORT_DEST=%s\n' "$DEST"
  else
    printf 'EXPORT_STATUS=BLOCKED\n'
    printf 'EXPORT_REASON=copy failed\n'
    printf 'COPY_RC=%s\n' "$COPY_RC"
  fi
fi
