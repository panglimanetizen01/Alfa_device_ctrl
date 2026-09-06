#!/usr/bin/env bash

set -u

main() {
    local SCRIPT_DIR PROJECT_ROOT DISCOVERY_SCRIPT INVOCATION_CWD
    local RUN_ID RUN_DIR STATE_FILE TRANSCRIPT SUMMARY
    local DISCOVERY_RC FINAL_STATUS CONTRACT_RESULT
    local SOURCE_COMMIT EDE_RC CDE_RC GATE3_RC
    local EDE_ARTIFACT CDE_ARTIFACT GATE3_ARTIFACT
    local FAILURE_STAGE FAILURE_REASON COMPLETION_STATUS
    local STAGE_COMPLETION ARTIFACT_HASHES

    INVOCATION_CWD=$(pwd 2>/dev/null || printf '%s' 'UNKNOWN')
    SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)

    if [ -z "$SCRIPT_DIR" ]; then
        printf '%s\n' 'PIPELINE_STATUS=BLOCKED'
        printf '%s\n' 'PIPELINE_ERROR=SCRIPT_LOCATION_UNRESOLVED'
        return 10
    fi

    PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." 2>/dev/null && pwd)
    if [ -z "$PROJECT_ROOT" ] || [ ! -d "$PROJECT_ROOT" ]; then
        printf '%s\n' 'PIPELINE_STATUS=BLOCKED'
        printf '%s\n' 'PIPELINE_ERROR=PROJECT_ROOT_UNRESOLVED'
        return 10
    fi

    DISCOVERY_SCRIPT="$PROJECT_ROOT/tools/runtime_discovery.sh"
    if [ ! -f "$DISCOVERY_SCRIPT" ]; then
        printf '%s\n' 'PIPELINE_STATUS=BLOCKED'
        printf '%s\n' 'PIPELINE_ERROR=RUNTIME_DISCOVERY_NOT_FOUND'
        printf '%s\n' "DISCOVERY_SCRIPT=$DISCOVERY_SCRIPT"
        return 11
    fi

    SOURCE_COMMIT=$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf '%s' 'UNKNOWN')
    RUN_ID="run_$(date +%Y%m%d_%H%M%S)_$$"
    RUN_DIR="$PROJECT_ROOT/artifacts/pipeline/$RUN_ID"

    if ! mkdir -p "$RUN_DIR"; then
        printf '%s\n' 'PIPELINE_STATUS=BLOCKED'
        printf '%s\n' 'PIPELINE_ERROR=RUN_DIRECTORY_CREATE_FAILED'
        printf '%s\n' "RUN_DIR=$RUN_DIR"
        return 12
    fi

    STATE_FILE="$RUN_DIR/pipeline_state.txt"
    TRANSCRIPT="$RUN_DIR/runtime_discovery_output.txt"
    SUMMARY="$RUN_DIR/manifest.txt"

    if ! {
        printf '%s\n' 'schema_version=pipeline-state.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' 'project_id=alfa_device_ctrl'
        printf '%s\n' "project_root=$PROJECT_ROOT"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "invocation_cwd=$INVOCATION_CWD"
        printf '%s\n' "execution_cwd=$PROJECT_ROOT"
        printf '%s\n' 'stage_order=EDE,CDE,GATE3'
        printf '%s\n' 'pipeline_status=RUNNING'
        printf '%s\n' 'ede_rc=UNSET'
        printf '%s\n' 'cde_rc=UNSET'
        printf '%s\n' 'gate3_rc=UNSET'
        printf '%s\n' "ede_artifact=artifacts/pipeline/$RUN_ID/ede/artifact.txt"
        printf '%s\n' "cde_artifact=artifacts/pipeline/$RUN_ID/cde/artifact.txt"
        printf '%s\n' "gate3_artifact=artifacts/pipeline/$RUN_ID/gate3/artifact.txt"
        printf '%s\n' 'failure_stage=NONE'
        printf '%s\n' 'failure_reason=NONE'
        printf '%s\n' 'completion_status=RUNNING'
        printf '%s\n' 'completed_at=UNSET'
        printf '%s\n' "state_artifact=artifacts/pipeline/$RUN_ID/pipeline_state.txt"
        printf '%s\n' "transcript_artifact=artifacts/pipeline/$RUN_ID/runtime_discovery_output.txt"
        printf '%s\n' "manifest_artifact=artifacts/pipeline/$RUN_ID/manifest.txt"
        printf '%s\n' 'stage_completion=EDE:NOT_STARTED,CDE:NOT_STARTED,GATE3:NOT_STARTED'
        printf '%s\n' 'artifact_hashes=UNSET'
        printf '%s\n' 'contract_result=BLOCKED'
    } > "$STATE_FILE"; then
        printf '%s\n' 'PIPELINE_STATUS=BLOCKED'
        printf '%s\n' 'PIPELINE_ERROR=INITIAL_STATE_WRITE_FAILED'
        return 13
    fi

    printf '%s\n' '=== ALFA DEVICE CTRL RUNTIME PIPELINE PHASE 5 ==='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "project_root=$PROJECT_ROOT"
    printf '%s\n' 'stage_order=EDE -> CDE -> GATE3'
    printf '%s\n' 'runtime_discovery_invocations=1'

    if CDPATH= cd -- "$PROJECT_ROOT" && bash "$DISCOVERY_SCRIPT" "$RUN_ID" "$RUN_DIR" "$INVOCATION_CWD" > "$TRANSCRIPT" 2>&1; then
        DISCOVERY_RC=0
    else
        DISCOVERY_RC=$?
    fi

    EDE_RC=$(grep '^EDE_RC=' "$SUMMARY" 2>/dev/null | sed 's/^EDE_RC=//' | sed -n '1p')
    CDE_RC=$(grep '^CDE_RC=' "$SUMMARY" 2>/dev/null | sed 's/^CDE_RC=//' | sed -n '1p')
    GATE3_RC=$(grep '^GATE3_RC=' "$SUMMARY" 2>/dev/null | sed 's/^GATE3_RC=//' | sed -n '1p')
    CONTRACT_RESULT=$(grep '^contract_result=' "$SUMMARY" 2>/dev/null | sed 's/^contract_result=//' | sed -n '1p')
    SOURCE_COMMIT=$(grep '^source_commit=' "$SUMMARY" 2>/dev/null | sed 's/^source_commit=//' | sed -n '1p')
    EDE_ARTIFACT=$(grep '^ede_artifact=' "$SUMMARY" 2>/dev/null | sed 's/^ede_artifact=//' | sed -n '1p')
    CDE_ARTIFACT=$(grep '^cde_artifact=' "$SUMMARY" 2>/dev/null | sed 's/^cde_artifact=//' | sed -n '1p')
    GATE3_ARTIFACT=$(grep '^gate3_artifact=' "$SUMMARY" 2>/dev/null | sed 's/^gate3_artifact=//' | sed -n '1p')
    STAGE_COMPLETION=$(grep '^stage_completion=' "$SUMMARY" 2>/dev/null | sed 's/^stage_completion=//' | sed -n '1p')
    ARTIFACT_HASHES=$(grep '^artifact_hashes=' "$SUMMARY" 2>/dev/null | sed 's/^artifact_hashes=//' | sed -n '1p')

    if [ "$DISCOVERY_RC" -eq 0 ] \
       && [ -s "$TRANSCRIPT" ] \
       && [ -s "$SUMMARY" ] \
       && grep -q '^RUNTIME_DISCOVERY_STATUS=PASS$' "$TRANSCRIPT" \
       && [ "$EDE_RC" = '0' ] \
       && [ "$CDE_RC" = '0' ] \
       && [ "$GATE3_RC" = '0' ] \
       && [ "$CONTRACT_RESULT" = 'VALID' ] \
       && [ -n "$SOURCE_COMMIT" ] \
       && [ "$SOURCE_COMMIT" != 'UNKNOWN' ]; then
        FINAL_STATUS=SUCCESS
        FAILURE_STAGE=NONE
        FAILURE_REASON=NONE
        COMPLETION_STATUS=COMPLETE
    elif [ "$DISCOVERY_RC" -ne 0 ]; then
        FINAL_STATUS=FAILED
        FAILURE_STAGE=RUNTIME_DISCOVERY
        FAILURE_REASON=RUNTIME_DISCOVERY_NONZERO_RC
        COMPLETION_STATUS=FAILED
    else
        FINAL_STATUS=BLOCKED
        FAILURE_STAGE=ARTIFACT_CONTRACT
        FAILURE_REASON=RUNTIME_DISCOVERY_OUTPUT_INVALID_OR_INCOMPLETE
        COMPLETION_STATUS=BLOCKED
    fi

    if [ -z "$EDE_RC" ]; then EDE_RC=UNSET; fi
    if [ -z "$CDE_RC" ]; then CDE_RC=UNSET; fi
    if [ -z "$GATE3_RC" ]; then GATE3_RC=UNSET; fi
    if [ -z "$CONTRACT_RESULT" ]; then CONTRACT_RESULT=BLOCKED; fi
    if [ -z "$SOURCE_COMMIT" ]; then SOURCE_COMMIT=UNKNOWN; fi
    if [ -z "$EDE_ARTIFACT" ]; then EDE_ARTIFACT="artifacts/pipeline/$RUN_ID/ede/artifact.txt"; fi
    if [ -z "$CDE_ARTIFACT" ]; then CDE_ARTIFACT="artifacts/pipeline/$RUN_ID/cde/artifact.txt"; fi
    if [ -z "$GATE3_ARTIFACT" ]; then GATE3_ARTIFACT="artifacts/pipeline/$RUN_ID/gate3/artifact.txt"; fi
    if [ -z "$STAGE_COMPLETION" ]; then STAGE_COMPLETION='EDE:UNKNOWN,CDE:UNKNOWN,GATE3:UNKNOWN'; fi
    if [ -z "$ARTIFACT_HASHES" ]; then ARTIFACT_HASHES=UNKNOWN; fi

    if ! {
        printf '%s\n' 'schema_version=pipeline-state.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' 'project_id=alfa_device_ctrl'
        printf '%s\n' "project_root=$PROJECT_ROOT"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "invocation_cwd=$INVOCATION_CWD"
        printf '%s\n' "execution_cwd=$PROJECT_ROOT"
        printf '%s\n' 'stage_order=EDE,CDE,GATE3'
        printf '%s\n' "pipeline_status=$FINAL_STATUS"
        printf '%s\n' "ede_rc=$EDE_RC"
        printf '%s\n' "cde_rc=$CDE_RC"
        printf '%s\n' "gate3_rc=$GATE3_RC"
        printf '%s\n' "ede_artifact=$EDE_ARTIFACT"
        printf '%s\n' "cde_artifact=$CDE_ARTIFACT"
        printf '%s\n' "gate3_artifact=$GATE3_ARTIFACT"
        printf '%s\n' "failure_stage=$FAILURE_STAGE"
        printf '%s\n' "failure_reason=$FAILURE_REASON"
        printf '%s\n' "completion_status=$COMPLETION_STATUS"
        printf '%s\n' "completed_at=$(date '+%Y-%m-%d %H:%M:%S')"
        printf '%s\n' "state_artifact=artifacts/pipeline/$RUN_ID/pipeline_state.txt"
        printf '%s\n' "transcript_artifact=artifacts/pipeline/$RUN_ID/runtime_discovery_output.txt"
        printf '%s\n' "manifest_artifact=artifacts/pipeline/$RUN_ID/manifest.txt"
        printf '%s\n' "stage_completion=$STAGE_COMPLETION"
        printf '%s\n' "artifact_hashes=$ARTIFACT_HASHES"
        printf '%s\n' "contract_result=$CONTRACT_RESULT"
    } > "$STATE_FILE"; then
        printf '%s\n' 'PIPELINE_STATUS=BLOCKED'
        printf '%s\n' 'PIPELINE_ERROR=FINAL_STATE_WRITE_FAILED'
        return 22
    fi

    printf '%s\n' '== RUNTIME DISCOVERY TRANSCRIPT =='
    if [ -s "$TRANSCRIPT" ]; then
        cat "$TRANSCRIPT"
    else
        printf '%s\n' 'TRANSCRIPT_STATUS=MISSING'
    fi

    printf '%s\n' '== PIPELINE FINAL STATE =='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "pipeline_status=$FINAL_STATUS"
    printf '%s\n' "runtime_discovery_rc=$DISCOVERY_RC"
    printf '%s\n' "state_artifact=$STATE_FILE"
    printf '%s\n' "transcript_artifact=$TRANSCRIPT"

    if [ "$FINAL_STATUS" = 'SUCCESS' ]; then
        return 0
    fi

    if [ "$DISCOVERY_RC" -ne 0 ]; then
        return 20
    fi

    return 21
}

main "$@"
