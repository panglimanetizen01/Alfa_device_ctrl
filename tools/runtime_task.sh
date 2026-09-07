#!/usr/bin/env bash
# Gate 8 entrypoint. Requires explicit current-run Gate 7 evidence.
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate7/session.txt"
OUTPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate8/task.txt"

if [ -z "$RUN_ID" ]; then
    printf '%s\n' 'G8_STATUS=BLOCKED'
    printf '%s\n' 'G8_REASON=explicit pipeline run id required'
    exit 1
fi

bash "$ROOT/tools/gate8_runtime_task.sh" "$RUN_ID" "$INPUT" "$OUTPUT"
