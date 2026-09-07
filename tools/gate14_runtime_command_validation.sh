#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT=${2:-}
OUTPUT=${3:-}
blocked(){ printf '%s\n' 'G14_STATUS=BLOCKED'; printf 'G14_REASON=%s\n' "$1"; exit 1; }
[ -n "$RUN_ID" ] && [ -n "$INPUT" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, Gate 13 artifact, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$INPUT" ] || blocked 'Gate 13 artifact missing'
HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
[ "$HEAD" != '' ] || blocked 'current Git HEAD unavailable'
field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,""); print; exit}' "$2"; }
SCHEMA=$(field schema_version "$INPUT")
GATE=$(field gate "$INPUT")
STATUS=$(field gate_status "$INPUT")
REQUEST_STATUS=$(field request_status "$INPUT")
IRUN=$(field pipeline_run_id "$INPUT")
SOURCE=$(field source_commit "$INPUT")
G4=$(field gate4_contract_sha256 "$INPUT")
PROFILE=$(field profile_sha256 "$INPUT")
KERNEL_SHA=$(field gate12_kernel_sha256 "$INPUT")
KERNEL_ARTIFACT=$(field gate12_artifact "$INPUT")
KERNEL_ID=$(field kernel_id "$INPUT")
REQUEST_ID=$(field request_id "$INPUT")
COMMAND=$(field command "$INPUT")
SEMANTICS=$(field command_semantics "$INPUT")
COMMAND_SHA=$(field command_sha256 "$INPUT")
EXEC=$(field execution_status "$INPUT")
AUTH=$(field execution_authority "$INPUT")
PATH_FIELD=$(field execution_path "$INPUT")
[ "$SCHEMA" = gate13-runtime-command-request.v1 ] || blocked 'Gate 13 schema invalid'
[ "$GATE" = gate13 ] || blocked 'Gate 13 identity invalid'
[ "$STATUS" = PASS ] || blocked 'Gate 13 is not PASS'
[ "$REQUEST_STATUS" = REQUESTED ] || blocked 'Gate 13 request status is not REQUESTED'
[ "$IRUN" = "$RUN_ID" ] || blocked 'pipeline run mismatch'
[ "$SOURCE" = "$HEAD" ] || blocked 'Gate 13 source is stale relative to current HEAD'
[[ "$G4" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 contract hash invalid'
[[ "$PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
[[ "$KERNEL_SHA" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 12 kernel hash invalid'
[ "$KERNEL_ID" = "kernel-$RUN_ID" ] || blocked 'kernel identity mismatch'
[ "$REQUEST_ID" = "pwd-request-$RUN_ID" ] || blocked 'request identity mismatch'
[ "$COMMAND" = pwd ] || blocked 'only the exact V1 pwd command is valid'
[ "$SEMANTICS" = POSIX_PWD ] || blocked 'command semantics invalid'
EXPECTED_COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
[ "$COMMAND_SHA" = "$EXPECTED_COMMAND_SHA" ] || blocked 'command hash mismatch'
[ "$EXEC" = DEFERRED ] || blocked 'upstream command must remain DEFERRED'
[ "$AUTH" = G17 ] || blocked 'execution authority must remain G17'
[ "$PATH_FIELD" = DEFERRED:G17 ] || blocked 'execution path must remain DEFERRED:G17'
[ -f "$KERNEL_ARTIFACT" ] || blocked 'referenced Gate 12 artifact missing'
ACTUAL_KERNEL_SHA=$(sha256sum "$KERNEL_ARTIFACT" 2>/dev/null | awk '{print $1}')
[ "$ACTUAL_KERNEL_SHA" = "$KERNEL_SHA" ] || blocked 'Gate 12 artifact hash mismatch'
INPUT_HASH=$(sha256sum "$INPUT" 2>/dev/null | awk '{print $1}')
[[ "$INPUT_HASH" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 13 artifact hash unavailable'
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' UNKNOWN)
[ "$NOW" != UNKNOWN ] || blocked 'timestamp unavailable'
VALIDATION_ID="pwd-validation-$RUN_ID"
mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
 printf '%s\n' 'schema_version=gate14-runtime-command-validation.v1'
 printf '%s\n' 'gate=gate14'
 printf '%s\n' 'gate_status=PASS'
 printf '%s\n' 'validation_status=ALLOWED'
 printf 'validation_id=%s\n' "$VALIDATION_ID"
 printf 'pipeline_run_id=%s\n' "$RUN_ID"
 printf 'source_commit=%s\n' "$HEAD"
 printf 'gate4_contract_sha256=%s\n' "$G4"
 printf 'profile_sha256=%s\n' "$PROFILE"
 printf 'gate13_request_sha256=%s\n' "$INPUT_HASH"
 printf 'gate13_artifact=%s\n' "$INPUT"
 printf 'gate12_kernel_sha256=%s\n' "$KERNEL_SHA"
 printf 'gate12_artifact=%s\n' "$KERNEL_ARTIFACT"
 printf 'request_id=%s\n' "$REQUEST_ID"
 printf '%s\n' 'command=pwd'
 printf '%s\n' 'command_semantics=POSIX_PWD'
 printf 'command_sha256=%s\n' "$EXPECTED_COMMAND_SHA"
 printf '%s\n' 'execution_status=DEFERRED'
 printf '%s\n' 'execution_authority=G17'
 printf '%s\n' 'execution_path=DEFERRED:G17'
 printf 'created_at=%s\n' "$NOW"
 printf 'validation_path=%s\n' "$ROOT"
 printf '%s\n' 'validation_reason=exact POSIX pwd request validated from current Gate 13 provenance and exact Gate 12 artifact hash; execution remains deferred to G17'
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write validation evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish validation evidence'; }
printf '%s\n' 'G14_STATUS=PASS'
printf '%s\n' 'G14_RESULT=COMMAND_VALIDATED'
printf 'G14_ARTIFACT=%s\n' "$OUTPUT"
printf 'VALIDATION_ID=%s\n' "$VALIDATION_ID"
printf '%s\n' 'COMMAND=pwd'
printf '%s\n' 'VALIDATION_STATUS=ALLOWED'
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
printf '%s\n' 'EXECUTION_AUTHORITY=G17'
