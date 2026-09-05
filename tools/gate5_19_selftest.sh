#!/usr/bin/env bash
# End-to-end Gate 5 -> Gate 19 verification.
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
FAIL=0
GATE5_OUT="/tmp/gate5-selftest-$RUN_ID.$$"
FULL_OUT="/tmp/gate5-19-selftest-$RUN_ID.$$"
bash "$ROOT/tools/gate5_selftest.sh" "$RUN_ID" > "$GATE5_OUT" 2>&1
if ! grep -q '^SELFTEST_STATUS=PASS$' "$GATE5_OUT" 2>/dev/null; then FAIL=1; fi
bash "$ROOT/tools/runtime_full_chain.sh" "$RUN_ID" > "$FULL_OUT" 2>&1
cat "$GATE5_OUT"
cat "$FULL_OUT"
if ! grep -q '^FULL_CHAIN_STATUS=PASS$' "$FULL_OUT" 2>/dev/null; then FAIL=1; fi
if [ "$FAIL" -eq 0 ]; then printf '%s\n' 'GATE5_TO_19_SELFTEST_STATUS=PASS'; else printf '%s\n' 'GATE5_TO_19_SELFTEST_STATUS=FAIL'; fi
