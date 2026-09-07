#!/usr/bin/env bash
# Gate 8: fail-closed runtime task construction from a verified Gate 7 session.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
. "$ROOT/tools/runtime_chain_common.sh"

RUN_ID=${1:-}
G7=${2:-}
OUTPUT=${3:-}
CURRENT_HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')

blocked() {
    printf '%s\n' 'G8_STATUS=BLOCKED'
    printf 'G8_REASON=%s\n' "$1"
    return 1
}

valid_token() {
    [ -n "$1" ] && [[ "$1" != *$'\n'* ]] && [[ "$1" != *$'\r'* ]] && [[ "$1" != *'='* ]]
}

if [ -z "$RUN_ID" ] || [ -z "$G7" ] || [ -z "$OUTPUT" ]; then
    blocked 'explicit run id, G7 artifact, and output are required' || true
    exit 1
fi

if ! [[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]]; then
    blocked 'invalid pipeline run id' || true
    exit 1
fi

if [ ! -f "$G7" ]; then
    blocked 'Gate 7 artifact missing' || true
    exit 1
fi

if ! [[ "$CURRENT_HEAD" =~ ^[0-9a-fA-F]{40}$ ]]; then
    blocked 'current source commit unavailable' || true
    exit 1
fi

G7_SCHEMA=$(chain_field "$G7" schema_version 2>/dev/null || printf '%s' '')
G7_GATE=$(chain_field "$G7" gate 2>/dev/null || printf '%s' '')
G7_STATUS=$(chain_field "$G7" gate_status 2>/dev/null || printf '%s' '')
G7_SESSION_STATUS=$(chain_field "$G7" session_status 2>/dev/null || printf '%s' '')
G7_RUN=$(chain_field "$G7" pipeline_run_id 2>/dev/null || printf '%s' '')
G7_SOURCE=$(chain_field "$G7" source_commit 2>/dev/null || printf '%s' '')
G7_CONTRACT_SHA=$(chain_field "$G7" gate4_contract_sha256 2>/dev/null || printf '%s' '')
G7_PROFILE_SHA=$(chain_field "$G7" profile_sha256 2>/dev/null || printf '%s' '')
SESSION_ID=$(chain_field "$G7" session_id 2>/dev/null || printf '%s' '')
REQUEST_ID=$(chain_field "$G7" request_id 2>/dev/null || printf '%s' '')
RUNTIME_ID=$(chain_field "$G7" runtime_id 2>/dev/null || printf '%s' '')

[ "$G7_SCHEMA" = 'gate7-runtime-session.v1' ] || { blocked 'Gate 7 schema invalid' || true; exit 1; }
[ "$G7_GATE" = 'gate7' ] || { blocked 'Gate 7 identity invalid' || true; exit 1; }
[ "$G7_STATUS" = 'PASS' ] || { blocked 'Gate 7 status is not PASS' || true; exit 1; }
[ "$G7_SESSION_STATUS" = 'PASS' ] || { blocked 'Gate 7 session status is not PASS' || true; exit 1; }
[ "$G7_RUN" = "$RUN_ID" ] || { blocked 'Gate 7 pipeline run mismatch' || true; exit 1; }
[ "$G7_SOURCE" = "$CURRENT_HEAD" ] || { blocked 'Gate 7 source commit is stale relative to current HEAD' || true; exit 1; }
[[ "$G7_CONTRACT_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'Gate 4 contract hash invalid' || true; exit 1; }
[[ "$G7_PROFILE_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'profile hash invalid' || true; exit 1; }
valid_token "$SESSION_ID" || { blocked 'session identity invalid' || true; exit 1; }
valid_token "$REQUEST_ID" || { blocked 'request identity invalid' || true; exit 1; }
valid_token "$RUNTIME_ID" || { blocked 'runtime identity invalid' || true; exit 1; }

CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
[ -n "$CONTRACT" ] && [ -f "$CONTRACT" ] || { blocked 'Gate 4 contract missing or invalid' || true; exit 1; }
chain_identity_ok "$CONTRACT" "$G7" || { blocked 'Gate 7 provenance does not match Gate 4' || true; exit 1; }

case "$RUNTIME_ID" in
    UNKNOWN|'') blocked 'runtime identity is UNKNOWN or missing' || true; exit 1 ;;
esac

TASK_ID="task-${RUN_ID}"
NOW=$(chain_now)
[ "$NOW" != 'UNKNOWN' ] || { blocked 'timestamp unavailable' || true; exit 1; }

mkdir -p "$(dirname -- "$OUTPUT")" || { blocked 'cannot create output directory' || true; exit 1; }
TMP="$OUTPUT.partial.$$"
{
    printf '%s\n' 'schema_version=gate8-runtime-task.v1'
    printf '%s\n' 'gate=gate8'
    printf '%s\n' 'gate_status=PASS'
    printf '%s\n' 'task_status=PASS'
    printf 'pipeline_run_id=%s\n' "$RUN_ID"
    printf 'source_commit=%s\n' "$CURRENT_HEAD"
    printf 'gate4_contract_sha256=%s\n' "$G7_CONTRACT_SHA"
    printf 'profile_sha256=%s\n' "$G7_PROFILE_SHA"
    printf 'gate7_artifact=%s\n' "$G7"
    printf 'session_id=%s\n' "$SESSION_ID"
    printf 'request_id=%s\n' "$REQUEST_ID"
    printf 'runtime_id=%s\n' "$RUNTIME_ID"
    printf 'task_id=%s\n' "$TASK_ID"
    printf '%s\n' 'task_name=runtime_self_test'
    printf '%s\n' 'task_type=SESSION_TASK'
    printf '%s\n' 'execution_status=DEFERRED'
    printf '%s\n' 'execution_authority=G17'
    printf 'created_at=%s\n' "$NOW"
    printf 'execution_path=%s\n' "$ROOT"
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write task evidence' || true; exit 1; }

mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish task evidence' || true; exit 1; }
printf '%s\n' 'G8_STATUS=PASS'
printf '%s\n' 'G8_RESULT=TASK_CONSTRUCTED'
printf 'G8_ARTIFACT=%s\n' "$OUTPUT"
printf 'TASK_ID=%s\n' "$TASK_ID"
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
exit 0
