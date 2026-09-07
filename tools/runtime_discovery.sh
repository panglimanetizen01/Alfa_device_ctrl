#!/usr/bin/env bash

# Runtime discovery integration boundary.
# Records objective evidence for EDE/CDE/G3 and binds each artifact to one
# explicit pipeline run and source commit. G3 is consumed according to its
# current semantic contract: shared storage is DATA I/O, never execution.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
RUN_DIR=${2:-}
INVOCATION_CWD=${3:-UNKNOWN}

fail() {
    printf '%s\n' 'RUNTIME_DISCOVERY_STATUS=ERROR'
    printf '%s\n' "RUNTIME_DISCOVERY_ERROR=$1"
    return 1
}

[ -n "$ROOT" ] && [ -d "$ROOT" ] || { fail PROJECT_ROOT_UNRESOLVED; exit 1; }
[ -n "$RUN_ID" ] && [ "$RUN_ID" != '.' ] && [ "$RUN_ID" != '..' ] || { fail RUN_CONTEXT_MISSING; exit 1; }
[ "$RUN_DIR" = "$ROOT/artifacts/pipeline/$RUN_ID" ] || { fail RUN_DIRECTORY_OUTSIDE_EXPECTED_BOUNDARY; exit 1; }

SOURCE_COMMIT=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' UNKNOWN)
EXECUTION_PATH=$(CDPATH= cd -- "$ROOT" 2>/dev/null && pwd || printf '%s' UNKNOWN)
[ "$SOURCE_COMMIT" != UNKNOWN ] || { fail SOURCE_COMMIT_UNRESOLVED; exit 1; }

mkdir -p "$RUN_DIR/ede" "$RUN_DIR/cde" "$RUN_DIR/gate3" "$ROOT/artifacts/cde" || { fail RUN_ARTIFACT_DIRECTORY_CREATE_FAILED; exit 1; }

EDE="$RUN_DIR/ede/artifact.txt"
CDE="$RUN_DIR/cde/artifact.txt"
G3="$RUN_DIR/gate3/artifact.txt"
EDE_ENV="$RUN_DIR/ede/envelope.txt"
CDE_ENV="$RUN_DIR/cde/envelope.txt"
G3_ENV="$RUN_DIR/gate3/envelope.txt"
SUMMARY="$RUN_DIR/discovery_summary.txt"
MANIFEST="$RUN_DIR/manifest.txt"

printf '%s\n' '=== ALFA DEVICE CTRL RUNTIME DISCOVERY PHASE 5 ==='
printf '%s\n' "pipeline_run_id=$RUN_ID"
printf '%s\n' 'project_id=alfa_device_ctrl'
printf '%s\n' "project_root=$ROOT"
printf '%s\n' "invocation_cwd=$INVOCATION_CWD"
printf '%s\n' "execution_cwd=$EXECUTION_PATH"
printf '%s\n' 'stage_order=EDE,CDE,GATE3'

if (CDPATH= cd -- "$ROOT" && bash ./tools/ede.sh) > "$EDE" 2>&1; then EDE_RC=0; else EDE_RC=$?; fi
[ -s "$EDE" ] || printf '%s\n' 'EDE_OUTPUT=EMPTY' > "$EDE"
printf '%s\n' "EDE_RC=$EDE_RC"
printf '%s\n' "EDE_ARTIFACT=$EDE"

CDE_BEFORE="$RUN_DIR/cde/.before.list"
CDE_AFTER="$RUN_DIR/cde/.after.list"
find "$ROOT/artifacts/cde" -maxdepth 1 -type f -name 'cde_*.txt' -printf '%f\n' 2>/dev/null | sort > "$CDE_BEFORE"
if (CDPATH= cd -- "$ROOT" && bash ./tools/cde.sh) > "$RUN_DIR/cde/cde_stdout.txt" 2>&1; then CDE_RC=0; else CDE_RC=$?; fi
find "$ROOT/artifacts/cde" -maxdepth 1 -type f -name 'cde_*.txt' -printf '%f\n' 2>/dev/null | sort > "$CDE_AFTER"
CDE_SOURCE=''
CDE_COUNT=0
while IFS= read -r candidate; do
    if [ -n "$candidate" ] && ! grep -Fqx "$candidate" "$CDE_BEFORE"; then
        CDE_SOURCE="$ROOT/artifacts/cde/$candidate"
        CDE_COUNT=$((CDE_COUNT + 1))
    fi
