#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT=${2:-}
OUTPUT=${3:-}
if [ -z "$RUN_ID" ] || [ -z "$INPUT" ] || [ -z "$OUTPUT" ]; then
    printf '%s\n' 'G12_STATUS=BLOCKED'
    printf '%s\n' 'G12_REASON=explicit run id, G11 artifact, and output are required'
    return 0 2>/dev/null || true
fi
bash "$ROOT/tools/gate12_runtime_kernel.sh" "$RUN_ID" "$INPUT" "$OUTPUT"
