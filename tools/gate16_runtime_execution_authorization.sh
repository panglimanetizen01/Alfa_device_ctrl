#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
POLICY_INPUT=${2:-}
G5_AUTH=${3:-}
OUTPUT=${4:-}

blocked(){
    printf '%s\n' 'G16_STATUS=BLOCKED'
    printf 'G16_REASON=%s\n' "$1"
    exit 1
}

[ -n "$RUN_ID" ] && [ -n "$POLICY_INPUT" ] && [ -n "$G5_AUTH" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, G15 policy, Gate 5 authorization, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$POLICY_INPUT" ] || blocked 'Gate 15 policy artifact missing'
[ -f "$G5_AUTH" ] || blocked 'Gate 5 authorization artifact missing'

HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
[ -n "$HEAD" ] || blocked 'current Git HEAD unavailable'

field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,"",$0); print; exit}' "$2"; }

P_SCHEMA=$(field schema_version "$POLICY_INPUT")
P_GATE=$(field gate "$POLICY_INPUT")
P_STATUS=$(field gate_status "$POLICY_INPUT")
P_POLICY=$(field policy_status "$POLICY_INPUT")
P_ID=$(field policy_id "$POLICY_INPUT")
P_RUN=$(field pipeline_run_id "$POLICY_INPUT")
P_SOURCE=$(field source_commit "$POLICY_INPUT")
P_G4=$(field gate4_contract_sha256 "$POLICY_INPUT")
P_PROFILE=$(field profile_sha256 "$POLICY_INPUT")
P_G14_SHA=$(field gate14_validation_sha256 "$POLICY_INPUT")
P_G14_ART=$(field gate14_artifact "$POLICY_INPUT")
P_G13_SHA=$(field gate13_request_sha256 "$POLICY_INPUT")
P_G13_ART=$(field gate13_artifact "$POLICY_INPUT")
P_G12_SHA=$(field gate12_kernel_sha256 "$POLICY_INPUT")
P_G12_ART=$(field gate12_artifact "$POLICY_INPUT")
P_VALIDATION_ID=$(field validation_id "$POLICY_INPUT")
P_REQUEST_ID=$(field request_id "$POLICY_INPUT")
P_COMMAND=$(field command "$POLICY_INPUT")
P_SEMANTICS=$(field command_semantics "$POLICY_INPUT")
P_COMMAND_SHA=$(field command_sha256 "$POLICY_INPUT")
P_EXEC=$(field execution_status "$POLICY_INPUT")
P_AUTH=$(field execution_authority "$POLICY_INPUT")
P_PATH=$(field execution_path "$POLICY_INPUT")

A_SCHEMA=$(field schema_version "$G5_AUTH")
A_ID=$(field authorization_id "$G5_AUTH")
A_REQUEST=$(field request_id "$G5_AUTH")
A_RUN=$(field pipeline_run_id "$G5_AUTH")
A_SOURCE=$(field source_commit "$G5_AUTH")
A_G4=$(field gate4_contract_sha256 "$G5_AUTH")
A_PROFILE=$(field profile_sha256 "$G5_AUTH")
A_POLICY=$(field policy_version "$G5_AUTH")
A_DECISION=$(field decision_id "$G5_AUTH")
A_STATUS=$(field authorization_status "$G5_AUTH")