done < "$CDE_AFTER"
if [ "$CDE_COUNT" -eq 1 ] && [ -s "$CDE_SOURCE" ]; then
    cp "$CDE_SOURCE" "$CDE"
else
    CDE_RC=1
    printf '%s\n' 'CDE_OUTPUT=UNRESOLVED_CURRENT_RUN_ARTIFACT' > "$CDE"
fi
printf '%s\n' "CDE_RC=$CDE_RC"
printf '%s\n' "CDE_ARTIFACT=$CDE"
printf '%s\n' "CDE_SOURCE_ARTIFACT=${CDE_SOURCE:-UNRESOLVED}"

if (CDPATH= cd -- "$ROOT" && bash ./tools/execution_capability.sh) > "$G3" 2>&1; then G3_RC=0; else G3_RC=$?; fi
[ -s "$G3" ] || printf '%s\n' 'GATE3_OUTPUT=EMPTY' > "$G3"
valid_g3_field() { grep -Eq "^$1=(PASS|ERROR)([[:space:]]|$)" "$G3"; }
if ! valid_g3_field EXEC_PRIVATE \
   || ! valid_g3_field SHARED_STORAGE_IO \
   || ! valid_g3_field SCRIPT_BASH \
   || ! valid_g3_field SCRIPT_PYTHON \
   || ! valid_g3_field PROCESS_SPAWN \
   || ! grep -q '^GATE3_STATUS=PASS$' "$G3"; then
    G3_RC=1
fi
printf '%s\n' "GATE3_RC=$G3_RC"
printf '%s\n' "GATE3_ARTIFACT=$G3"

EDE_STATUS=PARTIAL; [ "$EDE_RC" -eq 0 ] && [ -s "$EDE" ] && EDE_STATUS=COMPLETE
CDE_STATUS=PARTIAL; [ "$CDE_RC" -eq 0 ] && [ -s "$CDE" ] && CDE_STATUS=COMPLETE
G3_STATUS=PARTIAL; [ "$G3_RC" -eq 0 ] && [ -s "$G3" ] && G3_STATUS=COMPLETE
EDE_HASH=$(sha256sum "$EDE" 2>/dev/null | cut -d ' ' -f1)
CDE_HASH=$(sha256sum "$CDE" 2>/dev/null | cut -d ' ' -f1)
G3_HASH=$(sha256sum "$G3" 2>/dev/null | cut -d ' ' -f1)

write_envelope() {
    local stage producer payload status hash inputs out now
    stage=$1; producer=$2; payload=$3; status=$4; hash=$5; inputs=$6; out=$7
    now=$(date '+%Y-%m-%d %H:%M:%S')
    {
        printf '%s\n' 'schema_version=artifact-envelope.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' 'project_id=alfa_device_ctrl'
        printf '%s\n' "project_root=$ROOT"
        printf '%s\n' "stage_id=$stage"
        printf '%s\n' "producer=$producer"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "artifact_path=${payload#$ROOT/}"
        printf '%s\n' "sha256=$hash"
        printf '%s\n' "completion_status=$status"
        printf '%s\n' "created_at=$now"
        printf '%s\n' "execution_path=$EXECUTION_PATH"
        printf '%s\n' "input_artifacts=$inputs"
        printf '%s\n' 'payload_schema=stage-output.v1'
        printf '%s\n' "completed_at=$now"
        printf '%s\n' "evidence_status=$status"
    } > "$out"
}

write_envelope ede tools/ede.sh "$EDE" "$EDE_STATUS" "$EDE_HASH" NONE "$EDE_ENV"
write_envelope cde tools/cde.sh "$CDE" "$CDE_STATUS" "$CDE_HASH" "${EDE#$ROOT/}" "$CDE_ENV"
write_envelope gate3 tools/execution_capability.sh "$G3" "$G3_STATUS" "$G3_HASH" "${EDE#$ROOT/},${CDE#$ROOT/}" "$G3_ENV"

