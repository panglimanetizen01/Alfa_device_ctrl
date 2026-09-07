#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT=${2:-}
OUTPUT=${3:-}
blocked(){ printf '%s\n' 'G13_STATUS=BLOCKED'; printf 'G13_REASON=%s\n' "$1"; exit 1; }
[ -n "$RUN_ID" ] && [ -n "$INPUT" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, Gate 12 artifact, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$INPUT" ] || blocked 'Gate 12 artifact missing'
HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
[ "$HEAD" != '' ] || blocked 'current Git HEAD unavailable'
field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,""); print; exit}' "$2"; }
SCHEMA=$(field schema_version "$INPUT")
GATE=$(field gate "$INPUT")
STATUS=$(field gate_status "$INPUT")
KERNEL_STATUS=$(field kernel_status "$INPUT")
IRUN=$(field pipeline_run_id "$INPUT")
SOURCE=$(field source_commit "$INPUT")
G4=$(field gate4_contract_sha256 "$INPUT")
PROFILE=$(field profile_sha256 "$INPUT")
KERNEL_ID=$(field kernel_id "$INPUT")
KERNEL_TYPE=$(field kernel_type "$INPUT")
EXEC=$(field execution_status "$INPUT")
AUTH=$(field execution_authority "$INPUT")
[ "$SCHEMA" = gate12-runtime-kernel.v1 ] || blocked 'Gate 12 schema invalid'
[ "$GATE" = gate12 ] || blocked 'Gate 12 identity invalid'
[ "$STATUS" = PASS ] && [ "$KERNEL_STATUS" = PASS ] || blocked 'Gate 12 is not PASS'
[ "$IRUN" = "$RUN_ID" ] || blocked 'pipeline run mismatch'
[ "$SOURCE" = "$HEAD" ] || blocked 'Gate 12 source is stale relative to current HEAD'
[[ "$G4" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 contract hash invalid'
[[ "$PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
[ "$KERNEL_ID" = "kernel-$RUN_ID" ] || blocked 'kernel identity mismatch'
[ "$KERNEL_TYPE" = RUNTIME_KERNEL ] || blocked 'kernel type invalid'
[ "$EXEC" = DEFERRED ] || blocked 'upstream execution must remain DEFERRED'
[ "$AUTH" = G17 ] || blocked 'execution authority must remain G17'
COMMAND='pwd'
COMMAND_SEMANTICS='POSIX_PWD'
REQUEST_ID="pwd-request-$RUN_ID"
COMMAND_SHA256=$(printf '%s\n' "$COMMAND" | sha256sum | awk '{print $1}')
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' UNKNOWN)
[ "$NOW" != UNKNOWN ] || blocked 'timestamp unavailable'
KERNEL_SHA256=$(sha256sum "$INPUT" 2>/dev/null | awk '{print $1}')
[[ "$KERNEL_SHA256" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 12 artifact hash unavailable'
mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
 printf '%s\n' 'schema_version=gate13-runtime-command-request.v1'
 printf '%s\n' 'gate=gate13'
 printf '%s\n' 'gate_status=PASS'
 printf '%s\n' 'request_status=REQUESTED'
 printf 'pipeline_run_id=%s\n' "$RUN_ID"
 printf 'source_commit=%s\n' "$HEAD"
 printf 'gate4_contract_sha256=%s\n' "$G4"
 printf 'profile_sha256=%s\n' "$PROFILE"
 printf 'gate12_kernel_sha256=%s\n' "$KERNEL_SHA256"
 printf 'gate12_artifact=%s\n' "$INPUT"
 printf 'kernel_id=%s\n' "$KERNEL_ID"
 printf 'request_id=%s\n' "$REQUEST_ID"
 printf 'command=%s\n' "$COMMAND"
 printf 'command_semantics=%s\n' "$COMMAND_SEMANTICS"
 printf 'command_sha256=%s\n' "$COMMAND_SHA256"
 printf '%s\n' 'execution_status=DEFERRED'
 printf '%s\n' 'execution_authority=G17'
 printf '%s\n' 'execution_path=DEFERRED:G17'
 printf 'created_at=%s\n' "$NOW"
 printf 'construction_path=%s\n' "$ROOT"
 printf '%s\n' 'stage_reason=concrete pwd request constructed from current Gate 12 kernel; execution deferred to G17'
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write request evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish request evidence'; }
printf '%s\n' 'G13_STATUS=PASS'
printf '%s\n' 'G13_RESULT=PWD_REQUEST_CONSTRUCTED'
printf 'G13_ARTIFACT=%s\n' "$OUTPUT"
printf 'REQUEST_ID=%s\n' "$REQUEST_ID"
printf '%s\n' 'COMMAND=pwd'
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
printf '%s\n' 'EXECUTION_AUTHORITY=G17'
