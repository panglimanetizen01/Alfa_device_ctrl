#!/usr/bin/env bash

set -u

main() {
    local SCRIPT_DIR PROJECT_ROOT SOURCE_COMMIT RUN_ID RUN_DIR INVOCATION_CWD
    local EDE_PAYLOAD CDE_PAYLOAD GATE3_PAYLOAD
    local EDE_ENVELOPE CDE_ENVELOPE GATE3_ENVELOPE SUMMARY MANIFEST SUMMARY_TMP
    local EDE_TMP GATE3_TMP CDE_COPY_TMP
    local EDE_RC CDE_RC GATE3_RC OVERALL_RC
    local EDE_STATUS CDE_STATUS GATE3_STATUS
    local CDE_BEFORE CDE_AFTER CDE_SOURCE CDE_SOURCE_COUNT
    local EDE_HASH CDE_HASH GATE3_HASH ARTIFACT_HASHES
    local EXECUTION_PATH

    SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)
    if [ -z "$SCRIPT_DIR" ]; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=SCRIPT_LOCATION_UNRESOLVED'
        return 10
    fi

    PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." 2>/dev/null && pwd)
    if [ -z "$PROJECT_ROOT" ] || [ ! -d "$PROJECT_ROOT" ]; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=PROJECT_ROOT_UNRESOLVED'
        return 11
    fi

    RUN_ID="${1:-}"
    RUN_DIR="${2:-}"
    INVOCATION_CWD="${3:-UNKNOWN}"

    if [ -z "$RUN_ID" ] || [ -z "$RUN_DIR" ] || [ "$RUN_ID" = '.' ] || [ "$RUN_ID" = '..' ]; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=RUN_CONTEXT_MISSING'
        return 12
    fi

    case "$RUN_DIR" in
        "$PROJECT_ROOT/artifacts/pipeline/$RUN_ID") ;;
        *)
            printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
            printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=RUN_DIRECTORY_OUTSIDE_EXPECTED_BOUNDARY'
            return 13
            ;;
    esac

    if ! cd "$PROJECT_ROOT"; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=MASTER_PATH_UNAVAILABLE'
        return 14
    fi

    SOURCE_COMMIT=$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf '%s' 'UNKNOWN')
    EXECUTION_PATH=$(pwd 2>/dev/null || printf '%s' 'UNKNOWN')

    if ! mkdir -p "$RUN_DIR/ede" "$RUN_DIR/cde" "$RUN_DIR/gate3"; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=RUN_ARTIFACT_DIRECTORY_CREATE_FAILED'
        return 15
    fi

    EDE_PAYLOAD="$RUN_DIR/ede/artifact.txt"
    CDE_PAYLOAD="$RUN_DIR/cde/artifact.txt"
    GATE3_PAYLOAD="$RUN_DIR/gate3/artifact.txt"
    EDE_ENVELOPE="$RUN_DIR/ede/envelope.txt"
    CDE_ENVELOPE="$RUN_DIR/cde/envelope.txt"
    GATE3_ENVELOPE="$RUN_DIR/gate3/envelope.txt"
    SUMMARY="$RUN_DIR/discovery_summary.txt"
    MANIFEST="$RUN_DIR/manifest.txt"
    SUMMARY_TMP="$RUN_DIR/.discovery_summary.partial"
    EDE_TMP="$RUN_DIR/ede/.artifact.partial"
    GATE3_TMP="$RUN_DIR/gate3/.artifact.partial"
    CDE_COPY_TMP="$RUN_DIR/cde/.artifact.partial"
    CDE_BEFORE="$RUN_DIR/cde/.before.list"
    CDE_AFTER="$RUN_DIR/cde/.after.list"
    OVERALL_RC=0

    printf '%s\n' '=== ALFA DEVICE CTRL RUNTIME DISCOVERY PHASE 5 ==='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "project_id=alfa_device_ctrl"
    printf '%s\n' "project_root=$PROJECT_ROOT"
    printf '%s\n' "invocation_cwd=$INVOCATION_CWD"
    printf '%s\n' "execution_cwd=$EXECUTION_PATH"
    printf '%s\n' 'stage_order=EDE,CDE,GATE3'

    if ./tools/ede.sh > "$EDE_TMP" 2>&1; then
        EDE_RC=0
    else
        EDE_RC=$?
        OVERALL_RC=1
    fi

    if [ -s "$EDE_TMP" ]; then
        if mv "$EDE_TMP" "$EDE_PAYLOAD"; then
            :
        else
            EDE_RC=1
            OVERALL_RC=1
        fi
    else
        EDE_RC=1
        OVERALL_RC=1
        printf '%s\n' 'EDE_OUTPUT=EMPTY' > "$EDE_PAYLOAD"
    fi

    printf '%s\n' "EDE_RC=$EDE_RC"
    printf '%s\n' "EDE_ARTIFACT=$EDE_PAYLOAD"

    find "$PROJECT_ROOT/artifacts/cde" -maxdepth 1 -type f -name 'cde_*.txt' -printf '%f\n' 2>/dev/null | sort > "$CDE_BEFORE"

    if ./tools/cde.sh > "$RUN_DIR/cde/cde_stdout.txt" 2>&1; then
        CDE_RC=0
    else
        CDE_RC=$?
        OVERALL_RC=1
    fi

    find "$PROJECT_ROOT/artifacts/cde" -maxdepth 1 -type f -name 'cde_*.txt' -printf '%f\n' 2>/dev/null | sort > "$CDE_AFTER"
    CDE_SOURCE=''
    CDE_SOURCE_COUNT=0

    while IFS= read -r CDE_CANDIDATE; do
        if [ -n "$CDE_CANDIDATE" ] && ! grep -Fqx "$CDE_CANDIDATE" "$CDE_BEFORE"; then
            CDE_SOURCE="$PROJECT_ROOT/artifacts/cde/$CDE_CANDIDATE"
            CDE_SOURCE_COUNT=$((CDE_SOURCE_COUNT + 1))
        fi
    done < "$CDE_AFTER"

    if [ "$CDE_SOURCE_COUNT" -eq 1 ] && [ -s "$CDE_SOURCE" ]; then
        if cp "$CDE_SOURCE" "$CDE_COPY_TMP" && mv "$CDE_COPY_TMP" "$CDE_PAYLOAD"; then
            :
        else
            CDE_RC=1
            OVERALL_RC=1
        fi
    else
        CDE_RC=1
        OVERALL_RC=1
        printf '%s\n' 'CDE_OUTPUT=UNRESOLVED_CURRENT_RUN_ARTIFACT' > "$CDE_PAYLOAD"
    fi

    printf '%s\n' "CDE_RC=$CDE_RC"
    printf '%s\n' "CDE_ARTIFACT=$CDE_PAYLOAD"
    printf '%s\n' "CDE_SOURCE_ARTIFACT=${CDE_SOURCE:-UNRESOLVED}"

    if ./tools/execution_capability.sh > "$GATE3_TMP" 2>&1; then
        GATE3_RC=0
    else
        GATE3_RC=$?
        OVERALL_RC=1
    fi

    if [ -s "$GATE3_TMP" ]; then
        if mv "$GATE3_TMP" "$GATE3_PAYLOAD"; then
            :
        else
            GATE3_RC=1
            OVERALL_RC=1
        fi
    else
        GATE3_RC=1
        OVERALL_RC=1
        printf '%s\n' 'GATE3_OUTPUT=EMPTY' > "$GATE3_PAYLOAD"
    fi

    if ! grep -Eq '^EXEC_PRIVATE=(PASS|ERROR)([[:space:]]|$)' "$GATE3_PAYLOAD" \
       || ! grep -Eq '^EXEC_SHARED=(PASS|ERROR)([[:space:]]|$)' "$GATE3_PAYLOAD" \
       || ! grep -Eq '^SCRIPT_BASH=(PASS|ERROR)([[:space:]]|$)' "$GATE3_PAYLOAD" \
       || ! grep -Eq '^SCRIPT_PYTHON=(PASS|ERROR)([[:space:]]|$)' "$GATE3_PAYLOAD" \
       || ! grep -Eq '^PROCESS_SPAWN=(PASS|ERROR)([[:space:]]|$)' "$GATE3_PAYLOAD"; then
        GATE3_RC=1
        OVERALL_RC=1
    fi

    printf '%s\n' "GATE3_RC=$GATE3_RC"
    printf '%s\n' "GATE3_ARTIFACT=$GATE3_PAYLOAD"

    if [ "$EDE_RC" -eq 0 ] && [ -s "$EDE_PAYLOAD" ]; then EDE_STATUS=COMPLETE; else EDE_STATUS=PARTIAL; fi
    if [ "$CDE_RC" -eq 0 ] && [ -s "$CDE_PAYLOAD" ]; then CDE_STATUS=COMPLETE; else CDE_STATUS=PARTIAL; fi
    if [ "$GATE3_RC" -eq 0 ] && [ -s "$GATE3_PAYLOAD" ]; then GATE3_STATUS=COMPLETE; else GATE3_STATUS=PARTIAL; fi

    EDE_HASH=$(sha256sum "$EDE_PAYLOAD" 2>/dev/null | cut -d ' ' -f1)
    CDE_HASH=$(sha256sum "$CDE_PAYLOAD" 2>/dev/null | cut -d ' ' -f1)
    GATE3_HASH=$(sha256sum "$GATE3_PAYLOAD" 2>/dev/null | cut -d ' ' -f1)
    ARTIFACT_HASHES="ede=${EDE_HASH:-UNKNOWN},cde=${CDE_HASH:-UNKNOWN},gate3=${GATE3_HASH:-UNKNOWN}"

    write_envelope() {
        local STAGE_ID PRODUCER PAYLOAD STATUS HASH INPUTS ENVELOPE CREATED
        STAGE_ID="$1"
        PRODUCER="$2"
        PAYLOAD="$3"
        STATUS="$4"
        HASH="$5"
        INPUTS="$6"
        ENVELOPE="$7"
        CREATED=$(date '+%Y-%m-%d %H:%M:%S')
        {
            printf '%s\n' 'schema_version=artifact-envelope.v1'
            printf '%s\n' "pipeline_run_id=$RUN_ID"
            printf '%s\n' "project_id=alfa_device_ctrl"
            printf '%s\n' "project_root=$PROJECT_ROOT"
            printf '%s\n' "stage_id=$STAGE_ID"
            printf '%s\n' "producer=$PRODUCER"
            printf '%s\n' "source_commit=$SOURCE_COMMIT"
            printf '%s\n' "artifact_path=${PAYLOAD#$PROJECT_ROOT/}"
            printf '%s\n' "sha256=$HASH"
            printf '%s\n' "completion_status=$STATUS"
            printf '%s\n' "created_at=$CREATED"
            printf '%s\n' "execution_path=$EXECUTION_PATH"
            printf '%s\n' "input_artifacts=$INPUTS"
            printf '%s\n' "payload_schema=stage-output.v1"
            printf '%s\n' "completed_at=$CREATED"
            printf '%s\n' "evidence_status=$STATUS"
        } > "$ENVELOPE"
    }

    write_envelope 'ede' 'tools/ede.sh' "$EDE_PAYLOAD" "$EDE_STATUS" "$EDE_HASH" 'NONE' "$EDE_ENVELOPE"
    write_envelope 'cde' 'tools/cde.sh' "$CDE_PAYLOAD" "$CDE_STATUS" "$CDE_HASH" "${EDE_PAYLOAD#$PROJECT_ROOT/}" "$CDE_ENVELOPE"
    write_envelope 'gate3' 'tools/execution_capability.sh' "$GATE3_PAYLOAD" "$GATE3_STATUS" "$GATE3_HASH" "${EDE_PAYLOAD#$PROJECT_ROOT/},${CDE_PAYLOAD#$PROJECT_ROOT/}" "$GATE3_ENVELOPE"

    if [ "$EDE_RC" -eq 0 ] && [ "$CDE_RC" -eq 0 ] && [ "$GATE3_RC" -eq 0 ] \
       && [ "$EDE_STATUS" = 'COMPLETE' ] \
       && [ "$CDE_STATUS" = 'COMPLETE' ] \
       && [ "$GATE3_STATUS" = 'COMPLETE' ] \
       && [ "$SOURCE_COMMIT" != 'UNKNOWN' ] \
       && [ -n "$EDE_HASH" ] && [ -n "$CDE_HASH" ] && [ -n "$GATE3_HASH" ]; then
        CONTRACT_RESULT=VALID
        RUNTIME_STATUS=PASS
    else
        CONTRACT_RESULT=BLOCKED
        RUNTIME_STATUS=ERROR
        OVERALL_RC=1
    fi

    {
        printf '%s\n' 'schema_version=discovery-summary.v1'
        printf '%s\n' 'manifest_schema_version=manifest.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' 'project_id=alfa_device_ctrl'
        printf '%s\n' "project_root=$PROJECT_ROOT"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "invocation_cwd=$INVOCATION_CWD"
        printf '%s\n' "execution_cwd=$EXECUTION_PATH"
        printf '%s\n' 'stage_order=EDE,CDE,GATE3'
        printf '%s\n' "EDE_RC=$EDE_RC"
        printf '%s\n' "CDE_RC=$CDE_RC"
        printf '%s\n' "GATE3_RC=$GATE3_RC"
        printf '%s\n' "ede_artifact=${EDE_PAYLOAD#$PROJECT_ROOT/}"
        printf '%s\n' 'ede_stage_id=ede'
        printf '%s\n' "ede_envelope=${EDE_ENVELOPE#$PROJECT_ROOT/}"
        printf '%s\n' "ede_sha256=$EDE_HASH"
        printf '%s\n' "ede_completion_status=$EDE_STATUS"
        printf '%s\n' "cde_artifact=${CDE_PAYLOAD#$PROJECT_ROOT/}"
        printf '%s\n' 'cde_stage_id=cde'
        printf '%s\n' "cde_envelope=${CDE_ENVELOPE#$PROJECT_ROOT/}"
        printf '%s\n' "cde_sha256=$CDE_HASH"
        printf '%s\n' "cde_completion_status=$CDE_STATUS"
        printf '%s\n' "gate3_artifact=${GATE3_PAYLOAD#$PROJECT_ROOT/}"
        printf '%s\n' 'gate3_stage_id=gate3'
        printf '%s\n' "gate3_envelope=${GATE3_ENVELOPE#$PROJECT_ROOT/}"
        printf '%s\n' "gate3_sha256=$GATE3_HASH"
        printf '%s\n' "gate3_completion_status=$GATE3_STATUS"
        printf '%s\n' "failure_stage=$([ "$OVERALL_RC" -eq 0 ] && printf NONE || printf ARTIFACT_CONTRACT)"
        printf '%s\n' "failure_reason=$([ "$OVERALL_RC" -eq 0 ] && printf NONE || printf CURRENT_RUN_ARTIFACT_OR_STAGE_FAILURE)"
        printf '%s\n' "stage_completion=EDE:$EDE_STATUS,CDE:$CDE_STATUS,GATE3:$GATE3_STATUS"
        printf '%s\n' "artifact_hashes=$ARTIFACT_HASHES"
        printf '%s\n' "contract_result=$CONTRACT_RESULT"
        printf '%s\n' "RUNTIME_DISCOVERY_STATUS=$RUNTIME_STATUS"
    } > "$SUMMARY_TMP"

    if ! mv "$SUMMARY_TMP" "$SUMMARY"; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=SUMMARY_FINALIZE_FAILED'
        return 16
    fi

    if ! cp "$SUMMARY" "$MANIFEST"; then
        printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
        printf '%s\n' 'RUNTIME_DISCOVERY_ERROR=MANIFEST_WRITE_FAILED'
        return 17
    fi

    printf '%s\n' "SUMMARY_ARTIFACT=$SUMMARY"
    printf '%s\n' "MANIFEST_ARTIFACT=$MANIFEST"
    printf '%s\n' "SOURCE_COMMIT=$SOURCE_COMMIT"
    printf '%s\n' "RUNTIME_DISCOVERY_STATUS=$RUNTIME_STATUS"
    printf '%s\n' "runtime_discovery_success_rc=$([ "$OVERALL_RC" -eq 0 ] && printf 0 || printf 1)"
    printf '%s\n' '=== END ALFA DEVICE CTRL RUNTIME DISCOVERY PHASE 5 ==='

    return "$OVERALL_RC"
}

main "$@"
