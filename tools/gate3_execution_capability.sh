#!/usr/bin/env bash

set -u

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
EVIDENCE_DIR="$PROJECT_ROOT/artifacts/gates/g3"
EVIDENCE_FILE="$EVIDENCE_DIR/execution-capability.txt"
CAPABILITY_SCRIPT="$PROJECT_ROOT/tools/execution_capability.sh"
G2_EVIDENCE="$PROJECT_ROOT/artifacts/gates/g2/canonical-source-build-boundary.txt"
EXPECTED_COMMIT="${G3_EXPECT_SOURCE_COMMIT:-}"
SHARED_ROOT="${ALFA_EXEC_SHARED_ROOT:-/storage/emulated/0}"

fail() {
    printf '%s\n' 'G3_STATUS=RED'
    printf '%s\n' "G3_REASON=$1"
    return 1
}

[ -n "$PROJECT_ROOT" ] || { fail 'PROJECT_ROOT_UNRESOLVED'; exit 1; }
[ -f "$CAPABILITY_SCRIPT" ] || { fail 'CAPABILITY_SCRIPT_NOT_FOUND'; exit 1; }

SOURCE_COMMIT=$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || true)
printf '%s' "$SOURCE_COMMIT" | grep -Eq '^[0-9a-f]{40}$' || { fail 'MALFORMED_SOURCE_COMMIT'; exit 1; }

if [ -n "$EXPECTED_COMMIT" ] && [ "$SOURCE_COMMIT" != "$EXPECTED_COMMIT" ]; then
    fail 'STALE_EXPECTED_SOURCE_COMMIT'
    exit 1
fi

[ -s "$G2_EVIDENCE" ] || { fail 'G2_EVIDENCE_MISSING'; exit 1; }
G2_STATUS=$(grep '^gate_status=' "$G2_EVIDENCE" 2>/dev/null | sed 's/^gate_status=//' | sed -n '1p')
G2_SOURCE=$(grep '^source_commit=' "$G2_EVIDENCE" 2>/dev/null | sed 's/^source_commit=//' | sed -n '1p')
G2_SCHEMA=$(grep '^schema_version=' "$G2_EVIDENCE" 2>/dev/null | sed 's/^schema_version=//' | sed -n '1p')
G2_GATE=$(grep '^gate=' "$G2_EVIDENCE" 2>/dev/null | sed 's/^gate=//' | sed -n '1p')
[ "$G2_SCHEMA" = 'g2-canonical-source-build-boundary.v1' ] || { fail 'G2_SCHEMA_INVALID'; exit 1; }
[ "$G2_GATE" = 'G2' ] || { fail 'G2_GATE_INVALID'; exit 1; }
[ "$G2_STATUS" = 'GREEN' ] || { fail 'G2_NOT_GREEN'; exit 1; }
[ "$G2_SOURCE" = "$SOURCE_COMMIT" ] || { fail 'G2_STALE_SOURCE_COMMIT'; exit 1; }

mkdir -p "$EVIDENCE_DIR" || { fail 'EVIDENCE_DIRECTORY_CREATE_FAILED'; exit 1; }
TMP_OUTPUT="$EVIDENCE_DIR/.execution-capability.$$"

if ALFA_EXEC_SHARED_ROOT="$SHARED_ROOT" bash "$CAPABILITY_SCRIPT" > "$TMP_OUTPUT" 2>&1; then
    PRODUCER_RC=0
else
    PRODUCER_RC=$?
fi

get_status() {
    grep "^$1=" "$TMP_OUTPUT" 2>/dev/null | sed 's/^[^=]*=//' | sed 's/[[:space:]].*$//' | sed -n '1p'
}

EXEC_PRIVATE=$(get_status EXEC_PRIVATE)
EXEC_SHARED=$(get_status EXEC_SHARED)
SCRIPT_BASH=$(get_status SCRIPT_BASH)
SCRIPT_PYTHON=$(get_status SCRIPT_PYTHON)
PROCESS_SPAWN=$(get_status PROCESS_SPAWN)
PRODUCER_STATUS=$(get_status GATE3_STATUS)
EXECUTION_PATH=$(grep '^execution_path=' "$TMP_OUTPUT" 2>/dev/null | sed 's/^execution_path=//' | sed -n '1p')

if [ "$PRODUCER_RC" -eq 0 ] \
   && [ "$PRODUCER_STATUS" = 'PASS' ] \
   && [ "$EXEC_PRIVATE" = 'PASS' ] \
   && [ "$EXEC_SHARED" = 'PASS' ] \
   && [ "$SCRIPT_BASH" = 'PASS' ] \
   && [ "$SCRIPT_PYTHON" = 'PASS' ] \
   && [ "$PROCESS_SPAWN" = 'PASS' ]; then
    GATE_STATUS=GREEN
else
    GATE_STATUS=RED
fi

CREATED_AT=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || printf '%s' UNKNOWN)
{
    printf '%s\n' 'schema_version=g3-execution-capability.v1'
    printf '%s\n' 'gate=G3'
    printf '%s\n' "gate_status=$GATE_STATUS"
    printf '%s\n' "source_commit=$SOURCE_COMMIT"
    printf '%s\n' 'contract_identity=multi-distro-linux-runtime.v1'
    printf '%s\n' 'consumed_artifacts=artifacts/gates/g2/canonical-source-build-boundary.txt'
    printf '%s\n' "execution_path=${EXECUTION_PATH:-UNKNOWN}"
    printf '%s\n' "shared_root=$SHARED_ROOT"
    printf '%s\n' "exec_private=${EXEC_PRIVATE:-UNKNOWN}"
    printf '%s\n' "exec_shared=${EXEC_SHARED:-UNKNOWN}"
    printf '%s\n' "script_bash=${SCRIPT_BASH:-UNKNOWN}"
    printf '%s\n' "script_python=${SCRIPT_PYTHON:-UNKNOWN}"
    printf '%s\n' "process_spawn=${PROCESS_SPAWN:-UNKNOWN}"
    printf '%s\n' "producer_rc=$PRODUCER_RC"
    printf '%s\n' "producer_status=${PRODUCER_STATUS:-UNKNOWN}"
    printf '%s\n' "created_at=$CREATED_AT"
} > "$EVIDENCE_FILE"

rm -f "$TMP_OUTPUT" 2>/dev/null || true

printf '%s\n' "G3_STATUS=$GATE_STATUS"
printf '%s\n' "G3_SOURCE_COMMIT=$SOURCE_COMMIT"
printf '%s\n' "G3_EVIDENCE=$EVIDENCE_FILE"
printf '%s\n' "EXEC_PRIVATE=${EXEC_PRIVATE:-UNKNOWN}"
printf '%s\n' "EXEC_SHARED=${EXEC_SHARED:-UNKNOWN}"
printf '%s\n' "SCRIPT_BASH=${SCRIPT_BASH:-UNKNOWN}"
printf '%s\n' "SCRIPT_PYTHON=${SCRIPT_PYTHON:-UNKNOWN}"
printf '%s\n' "PROCESS_SPAWN=${PROCESS_SPAWN:-UNKNOWN}"

[ "$GATE_STATUS" = GREEN ]
