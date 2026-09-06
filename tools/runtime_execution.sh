#!/usr/bin/env bash
# Gate 5 V1 Execution Enforcement. Explicit artifacts are mandatory.
# V1 permits only the fixed self-test command: pwd.

set -u

main() {
    local REQUEST_PATH DECISION_PATH AUTH_PATH EXEC_PATH ROOT REASON STATUS NOW EXEC_TMP
    local ACTION RESOURCE DECISION_VALUE AUTH_VALUE OUTPUT RETURN_CODE

    if [ "$#" -ne 4 ]; then
        printf '%s\n' 'usage: bash tools/runtime_execution.sh REQUEST_PATH DECISION_PATH AUTHORIZATION_PATH EXECUTION_PATH'
        printf '%s\n' 'execution_status=BLOCKED'
        printf '%s\n' 'result_status=DENIED'
        return 2
    fi
    REQUEST_PATH=$1
    DECISION_PATH=$2
    AUTH_PATH=$3
    EXEC_PATH=$4
    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    . "$ROOT/tools/gate5_common.sh"
    REASON=''
    if ! CDPATH= cd -- "$ROOT" 2>/dev/null; then REASON='repository root unavailable'; fi

    [ -f "$REQUEST_PATH" ] || REASON='request artifact missing'
    [ -f "$DECISION_PATH" ] || REASON="${REASON:+$REASON; }decision artifact missing"
    [ -f "$AUTH_PATH" ] || REASON="${REASON:+$REASON; }authorization artifact missing"
    if [ -z "$REASON" ]; then
        if ! gate5_compare_identity "$REQUEST_PATH" "$DECISION_PATH"; then REASON='request/decision identity mismatch'; fi
        if ! gate5_compare_identity "$REQUEST_PATH" "$AUTH_PATH"; then REASON="${REASON:+$REASON; }request/authorization identity mismatch"; fi
        DECISION_VALUE=$(gate5_field "$DECISION_PATH" decision 2>/dev/null || printf '%s' '')
        AUTH_VALUE=$(gate5_field "$AUTH_PATH" authorization_status 2>/dev/null || printf '%s' '')
        [ "$DECISION_VALUE" = 'ALLOW' ] || REASON="${REASON:+$REASON; }decision is not ALLOW"
        [ "$AUTH_VALUE" = 'AUTHORIZED' ] || REASON="${REASON:+$REASON; }authorization is not AUTHORIZED"
        if ! gate5_gate4_load "$ROOT" "$(gate5_field "$REQUEST_PATH" pipeline_run_id 2>/dev/null || printf '%s' '')" >/dev/null 2>&1; then REASON="${REASON:+$REASON; }Gate 4 evidence invalid"; fi
        if ! gate5_fresh "$(gate5_field "$AUTH_PATH" created_at 2>/dev/null || printf '%s' '')"; then REASON="${REASON:+$REASON; }authorization is stale or invalid"; fi
        ACTION=$(gate5_field "$REQUEST_PATH" action 2>/dev/null || printf '%s' '')
        RESOURCE=$(gate5_field "$REQUEST_PATH" resource 2>/dev/null || printf '%s' '')
        [ "$ACTION" = 'pwd' ] || REASON="${REASON:+$REASON; }action is not pwd"
        [ "$RESOURCE" = 'runtime' ] || REASON="${REASON:+$REASON; }resource is not runtime"
    fi

    STATUS=EXECUTED
    OUTPUT=''
    RETURN_CODE=''
    if [ -n "$REASON" ]; then
        STATUS=BLOCKED
    else
        OUTPUT=$(CDPATH= cd -- "$ROOT" 2>/dev/null && pwd 2>/dev/null)
        RETURN_CODE=$?
        if [ "$RETURN_CODE" -ne 0 ] || [ -z "$OUTPUT" ]; then
            STATUS=BLOCKED
            REASON='pwd execution failed'
        fi
    fi
    NOW=$(gate5_now)
    mkdir -p "$(dirname -- "$EXEC_PATH")"
    EXEC_TMP="$EXEC_PATH.partial.$$"
    {
        printf '%s\n' 'schema_version=gate5-execution.v1'
        printf '%s\n' "execution_id=execution-$(gate5_field "$REQUEST_PATH" request_id 2>/dev/null || printf '%s' '')-$(printf '%s' "$NOW" | sha256sum | awk '{print substr($1,1,16)}')"
        printf '%s\n' "request_id=$(gate5_field "$REQUEST_PATH" request_id 2>/dev/null || printf '%s' '')"
        printf '%s\n' "pipeline_run_id=$(gate5_field "$REQUEST_PATH" pipeline_run_id 2>/dev/null || printf '%s' '')"
        printf '%s\n' "source_commit=$(gate5_field "$REQUEST_PATH" source_commit 2>/dev/null || printf '%s' '')"
        printf '%s\n' "gate4_contract_sha256=$(gate5_field "$REQUEST_PATH" gate4_contract_sha256 2>/dev/null || printf '%s' '')"
        printf '%s\n' "profile_sha256=$(gate5_field "$REQUEST_PATH" profile_sha256 2>/dev/null || printf '%s' '')"
        printf '%s\n' "policy_version=$(gate5_field "$REQUEST_PATH" policy_version 2>/dev/null || printf '%s' '')"
        printf '%s\n' "execution_status=$STATUS"
        if [ "$STATUS" = 'EXECUTED' ]; then
            printf '%s\n' 'result_status=PASS'
            printf '%s\n' 'command=pwd'
            printf '%s\n' "command_result=$OUTPUT"
            printf '%s\n' "command_returncode=$RETURN_CODE"
            printf '%s\n' 'execution_reason=authorization verified; pwd executed'
        else
            printf '%s\n' 'result_status=DENIED'
            printf '%s\n' 'command=pwd'
            printf '%s\n' 'command_result=NOT_EXECUTED'
            printf '%s\n' "execution_reason=${REASON:-execution blocked}"
        fi
        printf '%s\n' "created_at=$NOW"
    } > "$EXEC_TMP"
    mv "$EXEC_TMP" "$EXEC_PATH"
    printf '%s\n' "execution_status=$STATUS"
    if [ "$STATUS" = 'EXECUTED' ]; then
        printf '%s\n' 'result_status=PASS'
    else
        printf '%s\n' 'result_status=DENIED'
    fi
    printf '%s\n' "execution_artifact=$EXEC_PATH"
    return 0
}

main "$@"
