#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate8/task.txt"
OUTPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate9/action.txt"

if [ -z "$RUN_ID" ]; then
    printf '%s\n' 'G9_STATUS=BLOCKED'
    printf '%s\n' 'G9_REASON=explicit pipeline run id required'
    exit 1
fi

bash "$ROOT/tools/gate9_runtime_action.sh" "$RUN_ID" "$INPUT" "$OUTPUT"
