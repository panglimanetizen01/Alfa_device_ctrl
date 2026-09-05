#!/usr/bin/env bash
# Gate 5 V1 Decision Engine. It consumes one explicit Gate 4 run only.

set -u

main() {
    local ROOT RUN_ID REQUEST_ID SUBJECT ACTION RESOURCE CONTEXT POLICY NOW
    local CONTRACT SOURCE_COMMIT PROFILE_SHA CONTRACT_SHA DECISION REASON
    local GATE5_DIR REQUEST_PATH DECISION_PATH REQUEST_TMP DECISION_TMP
    local REQUEST_STATUS GATE5_STATUS GATE5_RESULT

    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    RUN_ID=${1:-run_20260823_171233_30513}
    REQUEST_ID=${2:-gate5-self-test}
    SUBJECT=${3:-local-runtime}
    ACTION=${4:-pwd}
    RESOURCE=${5:-runtime}
    CONTEXT=${6:-purpose=self-test}
    POLICY='gate5-policy-v1'
    NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' 'UNKNOWN')
    . "$ROOT/tools/gate5_common.sh"

    printf '%s\n' '=== ALFA GATE 5 V1 DECISION ==='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "request_id=$REQUEST_ID"

    if ! gate5_valid_token "$RUN_ID" || ! gate5_valid_token "$REQUEST_ID"; then
        printf '%s\n' 'REQUEST_STATUS=DENIED'
        printf '%s\n' 'GATE5_STATUS=DENIED'
        printf '%s\n' 'GATE5_RESULT=DECISION_DENY'
        printf '%s\n' 'decision=DENY'
        printf '%s\n' 'decision_reason=invalid request identity token'
        return 0
    fi

    GATE5_DIR="$ROOT/artifacts/pipeline/$RUN_ID/gate5"
    REQUEST_PATH="$GATE5_DIR/requests/$REQUEST_ID.txt"
    DECISION_PATH="$GATE5_DIR/decisions/$REQUEST_ID.txt"
    mkdir -p "$GATE5_DIR/requests" "$GATE5_DIR/decisions"

    CONTRACT=$(gate5_gate4_load "$ROOT" "$RUN_ID" 2>/dev/null)
    SOURCE_COMMIT=$(gate5_field "$CONTRACT" source_commit 2>/dev/null || printf '%s' '')
    PROFILE_SHA=$(gate5_field "$CONTRACT" profile_sha256 2>/dev/null || printf '%s' '')
    CONTRACT_SHA=$(gate5_hash "$CONTRACT")

    REQUEST_TMP="$REQUEST_PATH.partial.$$"
    {
        printf '%s\n' 'schema_version=gate5-request.v1'
        printf '%s\n' "request_id=$REQUEST_ID"
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "gate4_contract_sha256=$CONTRACT_SHA"
        printf '%s\n' "profile_sha256=$PROFILE_SHA"
        printf '%s\n' "subject=$SUBJECT"
        printf '%s\n' "action=$ACTION"
        printf '%s\n' "resource=$RESOURCE"
        printf '%s\n' "context=$CONTEXT"
        printf '%s\n' "policy_version=$POLICY"
        printf '%s\n' "created_at=$NOW"
    } > "$REQUEST_TMP"
    mv "$REQUEST_TMP" "$REQUEST_PATH"
    REQUEST_STATUS=READY_FOR_DECISION

    DECISION=ALLOW
    REASON='Gate 4 VALID and Gate 5 self-test policy matched'
    if [ -z "$CONTRACT" ] || [ ! -f "$CONTRACT" ]; then
        DECISION=DENY
        REASON='Gate 4 contract missing, malformed, or not VALID'
    elif [ "$ACTION" != 'pwd' ] || [ "$RESOURCE" != 'runtime' ] || [ "$CONTEXT" != 'purpose=self-test' ]; then
        DECISION=DENY
        REASON='V1 permits only action=pwd, resource=runtime, context=purpose=self-test'
    elif [ -z "$SOURCE_COMMIT" ] || [ -z "$PROFILE_SHA" ] || [ -z "$CONTRACT_SHA" ]; then
        DECISION=DENY
        REASON='Gate 4 identity evidence incomplete'
    fi

    DECISION_TMP="$DECISION_PATH.partial.$$"
    {
        printf '%s\n' 'schema_version=gate5-decision.v1'
        printf '%s\n' "decision_id=decision-$REQUEST_ID-$(printf '%s' "$CONTRACT_SHA$REQUEST_ID" | sha256sum | awk '{print substr($1,1,16)}')"
        printf '%s\n' "request_id=$REQUEST_ID"
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "gate4_contract_sha256=$CONTRACT_SHA"
        printf '%s\n' "profile_sha256=$PROFILE_SHA"
        printf '%s\n' "policy_version=$POLICY"
        printf '%s\n' "decision=$DECISION"
        printf '%s\n' "decision_reason=$REASON"
        printf '%s\n' "created_at=$NOW"
    } > "$DECISION_TMP"
    mv "$DECISION_TMP" "$DECISION_PATH"

    GATE5_STATUS=VALID
    GATE5_RESULT=DECISION_ALLOW
    if [ "$DECISION" != 'ALLOW' ]; then
        GATE5_STATUS=DENIED
        GATE5_RESULT=DECISION_DENY
    fi
    printf '%s\n' "REQUEST_STATUS=$REQUEST_STATUS"
    printf '%s\n' "GATE5_STATUS=$GATE5_STATUS"
    printf '%s\n' "GATE5_RESULT=$GATE5_RESULT"
    printf '%s\n' "decision=$DECISION"
    printf '%s\n' "decision_artifact=$DECISION_PATH"
    printf '%s\n' "request_artifact=$REQUEST_PATH"
    printf '%s\n' "decision_reason=$REASON"
    return 0
}

main "$@"
