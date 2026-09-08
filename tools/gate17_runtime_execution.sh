#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
AUTH_INPUT=${2:-}
OUTPUT=${3:-}
PROOT_EXEC=${4:-}
ROOTFS=${5:-}

blocked(){
  printf '%s\n' 'G17_STATUS=BLOCKED'
  printf 'G17_REASON=%s\n' "$1"
  return 1 2>/dev/null || :
}

[ -n "$RUN_ID" ] && [ -n "$AUTH_INPUT" ] && [ -n "$OUTPUT" ] && [ -n "$PROOT_EXEC" ] && [ -n "$ROOTFS" ] || blocked 'explicit run id, G16 authorization, output, PRoot executable, and guest rootfs are required' || return 1
[[ "$RUN_ID" =~ ^run_[0-9]{8}_[0-9]{6}_[0-9]+$ ]] || { blocked 'invalid pipeline run id'; return 1; }
[ -f "$AUTH_INPUT" ] || { blocked 'G16 authorization artifact missing'; return 1; }
[ -x "$PROOT_EXEC" ] || { blocked 'explicit PRoot executable is missing or not executable'; return 1; }
[ -d "$ROOTFS" ] || { blocked 'explicit guest rootfs is missing'; return 1; }
[ -x "$ROOTFS/bin/sh" ] || { blocked 'guest rootfs /bin/sh is missing or not executable'; return 1; }
[ -f "$ROOTFS/etc/os-release" ] || { blocked 'guest rootfs identity /etc/os-release missing'; return 1; }
HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' '')
[ -n "$HEAD" ] || { blocked 'current Git HEAD unavailable'; return 1; }
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
[ "$A_SCHEMA" = gate16-runtime-execution-authorization.v1 ] || { blocked 'G16 schema invalid'; return 1; }
[ "$A_GATE" = gate16 ] || { blocked 'G16 identity invalid'; return 1; }
[ "$A_STATUS" = PASS ] || { blocked 'G16 is not PASS'; return 1; }
[ "$A_AUTH" = AUTHORIZED ] || { blocked 'G16 authorization is not AUTHORIZED'; return 1; }
[ "$A_ID" = "authorization-$RUN_ID" ] || { blocked 'G16 authorization identity mismatch'; return 1; }
[ "$A_RUN" = "$RUN_ID" ] || { blocked 'G16 run mismatch'; return 1; }
[ "$A_SOURCE" = "$HEAD" ] || { blocked 'G16 source is stale relative to current HEAD'; return 1; }
[[ "$A_G4" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'Gate 4 hash invalid'; return 1; }
[[ "$A_PROFILE" =~ ^[0-9a-fA-F]{64}$ ]] || { blocked 'profile hash invalid'; return 1; }
[ -f "$A_POLICY_ART" ] || { blocked 'referenced G15 policy artifact missing'; return 1; }
[ -f "$A_G5_ART" ] || { blocked 'referenced Gate 5 authorization artifact missing'; return 1; }
[ "$(sha256sum "$A_POLICY_ART" | awk '{print $1}')" = "$A_POLICY_SHA" ] || { blocked 'G15 policy hash mismatch'; return 1; }
[ "$(sha256sum "$A_G5_ART" | awk '{print $1}')" = "$A_G5_SHA" ] || { blocked 'Gate 5 authorization hash mismatch'; return 1; }
[ "$A_REQUEST" = "pwd-request-$RUN_ID" ] || { blocked 'request identity mismatch'; return 1; }
[ "$A_COMMAND" = pwd ] || { blocked 'command is not exact pwd'; return 1; }
[ "$A_SEMANTICS" = POSIX_PWD ] || { blocked 'command semantics invalid'; return 1; }
EXPECTED_COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
[ "$A_CMD_SHA" = "$EXPECTED_COMMAND_SHA" ] || { blocked 'command hash mismatch'; return 1; }
[ "$A_EXEC" = DEFERRED ] || { blocked 'execution state is not DEFERRED'; return 1; }
[ "$A_AUTHORITY" = G17 ] || { blocked 'execution authority is not G17'; return 1; }
[ "$A_PATH" = DEFERRED:G17 ] || { blocked 'execution path is not DEFERRED:G17'; return 1; }

PROOT_SHA=$(sha256sum "$PROOT_EXEC" | awk '{print $1}')
ROOTFS_OS_SHA=$(sha256sum "$ROOTFS/etc/os-release" | awk '{print $1}')
GUEST_OS_ID=$(awk -F= '$1=="ID" {gsub(/"/,"",$2); print $2; exit}' "$ROOTFS/etc/os-release")
[ -n "$GUEST_OS_ID" ] || { blocked 'guest rootfs ID missing'; return 1; }

TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/alfa-g17.XXXXXX") || { blocked 'temporary execution directory unavailable'; return 1; }
cleanup(){ rm -rf "$TMP_DIR"; }
trap cleanup EXIT HUP INT TERM
STDOUT_FILE="$TMP_DIR/stdout"
STDERR_FILE="$TMP_DIR/stderr"

"$PROOT_EXEC" -r "$ROOTFS" -w /root /bin/sh -c 'pwd' >"$STDOUT_FILE" 2>"$STDERR_FILE" &
PROOT_PID=$!
GUEST_PID=''
for _ in $(seq 1 100); do
  CHILDREN="/proc/$PROOT_PID/task/$PROOT_PID/children"
  if [ -r "$CHILDREN" ]; then
    GUEST_PID=$(awk '{print $1}' "$CHILDREN" 2>/dev/null || printf '%s' '')
    [ -n "$GUEST_PID" ] && [ -d "/proc/$GUEST_PID" ] && break
  fi
  sleep 0.001
 done
wait "$PROOT_PID"
RETURN_CODE=$?
[ "$RETURN_CODE" -eq 0 ] || { blocked "PRoot execution failed with return code $RETURN_CODE"; return 1; }
[ -n "$GUEST_PID" ] || { blocked 'guest process PID could not be observed'; return 1; }
RESULT=$(cat "$STDOUT_FILE" 2>/dev/null) || { blocked 'guest stdout unavailable'; return 1; }
RESULT=${RESULT%$'\n'}
[ "$RESULT" = /root ] || { blocked 'guest pwd result is not the authorized guest working directory'; return 1; }

PROC_EXE=$(readlink "/proc/$GUEST_PID/exe" 2>/dev/null || printf '%s' '')
PROC_CWD=$(readlink "/proc/$GUEST_PID/cwd" 2>/dev/null || printf '%s' '')
PROC_ROOT=$(readlink "/proc/$GUEST_PID/root" 2>/dev/null || printf '%s' '')
[ -n "$PROC_EXE" ] || { blocked 'guest process /proc/exe evidence unavailable'; return 1; }
[ -n "$PROC_ROOT" ] || { blocked 'guest process /proc/root evidence unavailable'; return 1; }
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' '')
[ -n "$NOW" ] || { blocked 'timestamp unavailable'; return 1; }
EXEC_SHA=$(printf '%s\n' "$RESULT" | sha256sum | awk '{print $1}')
AUTH_SHA=$(sha256sum "$AUTH_INPUT" | awk '{print $1}')
mkdir -p "$(dirname -- "$OUTPUT")" || { blocked 'cannot create output directory'; return 1; }
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
  printf '%s\n' "gate16_authorization_sha256=$AUTH_SHA"
  printf '%s\n' "gate16_authorization_artifact=$AUTH_INPUT"
  printf '%s\n' "request_id=$A_REQUEST"
  printf '%s\n' 'command=pwd'
  printf '%s\n' 'command_semantics=POSIX_PWD'
  printf '%s\n' "command_sha256=$EXPECTED_COMMAND_SHA"
  printf '%s\n' 'authorization_status=AUTHORIZED'
  printf '%s\n' 'execution_status=EXECUTED'
  printf '%s\n' 'result_status=PASS'
  printf '%s\n' "command_result=$RESULT"
  printf '%s\n' "command_returncode=$RETURN_CODE"
  printf '%s\n' "command_result_sha256=$EXEC_SHA"
  printf '%s\n' "engine_path=$PROOT_EXEC"
  printf '%s\n' "engine_sha256=$PROOT_SHA"
  printf '%s\n' "rootfs_path=$ROOTFS"
  printf '%s\n' "rootfs_os_id=$GUEST_OS_ID"
  printf '%s\n' "rootfs_os_release_sha256=$ROOTFS_OS_SHA"
  printf '%s\n' "guest_pid=$GUEST_PID"
  printf '%s\n' "guest_proc_exe=$PROC_EXE"
  printf '%s\n' "guest_proc_cwd=$PROC_CWD"
  printf '%s\n' "guest_proc_root=$PROC_ROOT"
  printf '%s\n' "created_at=$NOW"
  printf '%s\n' 'execution_path=PRoot:-r:<rootfs>:-w:/root:/bin/sh:-c:pwd'
  printf '%s\n' 'execution_reason=G16 authorization verified; exact pwd command executed inside explicit PRoot guest rootfs'
} > "$TMP" || { rm -f "$TMP"; blocked 'failed to write execution evidence'; return 1; }
mv "$TMP" "$OUTPUT" || { rm -f "$TMP"; blocked 'failed to publish execution evidence'; return 1; }
printf '%s\n' 'G17_STATUS=PASS'
printf '%s\n' 'G17_RESULT=EXECUTED'
printf 'G17_ARTIFACT=%s\n' "$OUTPUT"
printf 'COMMAND_RESULT=%s\n' "$RESULT"
printf 'COMMAND_RETURNCODE=%s\n' "$RETURN_CODE"
printf 'GUEST_PID=%s\n' "$GUEST_PID"
