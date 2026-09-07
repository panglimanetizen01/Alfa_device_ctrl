#!/usr/bin/env bash
# Gate 7 V1 entrypoint: verify an Android-produced runtime session attestation.
# This gate never promotes Gate 6 bootstrap into session PASS and never discovers artifacts implicitly.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
BOOTSTRAP=${2:-}
EVIDENCE=${3:-}

if [ -z "$RUN_ID" ] || [ -z "$BOOTSTRAP" ] || [ -z "$EVIDENCE" ]; then
    printf '%s\n' 'GATE7_STATUS=BLOCKED'
    printf '%s\n' 'GATE7_FAILURE_REASON=explicit run_id, Gate6 bootstrap path, and Android session evidence path are required'
    exit 1
fi

exec bash "$ROOT/tools/gate7_runtime_session.sh" "$RUN_ID" "$BOOTSTRAP" "$EVIDENCE"
