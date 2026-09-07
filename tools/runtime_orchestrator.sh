#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate10/workflow.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate11/orchestrator.txt"
[ -n "$RUN_ID" ] || { echo 'G11_STATUS=BLOCKED'; echo 'G11_REASON=explicit run id required'; exit 1; }
bash "$ROOT/tools/gate11_runtime_orchestrator.sh" "$RUN_ID" "$INPUT" "$OUTPUT"
