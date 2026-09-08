#!/usr/bin/env bash
# G17 live verification: creates one fresh real G4 run and drives the exact
# current-run G5-G17 chain. No synthetic upstream fixtures and no newest lookup.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
HEAD_BEFORE=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' UNKNOWN)
TMP=$(mktemp 2>/dev/null || printf '%s/test-g17-live.%s' "${TMPDIR:-$ROOT/artifacts/.tmp}" "$$")
RUN_ID=''
FAIL=0

cleanup() { rm -f "$TMP" 2>/dev/null || true; }
trap cleanup EXIT

fail() { printf 'G17_LIVE_ERROR=%s\n' "$1"; FAIL=1; }

printf '%s\n' '=== G17 LIVE CURRENT-RUN VERIFICATION ==='
printf '%s\n' "source_head_before=$HEAD_BEFORE"

if [ "$HEAD_BEFORE" = UNKNOWN ]; then
    fail SOURCE_HEAD_UNAVAILABLE
else
    bash "$ROOT/tools/runtime_pipeline.sh" > "$TMP" 2>&1
    PIPE_RC=$?
    cat "$TMP"
    RUN_ID=$(sed -n 's/^pipeline_run_id=//p' "$TMP" | sed -n '1p')
    if [ "$PIPE_RC" -ne 0 ] || [ -z "$RUN_ID" ]; then
        fail REAL_G4_PIPELINE_FAILED
    fi
fi

if [ "$FAIL" -eq 0 ]; then
    RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
    G4="$RUN_DIR/gate4/environment_contract.txt"
    G5_REQUEST_ID='gate5-self-test'
    G5_REQ="$RUN_DIR/gate5/requests/$G5_REQUEST_ID.txt"
    G5_DEC="$RUN_DIR/gate5/decisions/$G5_REQUEST_ID.txt"
    G5_AUTH="$RUN_DIR/gate5/authorizations/$G5_REQUEST_ID.txt"
    G16="$RUN_DIR/gate16/authorization.txt"
    G17="$RUN_DIR/gate17/execution.txt"

    [ -f "$G4" ] || fail G4_ARTIFACT_MISSING
    grep -Fqx 'contract_result=VALID' "$G4" || fail G4_NOT_VALID
    grep -Fqx "pipeline_run_id=$RUN_ID" "$G4" || fail G4_RUN_MISMATCH
    grep -Fqx "source_commit=$HEAD_BEFORE" "$G4" || fail G4_SOURCE_MISMATCH
fi

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' '--- G5 REAL DECISION ---'
    bash "$ROOT/tools/runtime_decision.sh" "$RUN_ID" "$G5_REQUEST_ID" local-runtime pwd runtime purpose=self-test
    RC=$?
    [ "$RC" -eq 0 ] || fail G5_DECISION_RC_$RC
fi

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' '--- G5 REAL AUTHORIZATION ---'
    bash "$ROOT/tools/runtime_execution_authorize.sh" "$G5_REQ" "$G5_DEC" "$G5_AUTH"
    RC=$?
    [ "$RC" -eq 0 ] || fail G5_AUTHORIZATION_RC_$RC
    grep -Fqx 'authorization_status=AUTHORIZED' "$G5_AUTH" || fail G5_AUTHORIZATION_NOT_AUTHORIZED
fi

run_stage() {
    local label=$1
    shift
    printf '%s\n' "--- $label ---"
    "$@"
    local rc=$?
    if [ "$rc" -ne 0 ]; then
        fail "${label}_RC_$rc"
    fi
    [ "$FAIL" -eq 0 ]
}

if [ "$FAIL" -eq 0 ]; then
    run_stage G6 bash "$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G7 bash "$ROOT/tools/runtime_session.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G8 bash "$ROOT/tools/runtime_task.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G9 bash "$ROOT/tools/runtime_action.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G10 bash "$ROOT/tools/runtime_workflow.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G11 bash "$ROOT/tools/runtime_orchestrator.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G12 bash "$ROOT/tools/runtime_kernel.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G13 bash "$ROOT/tools/runtime_command.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G14 bash "$ROOT/tools/runtime_command_validate.sh" "$RUN_ID"
