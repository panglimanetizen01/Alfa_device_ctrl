#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate5/decisions/gate5-self-test.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate6/bootstrap.txt"
bash "$ROOT/tools/runtime_stage.sh" "gate6" "$RUN_ID" "$INPUT" "" "$OUTPUT"
