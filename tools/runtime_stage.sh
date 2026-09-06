#!/usr/bin/env bash
# Deterministic Gate 6-19 stage engine. All inputs and outputs are explicit.

set -u

main() {
    local STAGE RUN_ID INPUT1 INPUT2 OUTPUT ROOT CONTRACT SOURCE_COMMIT PROFILE_SHA CONTRACT_SHA
    local INPUT_STATUS INPUT2_STATUS STATUS REASON NOW COMMAND COMMAND_RESULT RETURN_CODE
    local INPUT_REL INPUT2_REL OUTPUT_TMP

    STAGE=${1:-}
    RUN_ID=${2:-}
    INPUT1=${3:-}
    INPUT2=${4:-}
    OUTPUT=${5:-}
    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    . "$ROOT/tools/runtime_chain_common.sh"

    if [ -z "$STAGE" ] || [ -z "$RUN_ID" ] || [ -z "$OUTPUT" ]; then
        printf '%s\n' 'STAGE_STATUS=BLOCKED'
        printf '%s\n' 'STAGE_REASON=invalid stage arguments'
        return 0
    fi

    CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
    SOURCE_COMMIT=$(chain_field "$CONTRACT" source_commit 2>/dev/null || printf '%s' 'UNKNOWN')
    PROFILE_SHA=$(chain_field "$CONTRACT" profile_sha256 2>/dev/null || printf '%s' 'UNKNOWN')
    CONTRACT_SHA=$(chain_hash "$CONTRACT" 2>/dev/null || printf '%s' 'UNKNOWN')
    NOW=$(chain_now)
    STATUS=PASS
    REASON='upstream artifact and current Gate 4 identity verified'
    INPUT_STATUS=$(chain_field "$INPUT1" gate_status 2>/dev/null || printf '%s' '')
    INPUT2_STATUS=$(chain_field "$INPUT2" gate_status 2>/dev/null || printf '%s' '')

    if [ -z "$CONTRACT" ] || [ ! -f "$CONTRACT" ]; then
        STATUS=BLOCKED
        REASON='Gate 4 contract missing, malformed, or not VALID'
    elif [ ! -f "$INPUT1" ]; then
        STATUS=BLOCKED
        REASON='required upstream artifact missing'
    elif ! chain_identity_ok "$CONTRACT" "$INPUT1"; then
        STATUS=BLOCKED
        REASON='upstream artifact identity mismatch'
    elif [ "$STAGE" = 'gate6' ] && [ "$(chain_field "$INPUT1" decision 2>/dev/null || printf '%s' '')" != 'ALLOW' ]; then
        STATUS=BLOCKED
        REASON='Gate 5 decision is not ALLOW'
    elif [ "$STAGE" != 'gate6' ] && [ "$INPUT_STATUS" != 'PASS' ]; then
        STATUS=BLOCKED
        REASON='upstream gate is not PASS'
    fi

    case "$STAGE" in
        gate6)
            ;;
        gate7|gate8|gate9|gate10|gate11|gate12)
            ;;
        gate13)
            COMMAND='pwd'
            ;;
        gate14)
            if [ "$STATUS" = 'PASS' ] && [ "$(chain_field "$INPUT1" command 2>/dev/null || printf '%s' '')" != 'pwd' ]; then STATUS=BLOCKED; REASON='command is not the V1 pwd command'; fi
            ;;
        gate15)
            if [ "$STATUS" = 'PASS' ] && [ "$(chain_field "$INPUT1" validation_status 2>/dev/null || printf '%s' '')" != 'ALLOWED' ]; then STATUS=BLOCKED; REASON='command validation is not ALLOWED'; fi
            ;;
        gate16)
            if [ "$STATUS" = 'PASS' ] && [ "$(chain_field "$INPUT1" policy_status 2>/dev/null || printf '%s' '')" != 'ALLOWED' ]; then STATUS=BLOCKED; REASON='command policy is not ALLOWED'; fi
            if [ "$STATUS" = 'PASS' ] && [ ! -f "$INPUT2" ]; then STATUS=BLOCKED; REASON='Gate 5 authorization artifact missing'; fi
            if [ "$STATUS" = 'PASS' ] && ! chain_identity_ok "$CONTRACT" "$INPUT2"; then STATUS=BLOCKED; REASON='Gate 5 authorization identity mismatch'; fi
            if [ "$STATUS" = 'PASS' ] && [ "$(chain_field "$INPUT2" authorization_status 2>/dev/null || printf '%s' '')" != 'AUTHORIZED' ]; then STATUS=BLOCKED; REASON='Gate 5 authorization is not AUTHORIZED'; fi
            ;;
        gate17)
            if [ "$STATUS" = 'PASS' ] && [ "$(chain_field "$INPUT1" authorization_status 2>/dev/null || printf '%s' '')" != 'AUTHORIZED' ]; then STATUS=BLOCKED; REASON='authorization is not AUTHORIZED'; fi
            if [ "$STATUS" = 'PASS' ] && [ ! -f "$INPUT2" ]; then STATUS=BLOCKED; REASON='command artifact missing'; fi
            if [ "$STATUS" = 'PASS' ] && [ "$(chain_field "$INPUT2" command 2>/dev/null || printf '%s' '')" != 'pwd' ]; then STATUS=BLOCKED; REASON='command is not pwd'; fi
            ;;
        gate18)
            ;;
        gate19)
            ;;
        *)
            STATUS=BLOCKED
            REASON='unknown runtime gate'
            ;;
    esac

    if [ "$STAGE" = 'gate17' ] && [ "$STATUS" = 'PASS' ]; then
        COMMAND='pwd'
        COMMAND_RESULT=$(CDPATH= cd -- "$ROOT" 2>/dev/null && pwd 2>/dev/null)
        RETURN_CODE=$?
        if [ "$RETURN_CODE" -ne 0 ] || [ -z "$COMMAND_RESULT" ]; then STATUS=ERROR; REASON='pwd execution failed'; fi
    fi

    if [ "$STAGE" = 'gate18' ]; then
        case "$(chain_field "$INPUT1" execution_status 2>/dev/null || printf '%s' '')" in
            PASS) STATUS=PASS ;;
            BLOCKED) STATUS=BLOCKED; REASON='execution was BLOCKED' ;;
            ERROR) STATUS=ERROR; REASON='execution returned ERROR' ;;
            *) STATUS=BLOCKED; REASON='execution status missing or invalid' ;;
        esac
    fi

    if [ "$STAGE" = 'gate19' ]; then
        if [ "$(chain_field "$INPUT1" result_status 2>/dev/null || printf '%s' '')" = 'PASS' ]; then STATUS=PASS; else STATUS=BLOCKED; REASON='runtime result is not PASS'; fi
    fi

    mkdir -p "$(dirname -- "$OUTPUT")"
    OUTPUT_TMP="$OUTPUT.partial.$$"
    {
        printf '%s\n' "schema_version=$STAGE-artifact.v1"
        printf '%s\n' "gate=$STAGE"
        printf '%s\n' "gate_status=$STATUS"
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "gate4_contract_sha256=$CONTRACT_SHA"
        printf '%s\n' "profile_sha256=$PROFILE_SHA"
        printf '%s\n' "input_artifact=$INPUT1"
        if [ -n "$INPUT2" ]; then printf '%s\n' "input_artifact_2=$INPUT2"; fi
        printf '%s\n' "created_at=$NOW"
        printf '%s\n' "execution_path=$ROOT"
        case "$STAGE" in
            gate6) printf '%s\n' "decision=$(chain_field "$INPUT1" decision 2>/dev/null || printf '%s' '')"; printf '%s\n' "bootstrap_status=$STATUS" ;;
            gate7) printf '%s\n' "bootstrap_status=$(chain_field "$INPUT1" bootstrap_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "session_status=$STATUS" ;;
            gate8) printf '%s\n' "session_status=$(chain_field "$INPUT1" session_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "task_status=$STATUS"; printf '%s\n' 'task_name=runtime_self_test' ;;
            gate9) printf '%s\n' "task_status=$(chain_field "$INPUT1" task_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "action_status=$STATUS"; printf '%s\n' 'action_name=runtime_self_action' ;;
            gate10) printf '%s\n' "action_status=$(chain_field "$INPUT1" action_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "workflow_status=$STATUS"; printf '%s\n' 'workflow_name=runtime_self_workflow' ;;
            gate11) printf '%s\n' "workflow_status=$(chain_field "$INPUT1" workflow_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "orchestrator_status=$STATUS"; printf '%s\n' 'orchestrator_name=runtime_self_orchestrator' ;;
            gate12) printf '%s\n' "orchestrator_status=$(chain_field "$INPUT1" orchestrator_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "kernel_status=$STATUS"; printf '%s\n' 'kernel_name=runtime_self_kernel' ;;
            gate13) printf '%s\n' "kernel_status=$(chain_field "$INPUT1" kernel_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "command=$COMMAND"; printf '%s\n' "command_status=$STATUS" ;;
            gate14) printf '%s\n' "command=$(chain_field "$INPUT1" command 2>/dev/null || printf '%s' '')"; printf '%s\n' "validation_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'ALLOWED' || printf '%s' 'BLOCKED')" ;;
            gate15) printf '%s\n' "validation_status=$(chain_field "$INPUT1" validation_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "policy_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'ALLOWED' || printf '%s' 'BLOCKED')" ;;
            gate16) printf '%s\n' "policy_status=$(chain_field "$INPUT1" policy_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "authorization_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'AUTHORIZED' || printf '%s' 'DENIED')"; printf '%s\n' "decision_id=$(chain_field "$INPUT2" decision_id 2>/dev/null || printf '%s' '')" ;;
            gate17) printf '%s\n' "authorization_status=$(chain_field "$INPUT1" authorization_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "command=pwd"; printf '%s\n' "execution_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'PASS' || printf '%s' "$STATUS")"; if [ "$STATUS" = 'PASS' ]; then printf '%s\n' "command_result=$COMMAND_RESULT"; printf '%s\n' "command_returncode=$RETURN_CODE"; else printf '%s\n' 'command_result=NOT_EXECUTED'; fi ;;
            gate18) printf '%s\n' "execution_status=$(chain_field "$INPUT1" execution_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "command=$(chain_field "$INPUT1" command 2>/dev/null || printf '%s' '')"; printf '%s\n' "command_result=$(chain_field "$INPUT1" command_result 2>/dev/null || printf '%s' '')"; printf '%s\n' "result_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'PASS' || printf '%s' "$STATUS")" ;;
            gate19) printf '%s\n' "result_status=$(chain_field "$INPUT1" result_status 2>/dev/null || printf '%s' '')"; printf '%s\n' "consume_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'ACCEPTED' || printf '%s' 'REJECTED')"; printf '%s\n' "command=$(chain_field "$INPUT1" command 2>/dev/null || printf '%s' '')"; printf '%s\n' "command_result=$(chain_field "$INPUT1" command_result 2>/dev/null || printf '%s' '')" ;;
        esac
        printf '%s\n' "stage_reason=$REASON"
    } > "$OUTPUT_TMP"
    mv "$OUTPUT_TMP" "$OUTPUT"
    printf '%s\n' "gate=$STAGE"
    printf '%s\n' "gate_status=$STATUS"
    printf '%s\n' "artifact=$OUTPUT"
    return 0
}

main "$@"
