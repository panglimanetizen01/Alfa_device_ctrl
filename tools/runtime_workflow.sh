#!/usr/bin/env bash
# Gate 10 explicit-run entry point. No propagation fallback.
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate9/action.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate10/workflow.txt"
if [ -z "$RUN_ID" ]; then
    printf '%s\n' 'G10_STATUS=BLOCKED'
    printf '%s\n' 'G10_REASON=explicit pipeline run id required'
    exit 1
fi
bash "$ROOT/tools/gate10_runtime_workflow.sh" "$RUN_ID" "$INPUT" "$OUTPUT"
