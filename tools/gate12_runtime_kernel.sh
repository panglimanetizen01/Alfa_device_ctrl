#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
INPUT=${2:-}
OUTPUT=${3:-}
blocked(){ printf '%s\n' 'G12_STATUS=BLOCKED'; printf 'G12_REASON=%s\n' "$1"; exit 1; }
[ -n "$RUN_ID" ] && [ -n "$INPUT" ] && [ -n "$OUTPUT" ] || blocked 'explicit run id, G11 artifact, and output are required'
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || blocked 'invalid pipeline run id'
[ -f "$INPUT" ] || blocked 'Gate 11 artifact missing'
HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
field(){ awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/,""); print; exit}' "$2"; }
SCHEMA=$(field schema_version "$INPUT"); GATE=$(field gate "$INPUT"); STATUS=$(field gate_status "$INPUT"); ORCH=$(field orchestrator_status "$INPUT"); IRUN=$(field pipeline_run_id "$INPUT"); SOURCE=$(field source_commit "$INPUT"); G4=$(field gate4_contract_sha256 "$INPUT"); PROFILE=$(field profile_sha256 "$INPUT"); OID=$(field orchestrator_id "$INPUT"); EXEC=$(field execution_status "$INPUT")
[ "$SCHEMA" = gate11-runtime-orchestrator.v1 ] || blocked 'Gate 11 schema invalid'
[ "$GATE" = gate11 ] || blocked 'Gate 11 identity invalid'
[ "$STATUS" = PASS ] && [ "$ORCH" = PASS ] || blocked 'Gate 11 is not PASS'
[ "$IRUN" = "$RUN_ID" ] || blocked 'pipeline run mismatch'
[ "$SOURCE" = "$HEAD" ] || blocked 'Gate 11 source is stale relative to current HEAD'
[[ "$G4" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'Gate 4 contract hash invalid'
[[ "$PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'profile hash invalid'
[ "$OID" = "orchestrator-$RUN_ID" ] || blocked 'orchestrator identity mismatch'
[ "$EXEC" = DEFERRED ] || blocked 'upstream execution must remain DEFERRED'
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' UNKNOWN)
[ "$NOW" != UNKNOWN ] || blocked 'timestamp unavailable'
mkdir -p "$(dirname -- "$OUTPUT")" || blocked 'cannot create output directory'
TMP="$OUTPUT.partial.$$"
{
 printf '%s\n' 'schema_version=gate12-runtime-kernel.v1'
 printf '%s\n' 'gate=gate12'
 printf '%s\n' 'gate_status=PASS'
 printf '%s\n' 'kernel_status=PASS'
 printf '%s\n' 'kernel_root_status=NOT_GRANTED'
 printf '%s\n' 'runtime_root_emulation=DEGRADED'
 printf '%s\n' 'root_claim=PRoot_UID_0_IS_NOT_KERNEL_ROOT'
 printf '%s\n' 'kernel_namespace_authority=UNKNOWN'
 printf '%s\n' 'hardware_privilege=NOT_GRANTED'
 printf 'pipeline_run_id=%s\n' "$RUN_ID"
 printf 'source_commit=%s\n' "$HEAD"
 printf 'gate4_contract_sha256=%s\n' "$G4"
 printf 'profile_sha256=%s\n' "$PROFILE"
 printf 'gate11_artifact=%s\n' "$INPUT"
 printf 'orchestrator_id=%s\n' "$OID"
 printf 'kernel_id=%s\n' "kernel-$RUN_ID"
 printf '%s\n' 'kernel_name=runtime_self_kernel'
 printf '%s\n' 'kernel_type=RUNTIME_KERNEL'
 printf '%s\n' 'execution_status=DEFERRED'
 printf '%s\n' 'execution_authority=G17'
 printf 'created_at=%s\n' "$NOW"
 printf 'execution_path=%s\n' "$ROOT"
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write kernel evidence'; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish kernel evidence'; }
printf '%s\n' 'G12_STATUS=PASS'
printf '%s\n' 'G12_RESULT=KERNEL_CONSTRUCTED'
printf 'G12_ARTIFACT=%s\n' "$OUTPUT"
printf 'KERNEL_ID=%s\n' "kernel-$RUN_ID"
printf '%s\n' 'KERNEL_ROOT_STATUS=NOT_GRANTED'
printf '%s\n' 'RUNTIME_ROOT_EMULATION=DEGRADED'
printf '%s\n' 'EXECUTION_STATUS=DEFERRED'
