#!/usr/bin/env bash
# Gate 6 contract test: positive live run plus fail-closed provenance negatives.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
FAIL=0

if [ -z "$RUN_ID" ]; then
    printf '%s\n' 'G6_CONTRACT_TEST=BLOCKED'
    printf '%s\n' 'G6_CONTRACT_REASON=explicit pipeline_run_id is required'
    exit 1
fi

printf '%s\n' '=== GATE 6 CONTRACT TEST ==='

if bash "$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID" >/tmp/g6-positive.$$ 2>&1; then
    if grep -q '^GATE6_STATUS=PASS$' /tmp/g6-positive.$$; then
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
cp "$ROOT/artifacts/pipeline/$RUN_ID/gate5/decisions/gate5-self-test.txt" "$BAD_ROOT/gate5/decisions/gate5-self-test.txt" 2>/dev/null || true
cp "$ROOT/artifacts/pipeline/$RUN_ID/gate5/authorizations/gate5-self-test.txt" "$BAD_ROOT/gate5/authorizations/gate5-self-test.txt" 2>/dev/null || true

if ! bash "$ROOT/tools/runtime_bootstrap.sh" "$BAD_RUN" >/tmp/g6-negative.$$ 2>&1; then
    if grep -q '^GATE6_STATUS=BLOCKED$' /tmp/g6-negative.$$; then
        printf '%s\n' 'NEGATIVE_CROSS_RUN=PASS'
    else
        printf '%s\n' 'NEGATIVE_CROSS_RUN=FAIL'
        FAIL=1
    fi
else
    printf '%s\n' 'NEGATIVE_CROSS_RUN=FAIL'
    FAIL=1
fi

rm -rf "$BAD_ROOT" /tmp/g6-positive.$$ /tmp/g6-negative.$$ 2>/dev/null || true

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' 'G6_CONTRACT_TEST=PASS'
else
    printf '%s\n' 'G6_CONTRACT_TEST=FAIL'
fi

[ "$FAIL" -eq 0 ]