fi
if [ "$FAIL" -eq 0 ]; then
    run_stage G15 bash "$ROOT/tools/runtime_command_policy.sh" "$RUN_ID"
fi

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' '--- G16 REAL AUTHORIZATION ---'
    bash "$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$RUN_DIR/gate15/policy.txt" "$G5_AUTH" "$G16"
    RC=$?
    [ "$RC" -eq 0 ] || fail G16_RC_$RC
    grep -Fqx 'gate_status=PASS' "$G16" || fail G16_NOT_PASS
    grep -Fqx 'authorization_status=AUTHORIZED' "$G16" || fail G16_NOT_AUTHORIZED
    grep -Fqx "source_commit=$HEAD_BEFORE" "$G16" || fail G16_SOURCE_MISMATCH
fi

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' '--- G17 REAL EXECUTION ---'
    bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$G16" "$G17"
    RC=$?
    [ "$RC" -eq 0 ] || fail G17_RC_$RC
fi

if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' '--- INDEPENDENT G17 EVIDENCE CHECK ---'
    [ -s "$G17" ] || fail G17_EVIDENCE_MISSING
    grep -Fqx 'schema_version=gate17-runtime-execution.v1' "$G17" || fail G17_SCHEMA
    grep -Fqx 'gate=gate17' "$G17" || fail G17_GATE_ID
    grep -Fqx 'gate_status=PASS' "$G17" || fail G17_STATUS
    grep -Fqx 'authorization_status=AUTHORIZED' "$G17" || fail G17_AUTH
    grep -Fqx 'execution_status=PASS' "$G17" || fail G17_EXECUTION
    grep -Fqx 'result_status=PASS' "$G17" || fail G17_RESULT
    grep -Fqx 'command=pwd' "$G17" || fail G17_COMMAND
    grep -Fqx 'command_semantics=POSIX_PWD' "$G17" || fail G17_SEMANTICS
    grep -Fqx 'command_returncode=0' "$G17" || fail G17_RETURN_CODE
    grep -Fqx "pipeline_run_id=$RUN_ID" "$G17" || fail G17_RUN_MISMATCH
    grep -Fqx "source_commit=$HEAD_BEFORE" "$G17" || fail G17_SOURCE_MISMATCH
    grep -Fqx "gate16_authorization_artifact=$G16" "$G17" || fail G17_G16_ARTIFACT_MISMATCH
    ACTUAL=$(awk -F= '$1=="command_result" {sub(/^[^=]*=/,"",$0); print; exit}' "$G17")
    [ "$ACTUAL" = "$ROOT" ] || fail G17_RESULT_NOT_CANONICAL_ROOT
    EXPECTED_CMD_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
    grep -Fqx "command_sha256=$EXPECTED_CMD_SHA" "$G17" || fail G17_COMMAND_HASH
    RECORDED_RESULT_SHA=$(sed -n 's/^command_result_sha256=//p' "$G17" | sed -n '1p')
    ACTUAL_RESULT_SHA=$(printf '%s\n' "$ACTUAL" | sha256sum | awk '{print $1}')
    [ "$RECORDED_RESULT_SHA" = "$ACTUAL_RESULT_SHA" ] || fail G17_RESULT_HASH
fi

HEAD_AFTER=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' UNKNOWN)
[ "$HEAD_AFTER" = "$HEAD_BEFORE" ] || fail SOURCE_CHANGED_DURING_RUN

printf '%s\n' '--- LIVE RESULT ---'
if [ "$FAIL" -eq 0 ]; then
    printf '%s\n' 'G17_LIVE_VERIFICATION=GREEN'
    printf '%s\n' "G17_RUN_ID=$RUN_ID"
    printf '%s\n' "G17_ARTIFACT=$G17"
    printf '%s\n' "G17_COMMAND_RESULT=$ACTUAL"
    printf '%s\n' "G17_ARTIFACT_SHA256=$(sha256sum "$G17" | awk '{print $1}')"
else
    printf '%s\n' 'G17_LIVE_VERIFICATION=FAIL'
    printf '%s\n' "G17_RUN_ID=${RUN_ID:-MISSING}"
fi

[ "$FAIL" -eq 0 ]
