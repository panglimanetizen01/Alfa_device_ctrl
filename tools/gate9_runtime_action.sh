#!/usr/bin/env bash
# Gate 9: fail-closed runtime action construction from a verified Gate 8 task.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
. "$ROOT/tools/runtime_chain_common.sh"

RUN_ID=${1:-}
G8=${2:-}
OUTPUT=${3:-}
CURRENT_HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')

blocked() {
    printf '%s\n' 'G9_STATUS=BLOCKED'
    printf 'G9_REASON=%s\n' "$1"
    return 1
}

valid_token() {
    [ -n "$1" ] && [[ "$1" != *$'\n'* ]] && [[ "$1" != *$'\r'* ]] && [[ "$1" != *'='* ]]
}

if [ -z "$RUN_ID" ] || [ -z "$G8" ] || [ -z "$OUTPUT" ]; then
    blocked 'explicit run id, G8 artifact, and output are required' || true
    exit 1
fi

if ! [[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]]; then
    blocked 'invalid pipeline run id' || true
    exit 1
fi

if [ ! -f "$G8" ]; then
    blocked 'Gate 8 artifact missing' || true
    exit 1
fi

if ! [[ "$CURRENT_HEAD" =~ ^[0-9a-fA-F]{40}$ ]]; then
    blocked 'current source commit unavailable' || true
    exit 1
fi

G8_SCHEMA=$(chain_field "$G8" schema_version 2>/dev/null || printf '%s' '')
G8_GATE=$(chain_field "$G8" gate 2>/dev/null || printf '%s' '')
G8_STATUS=$(chain_field "$G8" gate_status 2>/dev/null || printf '%s' '')
G8_TASK_STATUS=$(chain_field "$G8" task_status 2>/dev/null || printf '%s' '')
G8_RUN=$(chain_field "$G8" pipeline_run_id 2>/dev/null || printf '%s' '')
G8_SOURCE=$(chain_field "$G8" source_commit 2>/dev/null || printf '%s' '')
G8_CONTRACT_SHA=$(chain_field "$G8" gate4_contract_sha256 2>/dev/null || printf '%s' '')
G8_PROFILE_SHA=$(chain_field "$G8" profile_sha256 2>/dev/null || printf '%s' '')
G8_TASK_ID=$(chain_field "$G8" task_id 2>/dev/null || printf '%s' '')
G8_TASK_NAME=$(chain_field "$G8" task_name 2>/dev/null || printf '%s' '')
G8_TASK_TYPE=$(chain_field "$G8" task_type 2>/dev/null || printf '%s' '')
G8_EXEC_STATUS=$(chain_field "$G8" execution_status 2>/dev/null || printf '%s' '')

[ "$G8_SCHEMA" = 'gate8-runtime-task.v1' ] || { blocked 'Gate 8 schema invalid' || true; exit 1; }
[ "$G8_GATE" = 'gate8' ] || { blocked 'Gate 8 identity invalid' || true; exit 1; }
[ "$G8_STATUS" = 'PASS' ] || { blocked 'Gate 8 status is not PASS' || true; exit 1; }
[ "$G8_TASK_STATUS" = 'PASS' ] || { blocked 'Gate 8 task status is not PASS' || true; exit 1; }
[ "$G8_RUN" = "$RUN_ID" ] || { blocked 'Gate 8 pipeline run mismatch' || true; exit 1; }
[ "$G8_SOURCE" = "$CURRENT_HEAD" ] || { blocked 'Gate 8 source commit is stale relative to current HEAD' || true; exit 1; }
[[ "$G8_CONTRACT_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'Gate 4 contract hash invalid' || true; exit 1; }
[[ "$G8_PROFILE_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'profile hash invalid' || true; exit 1; }
valid_token "$G8_TASK_ID" || { blocked 'task identity invalid' || true; exit 1; }
[ "$G8_TASK_NAME" = 'runtime_self_test' ] || { blocked 'unexpected task name' || true; exit 1; }
[ "$G8_TASK_TYPE" = 'SESSION_TASK' ] || { blocked 'unexpected task type' || true; exit 1; }
[ "$G8_EXEC_STATUS" = 'DEFERRED' ] || { blocked 'Gate 8 execution status is not DEFERRED' || true; exit 1; }

CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
[ -n "$CONTRACT" ] && [ -f "$CONTRACT" ] || { blocked 'Gate 4 contract missing or invalid' || true; exit 1; }
chain_identity_ok "$CONTRACT" "$G8" || { blocked 'Gate 8 provenance does not match Gate 4' || true; exit 1; }

ACTION_ID="action-${RUN_ID}"
NOW=$(chain_now)
[ "$NOW" != 'UNKNOWN' ] || { blocked 'timestamp unavailable' || true; exit 1; }

mkdir -p "$(dirname -- "$OUTPUT")" || { blocked 'cannot create output directory' || true; exit 1; }
TMP="$OUTPUT.partial.$$"
{
    printf '%s\n' 'schema_version=gate9-runtime-action.v1'
    printf '%s\n' 'gate=gate9'
    printf '%s\n' 'gate_status=PASS'
    printf '%s\n' 'action_status=PASS'
    printf '%s\n' 'task_status=PASS'
    printf 'pipeline_run_id=%s\n' "$RUN_ID"
    printf 'source_commit=%s\n' "$CURRENT_HEAD"
    printf 'gate4_contract_sha256=%s\n' "$G8_CONTRACT_SHA"
    printf 'profile_sha256=%s\n' "$G8_PROFILE_SHA"
    printf 'gate8_artifact=%s\n' "$G8"
    printf 'task_id=%s\n' "$G8_TASK_ID"
    printf 'action_id=%s\n' "$ACTION_ID"
    printf '%s\n' 'action_name=runtime_self_action'
    printf '%s\n' 'action_type=RUNTIME_ACTION'
    printf '%s\n' 'execution_status=DEFERRED'
    printf '%s\n' 'execution_authority=G17'
    printf 'created_at=%s\n' "$NOW"
    printf 'execution_path=%s\n' "$ROOT"
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write action evidence' || true; exit 1; }

mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish action evidence' || true; exit 1; }
printf '%s\n' 'G9_STATUS=PASS'
printf '%s\n' 'G9_RESULT=ACTION_CONSTRUCTED'
printf 'G9_ARTIFACT=%s\n' "$OUTPUT"
printf 'ACTION_ID=%s\n' "$ACTION_ID"
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
exit 0
