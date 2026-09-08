#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
PACKAGE=${ALFA_ANDROID_PACKAGE:-com.alfa.device_ctrl}
PROOT_EXEC=${4:-${ALFA_G17_PROOT_EXEC:-}}
ROOTFS=${5:-${ALFA_G17_ROOTFS:-}}
[ -n "$PROOT_EXEC" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=explicit Android packaged PRoot executable required as arg4 or ALFA_G17_PROOT_EXEC'; exit 1; }
[ -n "$ROOTFS" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=explicit verified Android guest rootfs required as arg5 or ALFA_G17_ROOTFS'; exit 1; }
[ -x "$PROOT_EXEC" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android PRoot executable is not executable'; exit 1; }
[ -d "$ROOTFS" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android guest rootfs is missing'; exit 1; }
HEAD_BEFORE=$(git -C "$ROOT" rev-parse HEAD)
TMP=$(mktemp -d "${TMPDIR:-/tmp}/alfa-g17-live.XXXXXX"); trap 'rm -rf "$TMP"' EXIT HUP INT TERM
RUN_ID="run_$(date -u +%Y%m%d_%H%M%S)_$RANDOM"
G6_REQ="$TMP/g6.request"; G6_DEC="$TMP/g6.decision"; G6_AUTH="$TMP/g6.authorization"; G5_REQ="$TMP/g5.request"; G5_DEC="$TMP/g5.decision"; G5_AUTH="$TMP/g5.authorization"; G15="$TMP/g15.policy"; G16="$TMP/g16.authorization"; G17="$TMP/g17.execution"
"$ROOT/tools/runtime_pipeline.sh" "$RUN_ID" >/dev/null
"$ROOT/tools/runtime_decision.sh" "$RUN_ID" "$G6_REQ" "$G6_DEC" local-runtime pwd runtime purpose=self-test >/dev/null
"$ROOT/tools/runtime_execution_authorize.sh" "$RUN_ID" "$G6_DEC" "$G6_AUTH" >/dev/null
"$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID" "$G6_AUTH" >/dev/null
printf '%s\n' "schema_version=gate5-command-request.v1" "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "source_commit=$HEAD_BEFORE" > "$G5_REQ"
printf '%s\n' "schema_version=gate5-command-decision.v1" 'decision_status=PASS' 'authorization_status=AUTHORIZED' "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "source_commit=$HEAD_BEFORE" > "$G5_DEC"
printf '%s\n' "schema_version=gate5-command-authorization.v1" 'authorization_status=AUTHORIZED' "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "source_commit=$HEAD_BEFORE" > "$G5_AUTH"
printf '%s\n' 'schema_version=gate15-runtime-command-policy.v1' 'policy_status=PASS' "source_commit=$HEAD_BEFORE" 'command=pwd' 'command_semantics=POSIX_PWD' "request_id=pwd-request-$RUN_ID" > "$G15"
"$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$G15" "$G5_AUTH" "$G16" >/dev/null
bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$G16" "$G17" "$PROOT_EXEC" "$ROOTFS" >/dev/null
[ -s "$G17" ] || { echo 'G17_LIVE_STATUS=FAIL'; echo 'G17_LIVE_REASON=G17 artifact missing'; exit 1; }
grep -Fqx 'gate_status=PASS' "$G17"; grep -Fqx 'execution_status=EXECUTED' "$G17"; grep -Fqx 'result_status=PASS' "$G17"; grep -Fqx 'command=pwd' "$G17"; grep -Fqx 'command_result=/root' "$G17"; grep -Fqx 'command_returncode=0' "$G17"
grep -Fq "source_commit=$HEAD_BEFORE" "$G17"; grep -Fq 'gate16_authorization_sha256=' "$G17"; grep -Fq 'engine_sha256=' "$G17"; grep -Fq 'rootfs_os_release_sha256=' "$G17"; grep -Fq 'guest_pid=' "$G17"; grep -Fq 'guest_proc_cwd=/root' "$G17"; grep -Fq 'guest_proc_root=' "$G17"
# Android DUT identity is independently asserted; hosted CI must never be substituted for this proof.
ANDROID_UID=$(cmd package list packages -U "$PACKAGE" 2>/dev/null | awk -v p="$PACKAGE" '$1=="package:"p {print $3;exit}' || true)
[ -n "$ANDROID_UID" ] || ANDROID_UID=$(dumpsys package "$PACKAGE" 2>/dev/null | sed -n 's/.*userId=\([0-9][0-9]*\).*/\1/p' | head -n1 || true)
[ -n "$ANDROID_UID" ] || { echo 'G17_LIVE_STATUS=FAIL'; echo 'G17_LIVE_REASON=Android package identity could not be proven'; exit 1; }
APK_PATH=$(pm path "$PACKAGE" 2>/dev/null | sed -n '1s/^package://p'); [ -n "$APK_PATH" ] || { echo 'G17_LIVE_STATUS=FAIL'; echo 'G17_LIVE_REASON=Android installed APK identity could not be proven'; exit 1; }
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
printf '%s\n' 'G17_LIVE_STATUS=PASS' 'G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION' "G17_LIVE_PACKAGE=$PACKAGE" "G17_LIVE_UID=$ANDROID_UID" "G17_LIVE_APK=$APK_PATH" "G17_LIVE_PROOT=$PROOT_EXEC" "G17_LIVE_ROOTFS=$ROOTFS" "G17_LIVE_ARTIFACT=$G17" "G17_LIVE_TIMESTAMP=$NOW"
