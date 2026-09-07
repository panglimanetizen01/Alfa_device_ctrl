#!/usr/bin/env bash
# Gate 10: fail-closed workflow construction from a verified Gate 9 action.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
. "$ROOT/tools/runtime_chain_common.sh"

RUN_ID=${1:-}
G9=${2:-}
OUTPUT=${3:-}
CURRENT_HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')

blocked() {
    printf '%s\n' 'G10_STATUS=BLOCKED'
    printf 'G10_REASON=%s\n' "$1"
    return 1
}

valid_token() {
    [ -n "$1" ] && [[ "$1" != *$'\n'* ]] && [[ "$1" != *$'\r'* ]] && [[ "$1" != *'='* ]]
}

if [ -z "$RUN_ID" ] || [ -z "$G9" ] || [ -z "$OUTPUT" ]; then
    blocked 'explicit run id, G9 artifact, and output are required' || true
    exit 1
fi
if ! [[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]]; then blocked 'invalid pipeline run id' || true; exit 1; fi
if [ ! -f "$G9" ]; then blocked 'Gate 9 artifact missing' || true; exit 1; fi
if ! [[ "$CURRENT_HEAD" =~ ^[0-9a-fA-F]{40}$ ]]; then blocked 'current source commit unavailable' || true; exit 1; fi

G9_SCHEMA=$(chain_field "$G9" schema_version 2>/dev/null || printf '%s' '')
G9_GATE=$(chain_field "$G9" gate 2>/dev/null || printf '%s' '')
G9_STATUS=$(chain_field "$G9" gate_status 2>/dev/null || printf '%s' '')
G9_ACTION_STATUS=$(chain_field "$G9" action_status 2>/dev/null || printf '%s' '')
G9_RUN=$(chain_field "$G9" pipeline_run_id 2>/dev/null || printf '%s' '')
G9_SOURCE=$(chain_field "$G9" source_commit 2>/dev/null || printf '%s' '')
G9_CONTRACT_SHA=$(chain_field "$G9" gate4_contract_sha256 2>/dev/null || printf '%s' '')
G9_PROFILE_SHA=$(chain_field "$G9" profile_sha256 2>/dev/null || printf '%s' '')
G9_ACTION_ID=$(chain_field "$G9" action_id 2>/dev/null || printf '%s' '')
G9_ACTION_NAME=$(chain_field "$G9" action_name 2>/dev/null || printf '%s' '')
G9_ACTION_TYPE=$(chain_field "$G9" action_type 2>/dev/null || printf '%s' '')
G9_EXEC_STATUS=$(chain_field "$G9" execution_status 2>/dev/null || printf '%s' '')

[ "$G9_SCHEMA" = 'gate9-runtime-action.v1' ] || { blocked 'Gate 9 schema invalid' || true; exit 1; }
[ "$G9_GATE" = 'gate9' ] || { blocked 'Gate 9 identity invalid' || true; exit 1; }
[ "$G9_STATUS" = 'PASS' ] || { blocked 'Gate 9 status is not PASS' || true; exit 1; }
[ "$G9_ACTION_STATUS" = 'PASS' ] || { blocked 'Gate 9 action status is not PASS' || true; exit 1; }
[ "$G9_RUN" = "$RUN_ID" ] || { blocked 'Gate 9 pipeline run mismatch' || true; exit 1; }
[ "$G9_SOURCE" = "$CURRENT_HEAD" ] || { blocked 'Gate 9 source commit is stale relative to current HEAD' || true; exit 1; }
[[ "$G9_CONTRACT_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'Gate 4 contract hash invalid' || true; exit 1; }
[[ "$G9_PROFILE_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'profile hash invalid' || true; exit 1; }
valid_token "$G9_ACTION_ID" || { blocked 'action identity invalid' || true; exit 1; }
[ "$G9_ACTION_ID" = "action-$RUN_ID" ] || { blocked 'action identity is not deterministic for run' || true; exit 1; }
[ "$G9_ACTION_NAME" = 'runtime_self_action' ] || { blocked 'unexpected action name' || true; exit 1; }
[ "$G9_ACTION_TYPE" = 'RUNTIME_ACTION' ] || { blocked 'unexpected action type' || true; exit 1; }
[ "$G9_EXEC_STATUS" = 'DEFERRED' ] || { blocked 'Gate 9 execution status is not DEFERRED' || true; exit 1; }

CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
[ -n "$CONTRACT" ] && [ -f "$CONTRACT" ] || { blocked 'Gate 4 contract missing or invalid' || true; exit 1; }
chain_identity_ok "$CONTRACT" "$G9" || { blocked 'Gate 9 provenance does not match Gate 4' || true; exit 1; }

WORKFLOW_ID="workflow-$RUN_ID"
NOW=$(chain_now)
[ "$NOW" != 'UNKNOWN' ] || { blocked 'timestamp unavailable' || true; exit 1; }

mkdir -p "$(dirname -- "$OUTPUT")" || { blocked 'cannot create output directory' || true; exit 1; }
TMP="$OUTPUT.partial.$$"
{
    printf '%s\n' 'schema_version=gate10-runtime-workflow.v1'
    printf '%s\n' 'gate=gate10'
    printf '%s\n' 'gate_status=PASS'
    printf '%s\n' 'workflow_status=PASS'
    printf 'pipeline_run_id=%s\n' "$RUN_ID"
    printf 'source_commit=%s\n' "$CURRENT_HEAD"
    printf 'gate4_contract_sha256=%s\n' "$G9_CONTRACT_SHA"
    printf 'profile_sha256=%s\n' "$G9_PROFILE_SHA"
    printf 'gate9_artifact=%s\n' "$G9"
    printf 'action_id=%s\n' "$G9_ACTION_ID"
    printf 'workflow_id=%s\n' "$WORKFLOW_ID"
    printf '%s\n' 'workflow_name=runtime_self_workflow'
    printf '%s\n' 'workflow_type=RUNTIME_WORKFLOW'
    printf '%s\n' 'execution_status=DEFERRED'
    printf '%s\n' 'execution_authority=G17'
    printf 'created_at=%s\n' "$NOW"
    printf 'execution_path=%s\n' "$ROOT"
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write workflow evidence' || true; exit 1; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish workflow evidence' || true; exit 1; }
printf '%s\n' 'G10_STATUS=PASS'
printf '%s\n' 'G10_RESULT=WORKFLOW_CONSTRUCTED'
printf 'G10_ARTIFACT=%s\n' "$OUTPUT"
printf 'WORKFLOW_ID=%s\n' "$WORKFLOW_ID"
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
exit 0
