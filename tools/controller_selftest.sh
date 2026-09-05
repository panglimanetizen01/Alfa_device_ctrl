#!/usr/bin/env bash
# Positive and negative tests for the Android controller boundary.
set +e
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-run_20260823_171233_30513}"
PORT="${2:-8091}"
BASE="http://127.0.0.1:$PORT"
PASS=0
FAIL=0
check() {
  LABEL="$1"; EXPECT="$2"; shift 2
  OUTPUT=$("$@" 2>&1)
  if printf '%s' "$OUTPUT" | grep -q "$EXPECT"; then
    printf '%s=PASS\n' "$LABEL"; PASS=$((PASS + 1))
  else
    printf '%s=FAIL\n' "$LABEL"; printf '%s\n' "$OUTPUT"; FAIL=$((FAIL + 1))
  fi
}
check controller_health '"controller_status": "READY"' curl -fsS "$BASE/health"
POS=$(curl -fsS -X POST -H 'Content-Type: application/json' -d '{"request_id":"android-selftest","command":"printf controller-positive"}' "$BASE/execute")
printf '%s\n' "$POS"
if printf '%s' "$POS" | grep -q '"decision": "ALLOW"' && printf '%s' "$POS" | grep -q '"authorization_status": "AUTHORIZED"' && printf '%s' "$POS" | grep -q '"execution_status": "EXECUTED"' && printf '%s' "$POS" | grep -q '"result_status": "PASS"' && printf '%s' "$POS" | grep -q 'controller-positive'; then printf '%s\n' 'positive_command=PASS'; PASS=$((PASS + 1)); else printf '%s\n' 'positive_command=FAIL'; FAIL=$((FAIL + 1)); fi
NEG_EMPTY=$(curl -fsS -X POST -H 'Content-Type: application/json' -d '{"request_id":"android-empty","command":""}' "$BASE/execute")
if printf '%s' "$NEG_EMPTY" | grep -q '"authorization_status": "DENIED"' && printf '%s' "$NEG_EMPTY" | grep -q '"execution_status": "BLOCKED"'; then printf '%s\n' 'empty_command_denied=PASS'; PASS=$((PASS + 1)); else printf '%s\n' 'empty_command_denied=FAIL'; printf '%s\n' "$NEG_EMPTY"; FAIL=$((FAIL + 1)); fi
printf 'CONTROLLER_SELFTEST_PASS=%s\n' "$PASS"
printf 'CONTROLLER_SELFTEST_FAIL=%s\n' "$FAIL"
if [ "$FAIL" -eq 0 ]; then printf '%s\n' 'CONTROLLER_SELFTEST_STATUS=PASS'; else printf '%s\n' 'CONTROLLER_SELFTEST_STATUS=FAIL'; fi
