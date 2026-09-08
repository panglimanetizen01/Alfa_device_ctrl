#!/usr/bin/env bash
# Gate 6 contract test: positive live run plus fail-closed provenance negatives.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
REQUEST_ID=${2:-}
FAIL=0
TMP_DIR="$ROOT/artifacts/.g6-test-tmp-$$"
POSITIVE_LOG="$TMP_DIR/g6-positive.log"
NEGATIVE_LOG="$TMP_DIR/g6-negative.log"

cleanup() {
    rm -rf "$TMP_DIR" "$ROOT/artifacts/pipeline/g6-negative-${RUN_ID}-$$" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM

if [ -z "$RUN_ID" ] || [ -z "$REQUEST_ID" ]; then
    printf '%s\n' 'G6_CONTRACT_TEST=BLOCKED'
    printf '%s\n' 'G6_CONTRACT_REASON=explicit pipeline_run_id and Gate 5 request_id are required'
    exit 1
fi

printf '%s\n' '=== GATE 6 CONTRACT TEST ==='

if ! mkdir -p "$TMP_DIR"; then
    printf '%s\n' 'G6_CONTRACT_TEST=FAIL'
    printf '%s\n' 'G6_CONTRACT_REASON=test temporary directory creation failed'
    exit 1
fi

if bash "$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID" "$REQUEST_ID" >"$POSITIVE_LOG" 2>&1; then
    if grep -q '^GATE6_STATUS=PASS$' "$POSITIVE_LOG"; then
        printf '%s\n' 'POSITIVE_CURRENT_RUN=PASS'
    else
        printf '%s\n' 'POSITIVE_CURRENT_RUN=FAIL'
        FAIL=1
    fi
else
    printf '%s\n' 'POSITIVE_CURRENT_RUN=FAIL'
    FAIL=1
fi

BAD_RUN="g6-negative-${RUN_ID}-$$"
BAD_ROOT="$ROOT/artifacts/pipeline/$BAD_RUN"
mkdir -p "$BAD_ROOT/gate4" "$BAD_ROOT/gate5/decisions" "$BAD_ROOT/gate5/authorizations"

cp "$ROOT/artifacts/pipeline/$RUN_ID/gate4/environment_contract.txt" "$BAD_ROOT/gate4/environment_contract.txt" 2>/dev/null || true
cp "$ROOT/artifacts/pipeline/$RUN_ID/gate5/decisions/$REQUEST_ID.txt" "$BAD_ROOT/gate5/decisions/$REQUEST_ID.txt" 2>/dev/null || true
cp "$ROOT/artifacts/pipeline/$RUN_ID/gate5/authorizations/$REQUEST_ID.txt" "$BAD_ROOT/gate5/authorizations/$REQUEST_ID.txt" 2>/dev/null || true

if ! bash "$ROOT/tools/runtime_bootstrap.sh" "$BAD_RUN" "$REQUEST_ID" >"$NEGATIVE_LOG" 2>&1; then
    if grep -q '^GATE6_STATUS=BLOCKED$' "$NEGATIVE_LOG"; then
        printf '%s\n' 'NEGATIVE_CROSS_RUN=PASS'
    else
        printf '%s\n' 'NEGATIVE_CROSS_RUN=FAIL'
        FAIL=1
    fi
else
    printf '%s\n' 'NEGATIVE_CROSS_RUN=FAIL'
    FAIL=1
fi

if bash "$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID" >"$NEGATIVE_LOG" 2>&1; then
    printf '%s\n' 'NEGATIVE_MISSING_REQUEST_ID=FAIL'
    FAIL=1
elif grep -q '^GATE6_STATUS=BLOCKED$' "$NEGATIVE_LOG"; then
    printf '%s\n' 'NEGATIVE_MISSING_REQUEST_ID=PASS'
else
    printf '%s\n' 'NEGATIVE_MISSING_REQUEST_ID=FAIL'
    FAIL=1
fi

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' 'G6_CONTRACT_TEST=PASS'
else
    printf '%s\n' 'G6_CONTRACT_TEST=FAIL'
fi

[ "$FAIL" -eq 0 ]
