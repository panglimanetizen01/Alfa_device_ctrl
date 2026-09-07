#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT=${2:-}
OUTPUT=${3:-}

blocked(){
    printf '%s\n' 'G15_STATUS=BLOCKED'
    printf 'G15_REASON=%s\n' "$1"
    exit 1
}

[ -n "$RUN_ID" ] && [ -n "$INPUT" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, Gate 14 artifact, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$INPUT" ] || blocked 'Gate 14 artifact missing'

HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
[ -n "$HEAD" ] || blocked 'current Git HEAD unavailable'

field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,""); print; exit}' "$2"; }

SCHEMA=$(field schema_version "$INPUT")
GATE=$(field gate "$INPUT")
GATE_STATUS=$(field gate_status "$INPUT")
VALIDATION_STATUS=$(field validation_status "$INPUT")
VALIDATION_ID=$(field validation_id "$INPUT")
IRUN=$(field pipeline_run_id "$INPUT")
SOURCE=$(field source_commit "$INPUT")
G4=$(field gate4_contract_sha256 "$INPUT")
PROFILE=$(field profile_sha256 "$INPUT")
G13_SHA=$(field gate13_request_sha256 "$INPUT")
G12_SHA=$(field gate12_kernel_sha256 "$INPUT")
REQUEST_ID=$(field request_id "$INPUT")
COMMAND=$(field command "$INPUT")
SEMANTICS=$(field command_semantics "$INPUT")
COMMAND_SHA=$(field command_sha256 "$INPUT")
EXEC=$(field execution_status "$INPUT")
AUTH=$(field execution_authority "$INPUT")
EXEC_PATH=$(field execution_path "$INPUT")

[ "$SCHEMA" = gate14-runtime-command-validation.v1 ] || blocked 'Gate 14 schema invalid'
[ "$GATE" = gate14 ] || blocked 'Gate 14 identity invalid'
[ "$GATE_STATUS" = PASS ] || blocked 'Gate 14 is not PASS'
[ "$VALIDATION_STATUS" = ALLOWED ] || blocked 'Gate 14 validation is not ALLOWED'
[ "$VALIDATION_ID" = "pwd-validation-$RUN_ID" ] || blocked 'validation identity mismatch'
[ "$IRUN" = "$RUN_ID" ] || blocked 'pipeline run mismatch'
[ "$SOURCE" = "$HEAD" ] || blocked 'Gate 14 source is stale relative to current HEAD'
[[ "$G4" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 contract hash invalid'
[[ "$PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
[[ "$G13_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 13 request hash invalid'
[[ "$G12_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 12 kernel hash invalid'
[ "$REQUEST_ID" = "pwd-request-$RUN_ID" ] || blocked 'request identity mismatch'
[ "$COMMAND" = pwd ] || blocked 'policy V1 accepts only exact pwd command'
[ "$SEMANTICS" = POSIX_PWD ] || blocked 'command semantics invalid'
EXPECTED_COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
[ "$COMMAND_SHA" = "$EXPECTED_COMMAND_SHA" ] || blocked 'command hash mismatch'
[ "$EXEC" = DEFERRED ] || blocked 'execution must remain DEFERRED'
[ "$AUTH" = G17 ] || blocked 'execution authority must remain G17'
[ "$EXEC_PATH" = DEFERRED:G17 ] || blocked 'execution path must remain DEFERRED:G17'

POLICY_VERSION='gate15-policy-v1'
POLICY_STATUS='ALLOWED'
POLICY_REASON='exact validated POSIX pwd request is permitted; execution remains deferred to G17'
POLICY_ID="policy-$RUN_ID"
INPUT_HASH=$(sha256sum "$INPUT" 2>/dev/null | awk '{print $1}')
[[ "$INPUT_HASH" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 14 artifact hash unavailable'
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' UNKNOWN)
[ "$NOW" != UNKNOWN ] || blocked 'timestamp unavailable'

mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
    printf '%s\n' 'schema_version=gate15-runtime-command-policy.v1'
    printf '%s\n' 'gate=gate15'
    printf '%s\n' 'gate_status=PASS'
    printf '%s\n' "policy_status=$POLICY_STATUS"
    printf '%s\n' "policy_id=$POLICY_ID"
    printf '%s\n' "policy_version=$POLICY_VERSION"
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "source_commit=$HEAD"
    printf '%s\n' "gate4_contract_sha256=$G4"
    printf '%s\n' "profile_sha256=$PROFILE"
    printf '%s\n' "gate14_validation_sha256=$INPUT_HASH"
    printf '%s\n' "gate14_artifact=$INPUT"
    printf '%s\n' "gate13_request_sha256=$G13_SHA"
    printf '%s\n' "gate12_kernel_sha256=$G12_SHA"
    printf '%s\n' "validation_id=$VALIDATION_ID"
    printf '%s\n' "request_id=$REQUEST_ID"
    printf '%s\n' 'command=pwd'
    printf '%s\n' 'command_semantics=POSIX_PWD'
    printf 'command_sha256=%s\n' "$EXPECTED_COMMAND_SHA"
    printf '%s\n' "policy_reason=$POLICY_REASON"
    printf '%s\n' 'execution_status=DEFERRED'
    printf '%s\n' 'execution_authority=G17'
    printf '%s\n' 'execution_path=DEFERRED:G17'
    printf 'created_at=%s\n' "$NOW"
    printf 'policy_path=%s\n' "$ROOT"
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write policy evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish policy evidence'; }

printf '%s\n' 'G15_STATUS=PASS'
printf '%s\n' 'G15_RESULT=COMMAND_ALLOWED'
printf 'G15_ARTIFACT=%s\n' "$OUTPUT"
printf 'POLICY_ID=%s\n' "$POLICY_ID"
printf '%s\n' "POLICY_STATUS=$POLICY_STATUS"
printf '%s\n' 'COMMAND=pwd'
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
printf '%s\n' 'EXECUTION_AUTHORITY=G17'
