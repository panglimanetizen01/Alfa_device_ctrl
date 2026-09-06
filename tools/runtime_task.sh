#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate7/session.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate8/task.txt"
bash "$ROOT/tools/runtime_stage.sh" "gate8" "$RUN_ID" "$INPUT" "" "$OUTPUT"
