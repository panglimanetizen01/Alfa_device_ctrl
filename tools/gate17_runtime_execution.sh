#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
AUTH_INPUT=${2:-}
OUTPUT=${3:-}

blocked(){
  printf '%s\n' 'G17_STATUS=BLOCKED'
  printf 'G17_REASON=%s\n' "$1"
  exit 1
}

[ -n "$RUN_ID" ] && [ -n "$AUTH_INPUT" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, G16 authorization, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$AUTH_INPUT" ] || blocked 'G16 authorization artifact missing'
HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
[ -n "$HEAD" ] || blocked 'current Git HEAD unavailable'
field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,"",$0); print; exit}' "$2"; }
A_SCHEMA=$(field schema_version "$AUTH_INPUT")
A_GATE=$(field gate "$AUTH_INPUT")
A_STATUS=$(field gate_status "$AUTH_INPUT")
A_AUTH=$(field authorization_status "$AUTH_INPUT")
A_ID=$(field authorization_id "$AUTH_INPUT")
A_RUN=$(field pipeline_run_id "$AUTH_INPUT")
A_SOURCE=$(field source_commit "$AUTH_INPUT")
A_G4=$(field gate4_contract_sha256 "$AUTH_INPUT")
A_PROFILE=$(field profile_sha256 "$AUTH_INPUT")
A_POLICY_SHA=$(field gate15_policy_sha256 "$AUTH_INPUT")
A_POLICY_ART=$(field gate15_artifact "$AUTH_INPUT")
A_G5_SHA=$(field gate5_authorization_sha256 "$AUTH_INPUT")
A_G5_ART=$(field gate5_authorization_artifact "$AUTH_INPUT")
A_REQUEST=$(field request_id "$AUTH_INPUT")
A_COMMAND=$(field command "$AUTH_INPUT")
A_SEMANTICS=$(field command_semantics "$AUTH_INPUT")
A_CMD_SHA=$(field command_sha256 "$AUTH_INPUT")
A_EXEC=$(field execution_status "$AUTH_INPUT")
A_AUTHORITY=$(field execution_authority "$AUTH_INPUT")
A_PATH=$(field execution_path "$AUTH_INPUT")
[ "$A_SCHEMA" = gate16-runtime-execution-authorization.v1 ] || blocked 'G16 schema invalid'
[ "$A_GATE" = gate16 ] || blocked 'G16 identity invalid'
[ "$A_STATUS" = PASS ] || blocked 'G16 is not PASS'
[ "$A_AUTH" = AUTHORIZED ] || blocked 'G16 authorization is not AUTHORIZED'
[ "$A_ID" = "authorization-$RUN_ID" ] || blocked 'G16 authorization identity mismatch'
[ "$A_RUN" = "$RUN_ID" ] || blocked 'G16 run mismatch'
[ "$A_SOURCE" = "$HEAD" ] || blocked 'G16 source is stale relative to current HEAD'
[[ "$A_G4" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 hash invalid'
[[ "$A_PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
[ -f "$A_POLICY_ART" ] || blocked 'referenced G15 policy artifact missing'
[ -f "$A_G5_ART" ] || blocked 'referenced Gate 5 authorization artifact missing'
[ "$(sha256sum "$A_POLICY_ART" | awk '{print $1}')" = "$A_POLICY_SHA" ] || blocked 'G15 policy hash mismatch'
[ "$(sha256sum "$A_G5_ART" | awk '{print $1}')" = "$A_G5_SHA" ] || blocked 'Gate 5 authorization hash mismatch'
[ "$A_REQUEST" = "pwd-request-$RUN_ID" ] || blocked 'request identity mismatch'
[ "$A_COMMAND" = pwd ] || blocked 'command is not exact pwd'
[ "$A_SEMANTICS" = POSIX_PWD ] || blocked 'command semantics invalid'
EXPECTED_COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
[ "$A_CMD_SHA" = "$EXPECTED_COMMAND_SHA" ] || blocked 'command hash mismatch'
[ "$A_EXEC" = DEFERRED ] || blocked 'execution state is not DEFERRED'
[ "$A_AUTHORITY" = G17 ] || blocked 'execution authority is not G17'
[ "$A_PATH" = DEFERRED:G17 ] || blocked 'execution path is not DEFERRED:G17'

cd "$ROOT" 2>/dev/null || blocked 'execution working directory unavailable'
RESULT=$(pwd 2>/dev/null) || blocked 'pwd execution failed'
[ -n "$RESULT" ] || blocked 'pwd returned empty result'
RETURN_CODE=0
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' '')
[ -n "$NOW" ] || blocked 'timestamp unavailable'
EXEC_SHA=$(printf '%s\n' "$RESULT" | sha256sum | awk '{print $1}')
mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
  printf '%s\n' 'schema_version=gate17-runtime-execution.v1'
  printf '%s\n' 'gate=gate17'
  printf '%s\n' 'gate_status=PASS'
  printf '%s\n' "execution_id=execution-pwd-$RUN_ID"
  printf '%s\n' "pipeline_run_id=$RUN_ID"
  printf '%s\n' "source_commit=$HEAD"
  printf '%s\n' "gate4_contract_sha256=$A_G4"
  printf '%s\n' "profile_sha256=$A_PROFILE"
  printf '%s\n' "gate16_authorization_sha256=$(sha256sum "$AUTH_INPUT" | awk '{print $1}')"
  printf '%s\n' "gate16_authorization_artifact=$AUTH_INPUT"
  printf '%s\n' "request_id=$A_REQUEST"
  printf '%s\n' 'command=pwd'
  printf '%s\n' 'command_semantics=POSIX_PWD'
  printf '%s\n' "command_sha256=$EXPECTED_COMMAND_SHA"
  printf '%s\n' 'authorization_status=AUTHORIZED'
  printf '%s\n' 'execution_status=PASS'
  printf '%s\n' 'result_status=PASS'
  printf '%s\n' "command_result=$RESULT"
  printf '%s\n' "command_returncode=$RETURN_CODE"
  printf '%s\n' "command_result_sha256=$EXEC_SHA"
  printf '%s\n' "created_at=$NOW"
  printf '%s\n' "execution_path=$ROOT"
  printf '%s\n' 'execution_reason=G16 authorization verified; exact pwd command executed'
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write execution evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish execution evidence'; }
printf '%s\n' 'G17_STATUS=PASS'
printf '%s\n' 'G17_RESULT=EXECUTED'
printf 'G17_ARTIFACT=%s\n' "$OUTPUT"
printf 'COMMAND_RESULT=%s\n' "$RESULT"
printf '%s\n' 'COMMAND_RETURNCODE=0'
