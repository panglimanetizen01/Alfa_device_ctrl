#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate10/workflow.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate11/orchestrator.txt"
bash "$ROOT/tools/runtime_stage.sh" "gate11" "$RUN_ID" "$INPUT" "" "$OUTPUT"
