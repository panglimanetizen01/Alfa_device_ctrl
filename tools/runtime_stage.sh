#!/usr/bin/env bash
# Deterministic Gate 6-19 stage engine. All inputs and outputs are explicit.
# G14 is intentionally not implemented here; use gate14_runtime_command_validation.sh.
# G15 is intentionally not implemented here; use gate15_runtime_command_policy.sh.
# G16 is intentionally not implemented here; use gate16_runtime_execution_authorization.sh.
# G17 is intentionally not implemented here; use gate17_runtime_execution.sh.
set -u
main() {
    local STAGE RUN_ID INPUT1 INPUT2 OUTPUT ROOT CONTRACT SOURCE_COMMIT PROFILE_SHA CONTRACT_SHA
    local INPUT_STATUS INPUT2_STATUS STATUS REASON NOW COMMAND EXECUTION_PATH
    local OUTPUT_TMP G17_SCHEMA G17_GATE G17_EXEC_ID G17_REQUEST G17_COMMAND G17_SEMANTICS
    local G17_CMD_SHA G17_AUTH G17_EXEC G17_RESULT G17_RETURN G17_RESULT_SHA G17_RESULT_VALUE
    local G17_G16_SHA G17_G16_ART G17_CREATED G17_PATH EXPECTED_COMMAND_SHA
    STAGE=${1:-}; RUN_ID=${2:-}; INPUT1=${3:-}; INPUT2=${4:-}; OUTPUT=${5:-}
    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    . "$ROOT/tools/runtime_chain_common.sh"
    if [ -z "$STAGE" ] || [ -z "$RUN_ID" ] || [ -z "$OUTPUT" ]; then printf '%s\n' 'STAGE_STATUS=BLOCKED'; printf '%s\n' 'STAGE_REASON=invalid stage arguments'; return 0; fi
    CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
    SOURCE_COMMIT=$(chain_field "$CONTRACT" source_commit 2>/dev/null || printf '%s' 'UNKNOWN')
    PROFILE_SHA=$(chain_field "$CONTRACT" profile_sha256 2>/dev/null || printf '%s' 'UNKNOWN')
    CONTRACT_SHA=$(chain_hash "$CONTRACT" 2>/dev/null || printf '%s' 'UNKNOWN')
    NOW=$(chain_now); STATUS=PASS; REASON='upstream artifact and current Gate 4 identity verified'
    INPUT_STATUS=$(chain_field "$INPUT1" gate_status 2>/dev/null || printf '%s' '')
    INPUT2_STATUS=$(chain_field "$INPUT2" gate_status 2>/dev/null || printf '%s' '')
    EXECUTION_PATH=$(chain_field "$INPUT1" execution_path 2>/dev/null || printf '%s' 'UNKNOWN')
    if [ -z "$CONTRACT" ] || [ ! -f "$CONTRACT" ]; then STATUS=BLOCKED; REASON='Gate 4 contract missing, malformed, or not VALID'
    elif [ ! -f "$INPUT1" ]; then STATUS=BLOCKED; REASON='required upstream artifact missing'
    elif ! chain_identity_ok "$CONTRACT" "$INPUT1"; then STATUS=BLOCKED; REASON='upstream artifact identity mismatch'
    elif [ "$STAGE" = 'gate6' ] && [ "$(chain_field "$INPUT1" decision 2>/dev/null || printf '%s' '')" != 'ALLOW' ]; then STATUS=BLOCKED; REASON='Gate 5 decision is not ALLOW'
    elif [ "$STAGE" != 'gate6' ] && [ "$INPUT_STATUS" != 'PASS' ]; then STATUS=BLOCKED; REASON='upstream gate is not PASS'; fi
    case "$STAGE" in
        gate6|gate7|gate8|gate9|gate10|gate11|gate12) ;;
        gate13) COMMAND='pwd' ;;
        gate16) STATUS=BLOCKED; REASON='G16 is non-authoritative in runtime_stage.sh; use gate16_runtime_execution_authorization.sh' ;;
        gate17) STATUS=BLOCKED; REASON='G17 is non-authoritative in runtime_stage.sh; use gate17_runtime_execution.sh' ;;
        gate18) ;;
        gate19) STATUS=BLOCKED; REASON='G19 is non-authoritative in runtime_stage.sh; use runtime_result_consumer.sh' ;;
        *) STATUS=BLOCKED; REASON='unknown or non-authoritative runtime gate' ;;
    esac
    if [ "$STAGE" = 'gate18' ] && [ "$STATUS" = 'PASS' ]; then
        G17_SCHEMA=$(chain_field "$INPUT1" schema_version 2>/dev/null || printf '%s' ''); G17_GATE=$(chain_field "$INPUT1" gate 2>/dev/null || printf '%s' '')
        G17_EXEC_ID=$(chain_field "$INPUT1" execution_id 2>/dev/null || printf '%s' ''); G17_REQUEST=$(chain_field "$INPUT1" request_id 2>/dev/null || printf '%s' '')
        G17_COMMAND=$(chain_field "$INPUT1" command 2>/dev/null || printf '%s' ''); G17_SEMANTICS=$(chain_field "$INPUT1" command_semantics 2>/dev/null || printf '%s' '')
        G17_CMD_SHA=$(chain_field "$INPUT1" command_sha256 2>/dev/null || printf '%s' ''); G17_AUTH=$(chain_field "$INPUT1" authorization_status 2>/dev/null || printf '%s' '')
        G17_EXEC=$(chain_field "$INPUT1" execution_status 2>/dev/null || printf '%s' ''); G17_RESULT=$(chain_field "$INPUT1" result_status 2>/dev/null || printf '%s' '')
        G17_RETURN=$(chain_field "$INPUT1" command_returncode 2>/dev/null || printf '%s' ''); G17_RESULT_VALUE=$(chain_field "$INPUT1" command_result 2>/dev/null || printf '%s' '')
        G17_RESULT_SHA=$(chain_field "$INPUT1" command_result_sha256 2>/dev/null || printf '%s' ''); G17_G16_SHA=$(chain_field "$INPUT1" gate16_authorization_sha256 2>/dev/null || printf '%s' '')
        G17_G16_ART=$(chain_field "$INPUT1" gate16_authorization_artifact 2>/dev/null || printf '%s' ''); G17_CREATED=$(chain_field "$INPUT1" created_at 2>/dev/null || printf '%s' '')
        G17_PATH=$(chain_field "$INPUT1" execution_path 2>/dev/null || printf '%s' ''); EXPECTED_COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
        if [ "$G17_SCHEMA" != 'gate17-runtime-execution.v1' ]; then STATUS=BLOCKED; REASON='G17 schema invalid'
        elif [ "$G17_GATE" != 'gate17' ]; then STATUS=BLOCKED; REASON='G17 gate identity invalid'
        elif [ "$G17_EXEC_ID" != "execution-pwd-$RUN_ID" ]; then STATUS=BLOCKED; REASON='G17 execution identity mismatch'
        elif [ "$G17_REQUEST" != "pwd-request-$RUN_ID" ]; then STATUS=BLOCKED; REASON='G17 request identity mismatch'
        elif [ "$G17_COMMAND" != 'pwd' ] || [ "$G17_SEMANTICS" != 'POSIX_PWD' ]; then STATUS=BLOCKED; REASON='G17 command identity invalid'
        elif [ "$G17_CMD_SHA" != "$EXPECTED_COMMAND_SHA" ]; then STATUS=BLOCKED; REASON='G17 command hash mismatch'
        elif [ "$G17_AUTH" != 'AUTHORIZED' ]; then STATUS=BLOCKED; REASON='G17 authorization is not AUTHORIZED'
        elif [ "$G17_EXEC" != 'PASS' ] || [ "$G17_RESULT" != 'PASS' ]; then STATUS=BLOCKED; REASON='G17 execution/result status is not PASS'
        elif [ "$G17_RETURN" != '0' ]; then STATUS=BLOCKED; REASON='G17 command return code is not zero'
        elif [ -z "$G17_RESULT_VALUE" ]; then STATUS=BLOCKED; REASON='G17 command result is missing'
        elif ! [[ "$G17_RESULT_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || [ "$(printf '%s\n' "$G17_RESULT_VALUE" | sha256sum | awk '{print $1}')" != "$G17_RESULT_SHA" ]; then STATUS=BLOCKED; REASON='G17 command result hash mismatch'
        elif ! [[ "$G17_G16_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || [ ! -f "$G17_G16_ART" ]; then STATUS=BLOCKED; REASON='G17 Gate 16 authorization evidence missing'
        elif [ "$(sha256sum "$G17_G16_ART" | awk '{print $1}')" != "$G17_G16_SHA" ]; then STATUS=BLOCKED; REASON='G17 Gate 16 authorization hash mismatch'
        elif [ -z "$G17_CREATED" ] || [ -z "$G17_PATH" ]; then STATUS=BLOCKED; REASON='G17 execution timestamp or path missing'
        else STATUS=PASS; REASON='G17 execution evidence validated against current Gate 4 identity'; EXECUTION_PATH="$G17_PATH"; fi
    fi
    mkdir -p "$(dirname -- "$OUTPUT")"; OUTPUT_TMP="$OUTPUT.partial.$$"
    {
        printf '%s\n' "schema_version=$STAGE-artifact.v1" "gate=$STAGE" "gate_status=$STATUS" "pipeline_run_id=$RUN_ID" "source_commit=$SOURCE_COMMIT" "gate4_contract_sha256=$CONTRACT_SHA" "profile_sha256=$PROFILE_SHA" "input_artifact=$INPUT1"
        if [ -n "$INPUT2" ]; then printf '%s\n' "input_artifact_2=$INPUT2"; fi
        if [ "$STAGE" != 'gate18' ]; then printf '%s\n' "created_at=$NOW"; fi
        printf '%s\n' "execution_path=$EXECUTION_PATH"
        case "$STAGE" in
            gate6) printf '%s\n' "decision=$(chain_field "$INPUT1" decision 2>/dev/null || printf '%s' '')" "bootstrap_status=$STATUS" ;;
            gate7) printf '%s\n' "bootstrap_status=$(chain_field "$INPUT1" bootstrap_status 2>/dev/null || printf '%s' '')" "session_status=$STATUS" ;;
            gate8) printf '%s\n' "session_status=$(chain_field "$INPUT1" session_status 2>/dev/null || printf '%s' '')" "task_status=$STATUS" 'task_name=runtime_self_test' ;;
            gate9) printf '%s\n' "task_status=$(chain_field "$INPUT1" task_status 2>/dev/null || printf '%s' '')" "action_status=$STATUS" 'action_name=runtime_self_action' ;;
            gate10) printf '%s\n' "action_status=$(chain_field "$INPUT1" action_status 2>/dev/null || printf '%s' '')" "workflow_status=$STATUS" 'workflow_name=runtime_self_workflow' ;;
            gate11) printf '%s\n' "workflow_status=$(chain_field "$INPUT1" workflow_status 2>/dev/null || printf '%s' '')" "orchestrator_status=$STATUS" 'orchestrator_name=runtime_self_orchestrator' ;;
            gate12) printf '%s\n' "orchestrator_status=$(chain_field "$INPUT1" orchestrator_status 2>/dev/null || printf '%s' '')" "kernel_status=$STATUS" 'kernel_name=runtime_self_kernel' ;;
            gate13) printf '%s\n' "kernel_status=$(chain_field "$INPUT1" kernel_status 2>/dev/null || printf '%s' '')" "command=$COMMAND" "command_status=$STATUS" ;;
            gate16) printf '%s\n' 'authorization_status=DENIED' 'execution_status=DEFERRED' 'execution_authority=G17' ;;
            gate17) printf '%s\n' 'execution_status=BLOCKED' 'execution_authority=G17' ;;
            gate18) printf '%s\n' "execution_id=$G17_EXEC_ID" "request_id=$G17_REQUEST" "command=$(chain_field "$INPUT1" command 2>/dev/null || printf '%s' '')" "command_semantics=$(chain_field "$INPUT1" command_semantics 2>/dev/null || printf '%s' '')" "command_sha256=$(chain_field "$INPUT1" command_sha256 2>/dev/null || printf '%s' '')" "authorization_status=$(chain_field "$INPUT1" authorization_status 2>/dev/null || printf '%s' '')" "execution_status=$(chain_field "$INPUT1" execution_status 2>/dev/null || printf '%s' '')" "command_result=$(chain_field "$INPUT1" command_result 2>/dev/null || printf '%s' '')" "command_returncode=$(chain_field "$INPUT1" command_returncode 2>/dev/null || printf '%s' '')" "command_result_sha256=$(chain_field "$INPUT1" command_result_sha256 2>/dev/null || printf '%s' '')" "gate16_authorization_sha256=$(chain_field "$INPUT1" gate16_authorization_sha256 2>/dev/null || printf '%s' '')" "gate16_authorization_artifact=$(chain_field "$INPUT1" gate16_authorization_artifact 2>/dev/null || printf '%s' '')" "created_at=$G17_CREATED" "result_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'PASS' || printf '%s' "$STATUS")" ;;
            gate19) ;;
        esac
        printf '%s\n' "stage_reason=$REASON"
    } > "$OUTPUT_TMP"
    mv "$OUTPUT_TMP" "$OUTPUT"
    printf '%s\n' "gate=$STAGE" "gate_status=$STATUS" "artifact=$OUTPUT"
    return 0
}
main "$@"
