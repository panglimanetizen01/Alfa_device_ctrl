#!/usr/bin/env bash
# Gate 11: fail-closed orchestration construction from a verified Gate 10 workflow.
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
. "$ROOT/tools/runtime_chain_common.sh"
RUN_ID=${1:-}
G10=${2:-}
OUTPUT=${3:-}
CURRENT_HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
blocked() { printf '%s\n' 'G11_STATUS=BLOCKED'; printf 'G11_REASON=%s\n' "$1"; exit 1; }
valid_token() { [ -n "$1" ] && [[ "$1" != *$'\n'* ]] && [[ "$1" != *$'\r'* ]] && [[ "$1" != *'='* ]]; }
[ -n "$RUN_ID" ] && [ -n "$G10" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, G10 artifact, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$G10" ] || blocked 'Gate 10 artifact missing'
[[ "$CURRENT_HEAD" =~ ^[0-9a-fA-F]{40}$ ]] || blocked 'current source commit unavailable'
G10_SCHEMA=$(chain_field "$G10" schema_version 2>/dev/null || printf '%s' '')
G10_GATE=$(chain_field "$G10" gate 2>/dev/null || printf '%s' '')
G10_STATUS=$(chain_field "$G10" gate_status 2>/dev/null || printf '%s' '')
G10_WORKFLOW_STATUS=$(chain_field "$G10" workflow_status 2>/dev/null || printf '%s' '')
G10_RUN=$(chain_field "$G10" pipeline_run_id 2>/dev/null || printf '%s' '')
G10_SOURCE=$(chain_field "$G10" source_commit 2>/dev/null || printf '%s' '')
G10_CONTRACT_SHA=$(chain_field "$G10" gate4_contract_sha256 2>/dev/null || printf '%s' '')
G10_PROFILE_SHA=$(chain_field "$G10" profile_sha256 2>/dev/null || printf '%s' '')
G10_ACTION_ID=$(chain_field "$G10" action_id 2>/dev/null || printf '%s' '')
G10_WORKFLOW_ID=$(chain_field "$G10" workflow_id 2>/dev/null || printf '%s' '')
G10_EXEC_STATUS=$(chain_field "$G10" execution_status 2>/dev/null || printf '%s' '')
[ "$G10_SCHEMA" = 'gate10-runtime-workflow.v1' ] || blocked 'Gate 10 schema invalid'
[ "$G10_GATE" = 'gate10' ] || blocked 'Gate 10 identity invalid'
[ "$G10_STATUS" = 'PASS' ] || blocked 'Gate 10 status is not PASS'
[ "$G10_WORKFLOW_STATUS" = 'PASS' ] || blocked 'Gate 10 workflow status is not PASS'
[ "$G10_RUN" = "$RUN_ID" ] || blocked 'Gate 10 pipeline run mismatch'
[ "$G10_SOURCE" = "$CURRENT_HEAD" ] || blocked 'Gate 10 source commit is stale relative to current HEAD'
[[ "$G10_CONTRACT_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 contract hash invalid'
[[ "$G10_PROFILE_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
valid_token "$G10_ACTION_ID" || blocked 'action identity invalid'
valid_token "$G10_WORKFLOW_ID" || blocked 'workflow identity invalid'
[ "$G10_ACTION_ID" = "action-$RUN_ID" ] || blocked 'action identity mismatch'
[ "$G10_WORKFLOW_ID" = "workflow-$RUN_ID" ] || blocked 'workflow identity mismatch'
[ "$G10_EXEC_STATUS" = 'DEFERRED' ] || blocked 'Gate 10 execution status is not DEFERRED'
CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
[ -n "$CONTRACT" ] && [ -f "$CONTRACT" ] || blocked 'Gate 4 contract missing or invalid'
chain_identity_ok "$CONTRACT" "$G10" || blocked 'Gate 10 provenance does not match Gate 4'
ORCH_ID="orchestrator-$RUN_ID"
NOW=$(chain_now)
[ "$NOW" != 'UNKNOWN' ] || blocked 'timestamp unavailable'
mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
 printf '%s\n' 'schema_version=gate11-runtime-orchestrator.v1'
 printf '%s\n' 'gate=gate11'
 printf '%s\n' 'gate_status=PASS'
 printf '%s\n' 'orchestrator_status=PASS'
 printf 'pipeline_run_id=%s\n' "$RUN_ID"
 printf 'source_commit=%s\n' "$CURRENT_HEAD"
 printf 'gate4_contract_sha256=%s\n' "$G10_CONTRACT_SHA"
 printf 'profile_sha256=%s\n' "$G10_PROFILE_SHA"
 printf 'gate10_artifact=%s\n' "$G10"
 printf 'workflow_id=%s\n' "$G10_WORKFLOW_ID"
 printf 'orchestrator_id=%s\n' "$ORCH_ID"
 printf '%s\n' 'orchestrator_name=runtime_self_orchestrator'
 printf '%s\n' 'orchestrator_type=RUNTIME_ORCHESTRATOR'
 printf '%s\n' 'execution_status=DEFERRED'
 printf '%s\n' 'execution_authority=G17'
 printf 'created_at=%s\n' "$NOW"
 printf 'execution_path=%s\n' "$ROOT"
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write orchestrator evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish orchestrator evidence'; }
printf '%s\n' 'G11_STATUS=PASS'
printf '%s\n' 'G11_RESULT=ORCHESTRATOR_CONSTRUCTED'
printf 'G11_ARTIFACT=%s\n' "$OUTPUT"
printf 'ORCHESTRATOR_ID=%s\n' "$ORCH_ID"
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
