#!/usr/bin/env bash
# Gate 5 V1 Authorization. All three paths are explicit; no newest lookup.

set -u

main() {
    local REQUEST_PATH DECISION_PATH AUTH_PATH ROOT REQUEST_DIR RUN_ID REQUEST_ID
    local REQUEST_ERROR DECISION_ERROR DECISION_VALUE REASON STATUS NOW AUTH_TMP

    if [ "$#" -ne 3 ]; then
        printf '%s\n' 'usage: bash tools/runtime_execution_authorize.sh REQUEST_PATH DECISION_PATH AUTHORIZATION_PATH'
        printf '%s\n' 'authorization_status=DENIED'
        return 2
    fi
    REQUEST_PATH=$1
    DECISION_PATH=$2
    AUTH_PATH=$3
    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    . "$ROOT/tools/gate5_common.sh"

    REQUEST_DIR=$(dirname -- "$REQUEST_PATH")
    RUN_ID=$(gate5_field "$REQUEST_PATH" pipeline_run_id 2>/dev/null || printf '%s' '')
    REQUEST_ID=$(gate5_field "$REQUEST_PATH" request_id 2>/dev/null || printf '%s' '')
    REQUEST_ERROR=0
    DECISION_ERROR=0
    REASON=''

    [ -f "$REQUEST_PATH" ] || REQUEST_ERROR=1
    [ -f "$DECISION_PATH" ] || DECISION_ERROR=1
    if [ "$REQUEST_ERROR" -ne 0 ]; then REASON='request artifact missing'; fi
    if [ "$DECISION_ERROR" -ne 0 ]; then REASON="${REASON:+$REASON; }decision artifact missing"; fi

    if [ -z "$REASON" ]; then
        if ! gate5_gate4_load "$ROOT" "$RUN_ID" >/dev/null 2>&1; then REASON='Gate 4 evidence invalid'; fi
        if ! gate5_compare_identity "$REQUEST_PATH" "$DECISION_PATH"; then REASON="${REASON:+$REASON; }request/decision identity mismatch"; fi
        DECISION_VALUE=$(gate5_field "$DECISION_PATH" decision 2>/dev/null || printf '%s' '')
        if [ "$DECISION_VALUE" != 'ALLOW' ]; then REASON="${REASON:+$REASON; }decision is not ALLOW"; fi
        if ! gate5_fresh "$(gate5_field "$REQUEST_PATH" created_at 2>/dev/null || printf '%s' '')"; then REASON="${REASON:+$REASON; }request is stale or invalid"; fi
        if ! gate5_fresh "$(gate5_field "$DECISION_PATH" created_at 2>/dev/null || printf '%s' '')"; then REASON="${REASON:+$REASON; }decision is stale or invalid"; fi
    fi

    STATUS=AUTHORIZED
    if [ -n "$REASON" ]; then STATUS=DENIED; fi
    NOW=$(gate5_now)
    mkdir -p "$(dirname -- "$AUTH_PATH")"
    AUTH_TMP="$AUTH_PATH.partial.$$"
    {
        printf '%s\n' 'schema_version=gate5-authorization.v1'
        printf '%s\n' "authorization_id=authorization-$REQUEST_ID-$(printf '%s' "$REQUEST_ID$RUN_ID" | sha256sum | awk '{print substr($1,1,16)}')"
        printf '%s\n' "request_id=$(gate5_field "$REQUEST_PATH" request_id 2>/dev/null || printf '%s' '')"
        printf '%s\n' "pipeline_run_id=$(gate5_field "$REQUEST_PATH" pipeline_run_id 2>/dev/null || printf '%s' '')"
        printf '%s\n' "source_commit=$(gate5_field "$REQUEST_PATH" source_commit 2>/dev/null || printf '%s' '')"
        printf '%s\n' "gate4_contract_sha256=$(gate5_field "$REQUEST_PATH" gate4_contract_sha256 2>/dev/null || printf '%s' '')"
        printf '%s\n' "profile_sha256=$(gate5_field "$REQUEST_PATH" profile_sha256 2>/dev/null || printf '%s' '')"
        printf '%s\n' "policy_version=$(gate5_field "$REQUEST_PATH" policy_version 2>/dev/null || printf '%s' '')"
        printf '%s\n' "decision_id=$(gate5_field "$DECISION_PATH" decision_id 2>/dev/null || printf '%s' '')"
        printf '%s\n' "authorization_status=$STATUS"
        printf '%s\n' "authorization_reason=${REASON:-all identity and freshness checks passed}"
        printf '%s\n' "created_at=$NOW"
    } > "$AUTH_TMP"
    mv "$AUTH_TMP" "$AUTH_PATH"
    printf '%s\n' "authorization_status=$STATUS"
    printf '%s\n' "authorization_reason=${REASON:-all identity and freshness checks passed}"
    printf '%s\n' "authorization_artifact=$AUTH_PATH"
    return 0
}

main "$@"
