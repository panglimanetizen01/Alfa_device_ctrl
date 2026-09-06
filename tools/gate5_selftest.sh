#!/usr/bin/env bash
# Gate 5 V1 positive and negative verification. No repository reset/delete.

set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$ROOT/.." 2>/dev/null && pwd)
RUN_ID=${1:-run_20260823_171233_30513}
RUN_DIR="$PROJECT_ROOT/artifacts/pipeline/$RUN_ID/gate5"
REQUEST="$RUN_DIR/requests/gate5-self-test.txt"
DECISION="$RUN_DIR/decisions/gate5-self-test.txt"
AUTH="$RUN_DIR/authorizations/gate5-self-test.txt"
EXEC="$RUN_DIR/executions/gate5-self-test.txt"

    SELFTEST_FAIL=0
printf '%s\n' '=== GATE 5 V1 SELFTEST ==='
if ! bash "$PROJECT_ROOT/tools/runtime_decision.sh" "$RUN_ID" gate5-self-test local-runtime pwd runtime purpose=self-test >/tmp/gate5_decision_selftest.$$.out 2>&1; then
    SELFTEST_FAIL=1
    printf '%s\n' 'valid_gate4_to_allow=FAIL'
else
    cat /tmp/gate5_decision_selftest.$$.out
fi
bash "$PROJECT_ROOT/tools/runtime_execution_authorize.sh" "$REQUEST" "$DECISION" "$AUTH"
bash "$PROJECT_ROOT/tools/runtime_execution.sh" "$REQUEST" "$DECISION" "$AUTH" "$EXEC"

VALID_DECISION=$(grep '^decision=' "$DECISION" 2>/dev/null | awk -F= '{print $2}')
VALID_AUTH=$(grep '^authorization_status=' "$AUTH" 2>/dev/null | awk -F= '{print $2}')
VALID_EXEC=$(grep '^execution_status=' "$EXEC" 2>/dev/null | awk -F= '{print $2}')
VALID_RESULT=$(grep '^result_status=' "$EXEC" 2>/dev/null | awk -F= '{print $2}')
if [ "$VALID_DECISION" = 'ALLOW' ] && [ "$VALID_AUTH" = 'AUTHORIZED' ] && [ "$VALID_EXEC" = 'EXECUTED' ] && [ "$VALID_RESULT" = 'PASS' ]; then
    printf '%s\n' 'valid_gate4_to_allow=PASS'
else
    SELFTEST_FAIL=1
    printf '%s\n' 'valid_gate4_to_allow=FAIL'
fi

run_negative() {
    local NAME MUTATION REQUEST_COPY DECISION_COPY AUTH_COPY EXEC_COPY A E R
    NAME=$1
    MUTATION=$2
    REQUEST_COPY="/tmp/gate5-$NAME-request.$$"
    DECISION_COPY="/tmp/gate5-$NAME-decision.$$"
    AUTH_COPY="/tmp/gate5-$NAME-auth.$$"
    EXEC_COPY="/tmp/gate5-$NAME-exec.$$"
    cp "$REQUEST" "$REQUEST_COPY" 2>/dev/null
    cp "$DECISION" "$DECISION_COPY" 2>/dev/null
    if [ "$MUTATION" = 'missing_decision' ]; then
        DECISION_COPY="/tmp/gate5-$NAME-missing.$$"
    elif [ "$MUTATION" = 'invalid_decision' ]; then
        sed -i 's/^decision=ALLOW$/decision=READY/' "$DECISION_COPY"
    elif [ "$MUTATION" = 'stale_decision' ]; then
        sed -i 's/^created_at=.*/created_at=2000-01-01T00:00:00Z/' "$DECISION_COPY"
    elif [ "$MUTATION" = 'wrong_request_id' ]; then
        sed -i 's/^request_id=.*/request_id=other-request/' "$REQUEST_COPY"
    elif [ "$MUTATION" = 'wrong_pipeline_run_id' ]; then
        sed -i 's/^pipeline_run_id=.*/pipeline_run_id=wrong-run/' "$REQUEST_COPY"
    elif [ "$MUTATION" = 'wrong_source_commit' ]; then
        sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$REQUEST_COPY"
    elif [ "$MUTATION" = 'wrong_gate4_hash' ]; then
        sed -i 's/^gate4_contract_sha256=.*/gate4_contract_sha256=0000000000000000000000000000000000000000000000000000000000000000/' "$REQUEST_COPY"
    elif [ "$MUTATION" = 'wrong_profile_hash' ]; then
        sed -i 's/^profile_sha256=.*/profile_sha256=0000000000000000000000000000000000000000000000000000000000000000/' "$REQUEST_COPY"
    fi
    bash "$PROJECT_ROOT/tools/runtime_execution_authorize.sh" "$REQUEST_COPY" "$DECISION_COPY" "$AUTH_COPY" >/tmp/gate5-$NAME-auth-out.$$ 2>&1
    bash "$PROJECT_ROOT/tools/runtime_execution.sh" "$REQUEST_COPY" "$DECISION_COPY" "$AUTH_COPY" "$EXEC_COPY" >/tmp/gate5-$NAME-exec-out.$$ 2>&1
    A=$(grep '^authorization_status=' "$AUTH_COPY" 2>/dev/null | awk -F= '{print $2}')
    E=$(grep '^execution_status=' "$EXEC_COPY" 2>/dev/null | awk -F= '{print $2}')
    R=$(grep '^result_status=' "$EXEC_COPY" 2>/dev/null | awk -F= '{print $2}')
    if [ "$A" = 'DENIED' ] && [ "$E" = 'BLOCKED' ] && [ "$R" = 'DENIED' ]; then
        printf '%s=PASS\n' "$NAME"
    else
        SELFTEST_FAIL=1
        printf '%s=FAIL authorization_status=%s execution_status=%s result_status=%s\n' "$NAME" "${A:-MISSING}" "${E:-MISSING}" "${R:-MISSING}"
    fi
}

for CASE in wrong_pipeline_run_id wrong_source_commit wrong_gate4_hash wrong_profile_hash wrong_request_id missing_decision invalid_decision stale_decision
do
    run_negative "$CASE" "$CASE"
done

if [ "$SELFTEST_FAIL" -eq 0 ]; then
    printf '%s\n' 'SELFTEST_STATUS=PASS'
else
    printf '%s\n' 'SELFTEST_STATUS=FAIL'
fi
printf '%s\n' "gate5_artifacts=$RUN_DIR"
