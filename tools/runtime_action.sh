#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate8/task.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate9/action.txt"
bash "$ROOT/tools/runtime_stage.sh" "gate9" "$RUN_ID" "$INPUT" "" "$OUTPUT"
