#!/usr/bin/env bash
# Authoritative Gate 19 consumer. Consumes validated G18 evidence only; never executes commands.
set -u

main() {
    local ROOT RUN_ID INPUT OUTPUT CONTRACT SOURCE_COMMIT PROFILE_SHA CONTRACT_SHA STATUS REASON NOW TMP
    local SCHEMA GATE GATE_STATUS PIPELINE SOURCE G4SHA PROFILE INPUT_ART CREATED
    local EXEC_ID REQUEST COMMAND SEMANTICS CMD_SHA AUTH EXEC RESULT RETURN RESULT_VALUE RESULT_SHA G16_SHA G16_ART PATH
    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    RUN_ID=${1:-}
    INPUT=${2:-}
    OUTPUT=${3:-}

    if [ -z "$RUN_ID" ] || [ -z "$INPUT" ] || [ -z "$OUTPUT" ]; then
        printf '%s\n' 'GATE19_STATUS=BLOCKED'
        printf '%s\n' 'GATE19_REASON=explicit run_id, G18 input path, and output path are required'
        return 1
    fi

    . "$ROOT/tools/runtime_chain_common.sh"
    CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
    SOURCE_COMMIT=$(chain_field "$CONTRACT" source_commit 2>/dev/null || printf '%s' '')
    PROFILE_SHA=$(chain_field "$CONTRACT" profile_sha256 2>/dev/null || printf '%s' '')
    CONTRACT_SHA=$(chain_hash "$CONTRACT" 2>/dev/null || printf '%s' '')
    NOW=$(chain_now)
    STATUS=PASS
    REASON='G18 runtime result accepted as terminal evidence'

    SCHEMA=$(chain_field "$INPUT" schema_version 2>/dev/null || printf '%s' '')
    GATE=$(chain_field "$INPUT" gate 2>/dev/null || printf '%s' '')
    GATE_STATUS=$(chain_field "$INPUT" gate_status 2>/dev/null || printf '%s' '')
    PIPELINE=$(chain_field "$INPUT" pipeline_run_id 2>/dev/null || printf '%s' '')
    SOURCE=$(chain_field "$INPUT" source_commit 2>/dev/null || printf '%s' '')
    G4SHA=$(chain_field "$INPUT" gate4_contract_sha256 2>/dev/null || printf '%s' '')
    PROFILE=$(chain_field "$INPUT" profile_sha256 2>/dev/null || printf '%s' '')
    INPUT_ART=$(chain_field "$INPUT" input_artifact 2>/dev/null || printf '%s' '')
    CREATED=$(chain_field "$INPUT" created_at 2>/dev/null || printf '%s' '')
    EXEC_ID=$(chain_field "$INPUT" execution_id 2>/dev/null || printf '%s' '')
    REQUEST=$(chain_field "$INPUT" request_id 2>/dev/null || printf '%s' '')
    COMMAND=$(chain_field "$INPUT" command 2>/dev/null || printf '%s' '')
    SEMANTICS=$(chain_field "$INPUT" command_semantics 2>/dev/null || printf '%s' '')
    CMD_SHA=$(chain_field "$INPUT" command_sha256 2>/dev/null || printf '%s' '')
    AUTH=$(chain_field "$INPUT" authorization_status 2>/dev/null || printf '%s' '')
    EXEC=$(chain_field "$INPUT" execution_status 2>/dev/null || printf '%s' '')
    RESULT=$(chain_field "$INPUT" result_status 2>/dev/null || printf '%s' '')
    RETURN=$(chain_field "$INPUT" command_returncode 2>/dev/null || printf '%s' '')
    RESULT_VALUE=$(chain_field "$INPUT" command_result 2>/dev/null || printf '%s' '')
    RESULT_SHA=$(chain_field "$INPUT" command_result_sha256 2>/dev/null || printf '%s' '')
    G16_SHA=$(chain_field "$INPUT" gate16_authorization_sha256 2>/dev/null || printf '%s' '')
    G16_ART=$(chain_field "$INPUT" gate16_authorization_artifact 2>/dev/null || printf '%s' '')
    PATH=$(chain_field "$INPUT" execution_path 2>/dev/null || printf '%s' '')

    if [ ! -f "$INPUT" ]; then
        STATUS=BLOCKED; REASON='G18 result artifact missing'
    elif [ -z "$CONTRACT" ] || [ ! -f "$CONTRACT" ]; then
        STATUS=BLOCKED; REASON='Gate 4 contract missing'
    elif [ "$SCHEMA" != 'gate18-artifact.v1' ] || [ "$GATE" != 'gate18' ]; then
        STATUS=BLOCKED; REASON='G18 schema or gate identity invalid'
    elif [ "$GATE_STATUS" != 'PASS' ]; then
        STATUS=BLOCKED; REASON='G18 gate_status is not PASS'
    elif [ "$PIPELINE" != "$RUN_ID" ]; then
        STATUS=BLOCKED; REASON='G18 pipeline_run_id mismatch'
    elif [ "$SOURCE" != "$SOURCE_COMMIT" ] || [ "$G4SHA" != "$CONTRACT_SHA" ] || [ "$PROFILE" != "$PROFILE_SHA" ]; then
        STATUS=BLOCKED; REASON='G18 current Gate 4 provenance mismatch'
    elif [ "$EXEC_ID" != "execution-pwd-$RUN_ID" ] || [ "$REQUEST" != "pwd-request-$RUN_ID" ]; then
        STATUS=BLOCKED; REASON='G18 execution/request identity mismatch'
    elif [ "$COMMAND" != 'pwd' ] || [ "$SEMANTICS" != 'POSIX_PWD' ]; then
        STATUS=BLOCKED; REASON='G18 command identity invalid'
    elif [ "$AUTH" != 'AUTHORIZED' ] || [ "$EXEC" != 'PASS' ] || [ "$RESULT" != 'PASS' ] || [ "$RETURN" != '0' ]; then
        STATUS=BLOCKED; REASON='G18 execution/result authorization state invalid'
    elif [ -z "$RESULT_VALUE" ] || [ -z "$PATH" ] || [ -z "$CREATED" ]; then
        STATUS=BLOCKED; REASON='G18 result evidence incomplete'
    elif ! [[ "$CMD_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || [ "$CMD_SHA" != "$(printf '%s\n' pwd | sha256sum | awk '{print $1}')" ]; then
        STATUS=BLOCKED; REASON='G18 command hash invalid'
    elif ! [[ "$RESULT_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || [ "$RESULT_SHA" != "$(printf '%s\n' "$RESULT_VALUE" | sha256sum | awk '{print $1}')" ]; then
        STATUS=BLOCKED; REASON='G18 command result hash invalid'
    elif ! [[ "$G16_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || [ ! -f "$G16_ART" ] || [ "$(sha256sum "$G16_ART" | awk '{print $1}')" != "$G16_SHA" ]; then
        STATUS=BLOCKED; REASON='G18 Gate 16 authorization linkage invalid'
    elif [ -n "$INPUT_ART" ] && [ ! -f "$INPUT_ART" ]; then
        STATUS=BLOCKED; REASON='G18 source execution artifact missing'
    fi

    mkdir -p "$(dirname -- "$OUTPUT")"
    TMP="$OUTPUT.partial.$$"
    {
        printf '%s\n' 'schema_version=gate19-artifact.v1'
        printf '%s\n' 'gate=gate19'
        printf '%s\n' "gate_status=$STATUS"
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "gate4_contract_sha256=$CONTRACT_SHA"
        printf '%s\n' "profile_sha256=$PROFILE_SHA"
        printf '%s\n' "input_artifact=$INPUT"
        printf '%s\n' "input_artifact_sha256=$(sha256sum "$INPUT" 2>/dev/null | awk '{print $1}')"
        printf '%s\n' "created_at=$NOW"
        printf '%s\n' "execution_id=$EXEC_ID"
        printf '%s\n' "request_id=$REQUEST"
        printf '%s\n' "command=$COMMAND"
        printf '%s\n' "command_semantics=$SEMANTICS"
        printf '%s\n' "command_sha256=$CMD_SHA"
        printf '%s\n' "authorization_status=$AUTH"
        printf '%s\n' "execution_status=$EXEC"
        printf '%s\n' "result_status=$RESULT"
        printf '%s\n' "command_result=$RESULT_VALUE"
        printf '%s\n' "command_returncode=$RETURN"
        printf '%s\n' "command_result_sha256=$RESULT_SHA"
        printf '%s\n' "gate16_authorization_sha256=$G16_SHA"
        printf '%s\n' "gate16_authorization_artifact=$G16_ART"
        printf '%s\n' "execution_path=$PATH"
        printf '%s\n' "consume_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'ACCEPTED' || printf '%s' 'REJECTED')"
        printf '%s\n' "next_phase=APK_BUILD_AND_UI"
        printf '%s\n' "next_phase_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'UNLOCKED' || printf '%s' 'BLOCKED')"
        printf '%s\n' "apk_build_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'OPEN' || printf '%s' 'BLOCKED')"
        printf '%s\n' "stage_reason=$REASON"
    } > "$TMP"
    mv "$TMP" "$OUTPUT"
    printf '%s\n' "GATE19_STATUS=$STATUS"
    printf '%s\n' "consume_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'ACCEPTED' || printf '%s' 'REJECTED')"
    printf '%s\n' "next_phase_status=$([ "$STATUS" = 'PASS' ] && printf '%s' 'UNLOCKED' || printf '%s' 'BLOCKED')"
    printf '%s\n' "artifact=$OUTPUT"
    [ "$STATUS" = 'PASS' ]
}

main "$@"