OVERALL_RC=1
CONTRACT_RESULT=BLOCKED
RUNTIME_STATUS=ERROR
if [ "$EDE_RC" -eq 0 ] && [ "$CDE_RC" -eq 0 ] && [ "$G3_RC" -eq 0 ] \
   && [ "$EDE_STATUS" = COMPLETE ] && [ "$CDE_STATUS" = COMPLETE ] && [ "$G3_STATUS" = COMPLETE ] \
   && [ -n "$EDE_HASH" ] && [ -n "$CDE_HASH" ] && [ -n "$G3_HASH" ]; then
    OVERALL_RC=0
    CONTRACT_RESULT=VALID
    RUNTIME_STATUS=PASS
fi

STAGE_COMPLETION="EDE:$EDE_STATUS,CDE:$CDE_STATUS,GATE3:$G3_STATUS"
ARTIFACT_HASHES="ede=${EDE_HASH:-UNKNOWN},cde=${CDE_HASH:-UNKNOWN},gate3=${G3_HASH:-UNKNOWN}"
FAILURE_STAGE=NONE
FAILURE_REASON=NONE
if [ "$OVERALL_RC" -ne 0 ]; then
    FAILURE_STAGE=ARTIFACT_CONTRACT
    FAILURE_REASON=CURRENT_RUN_ARTIFACT_OR_STAGE_FAILURE
fi

{
    printf '%s\n' 'schema_version=discovery-summary.v1'
    printf '%s\n' 'manifest_schema_version=manifest.v1'
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' 'project_id=alfa_device_ctrl'
    printf '%s\n' "project_root=$ROOT"
    printf '%s\n' "source_commit=$SOURCE_COMMIT"
    printf '%s\n' "invocation_cwd=$INVOCATION_CWD"
    printf '%s\n' "execution_cwd=$EXECUTION_PATH"
    printf '%s\n' 'stage_order=EDE,CDE,GATE3'
    printf '%s\n' "EDE_RC=$EDE_RC"
    printf '%s\n' "CDE_RC=$CDE_RC"
    printf '%s\n' "GATE3_RC=$G3_RC"
    printf '%s\n' "ede_artifact=${EDE#$ROOT/}"
    printf '%s\n' 'ede_stage_id=ede'
    printf '%s\n' "ede_envelope=${EDE_ENV#$ROOT/}"
    printf '%s\n' "ede_sha256=$EDE_HASH"
    printf '%s\n' "ede_completion_status=$EDE_STATUS"
    printf '%s\n' "cde_artifact=${CDE#$ROOT/}"
    printf '%s\n' 'cde_stage_id=cde'
    printf '%s\n' "cde_envelope=${CDE_ENV#$ROOT/}"
    printf '%s\n' "cde_sha256=$CDE_HASH"
    printf '%s\n' "cde_completion_status=$CDE_STATUS"
    printf '%s\n' "gate3_artifact=${G3#$ROOT/}"
    printf '%s\n' 'gate3_stage_id=gate3'
    printf '%s\n' "gate3_envelope=${G3_ENV#$ROOT/}"
    printf '%s\n' "gate3_sha256=$G3_HASH"
    printf '%s\n' "gate3_completion_status=$G3_STATUS"
    printf '%s\n' "failure_stage=$FAILURE_STAGE"
    printf '%s\n' "failure_reason=$FAILURE_REASON"
    printf '%s\n' "stage_completion=$STAGE_COMPLETION"
    printf '%s\n' "artifact_hashes=$ARTIFACT_HASHES"
    printf '%s\n' "contract_result=$CONTRACT_RESULT"
    printf '%s\n' "RUNTIME_DISCOVERY_STATUS=$RUNTIME_STATUS"
} > "$SUMMARY"
cp "$SUMMARY" "$MANIFEST" || { fail MANIFEST_WRITE_FAILED; exit 1; }

printf '%s\n' "SUMMARY_ARTIFACT=$SUMMARY"
printf '%s\n' "MANIFEST_ARTIFACT=$MANIFEST"
printf '%s\n' "SOURCE_COMMIT=$SOURCE_COMMIT"
printf '%s\n' "RUNTIME_DISCOVERY_STATUS=$RUNTIME_STATUS"
printf '%s\n' "runtime_discovery_success_rc=$OVERALL_RC"
if [ "$OVERALL_RC" -eq 0 ]; then exit 0; else exit 1; fi