[ "$P_SCHEMA" = gate15-runtime-command-policy.v1 ] || blocked 'Gate 15 schema invalid'
[ "$P_GATE" = gate15 ] || blocked 'Gate 15 identity invalid'
[ "$P_STATUS" = PASS ] || blocked 'Gate 15 is not PASS'
[ "$P_POLICY" = ALLOWED ] || blocked 'Gate 15 policy is not ALLOWED'
[ "$P_ID" = "policy-$RUN_ID" ] || blocked 'policy identity mismatch'
[ "$P_RUN" = "$RUN_ID" ] || blocked 'Gate 15 run mismatch'
[ "$P_SOURCE" = "$HEAD" ] || blocked 'Gate 15 source is stale relative to current HEAD'
[[ "$P_G4" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 contract hash invalid'
[[ "$P_PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
[[ "$P_G14_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 14 hash invalid'
[[ "$P_G13_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 13 hash invalid'
[[ "$P_G12_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 12 hash invalid'
[ -f "$P_G14_ART" ] || blocked 'referenced Gate 14 artifact missing'
[ -f "$P_G13_ART" ] || blocked 'referenced Gate 13 artifact missing'
[ -f "$P_G12_ART" ] || blocked 'referenced Gate 12 artifact missing'
[ "$(sha256sum "$P_G14_ART" | awk '{print $1}')" = "$P_G14_SHA" ] || blocked 'Gate 14 artifact hash mismatch'
[ "$(sha256sum "$P_G13_ART" | awk '{print $1}')" = "$P_G13_SHA" ] || blocked 'Gate 13 artifact hash mismatch'
[ "$(sha256sum "$P_G12_ART" | awk '{print $1}')" = "$P_G12_SHA" ] || blocked 'Gate 12 artifact hash mismatch'
[ "$P_VALIDATION_ID" = "pwd-validation-$RUN_ID" ] || blocked 'validation identity mismatch'
[ "$P_REQUEST_ID" = "pwd-request-$RUN_ID" ] || blocked 'request identity mismatch'
[ "$P_COMMAND" = pwd ] || blocked 'command is not exact pwd'
[ "$P_SEMANTICS" = POSIX_PWD ] || blocked 'command semantics invalid'
EXPECTED_COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
[ "$P_COMMAND_SHA" = "$EXPECTED_COMMAND_SHA" ] || blocked 'command hash mismatch'
[ "$P_EXEC" = DEFERRED ] || blocked 'execution is not DEFERRED'
[ "$P_AUTH" = G17 ] || blocked 'execution authority is not G17'
[ "$P_PATH" = DEFERRED:G17 ] || blocked 'execution path is not DEFERRED:G17'

[ "$A_SCHEMA" = gate5-authorization.v1 ] || blocked 'Gate 5 authorization schema invalid'
[ "$A_ID" != '' ] || blocked 'Gate 5 authorization id missing'
[ "$A_REQUEST" = "$P_REQUEST_ID" ] || blocked 'Gate 5 authorization request mismatch'
[ "$A_RUN" = "$RUN_ID" ] || blocked 'Gate 5 authorization run mismatch'
[ "$A_SOURCE" = "$HEAD" ] || blocked 'Gate 5 authorization source is stale'
[ "$A_G4" = "$P_G4" ] || blocked 'Gate 5 authorization Gate 4 hash mismatch'
[ "$A_PROFILE" = "$P_PROFILE" ] || blocked 'Gate 5 authorization profile hash mismatch'
[ "$A_POLICY" != '' ] || blocked 'Gate 5 authorization policy version missing'
[ "$A_DECISION" != '' ] || blocked 'Gate 5 decision id missing'
[ "$A_STATUS" = AUTHORIZED ] || blocked 'Gate 5 authorization is not AUTHORIZED'

NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' UNKNOWN)
[ "$NOW" != UNKNOWN ] || blocked 'timestamp unavailable'
POLICY_HASH=$(sha256sum "$POLICY_INPUT" | awk '{print $1}')
AUTH_HASH=$(sha256sum "$G5_AUTH" | awk '{print $1}')
[[ "$POLICY_HASH" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 15 policy hash unavailable'
[[ "$AUTH_HASH" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 5 authorization hash unavailable'
AUTH_ID="authorization-$RUN_ID"

mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
    printf '%s\n' 'schema_version=gate16-runtime-execution-authorization.v1'
    printf '%s\n' 'gate=gate16'
    printf '%s\n' 'gate_status=PASS'
    printf '%s\n' 'authorization_status=AUTHORIZED'
    printf 'authorization_id=%s\n' "$AUTH_ID"
    printf 'pipeline_run_id=%s\n' "$RUN_ID"
    printf 'source_commit=%s\n' "$HEAD"
    printf 'gate4_contract_sha256=%s\n' "$P_G4"
    printf 'profile_sha256=%s\n' "$P_PROFILE"
    printf 'gate15_policy_sha256=%s\n' "$POLICY_HASH"
    printf 'gate15_artifact=%s\n' "$POLICY_INPUT"
    printf 'gate5_authorization_sha256=%s\n' "$AUTH_HASH"
    printf 'gate5_authorization_artifact=%s\n' "$G5_AUTH"
    printf 'gate5_authorization_id=%s\n' "$A_ID"
    printf 'gate5_decision_id=%s\n' "$A_DECISION"
    printf 'request_id=%s\n' "$P_REQUEST_ID"
    printf '%s\n' 'command=pwd'
    printf '%s\n' 'command_semantics=POSIX_PWD'
    printf 'command_sha256=%s\n' "$EXPECTED_COMMAND_SHA"
    printf '%s\n' 'policy_status=ALLOWED'
    printf '%s\n' 'execution_status=DEFERRED'
    printf '%s\n' 'execution_authority=G17'
    printf '%s\n' 'execution_path=DEFERRED:G17'
    printf 'created_at=%s\n' "$NOW"
    printf 'authorization_path=%s\n' "$ROOT"
    printf '%s\n' 'authorization_reason=Gate 15 policy ALLOWED and exact Gate 5 AUTHORIZED evidence match; execution remains deferred to G17'
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write authorization evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish authorization evidence'; }

printf '%s\n' 'G16_STATUS=PASS'
printf '%s\n' 'G16_RESULT=EXECUTION_AUTHORIZED'
printf 'G16_ARTIFACT=%s\n' "$OUTPUT"
printf '%s\n' 'AUTHORIZATION_STATUS=AUTHORIZED'
printf '%s\n' 'COMMAND=pwd'
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
printf '%s\n' 'EXECUTION_AUTHORITY=G17'
