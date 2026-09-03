#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
INPUT="${ROOT}/artifacts/pipeline/${RUN_ID}/gate12/kernel.txt"
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate13/command.txt"
bash "$ROOT/tools/runtime_stage.sh" "gate13" "$RUN_ID" "$INPUT" "" "$OUTPUT"
