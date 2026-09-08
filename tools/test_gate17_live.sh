#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
PACKAGE=${ALFA_ANDROID_PACKAGE:-com.alfa.device_ctrl}
HEAD=$(git -C "$ROOT" rev-parse HEAD)
TMP=$(mktemp -d "${TMPDIR:-/tmp}/alfa-g17-live.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
RUN_ID="run_$(date -u +%Y%m%d_%H%M%S)_$RANDOM"
G6_REQ="$TMP/g6.request"; G6_DEC="$TMP/g6.decision"; G6_AUTH="$TMP/g6.authorization"
G5_REQ="$TMP/g5.request"; G5_DEC="$TMP/g5.decision"; G5_AUTH="$TMP/g5.authorization"
G15="$TMP/g15.policy"; G16="$TMP/g16.authorization"

# The host side may create the exact authorization chain, but it MUST NOT execute G17.
"$ROOT/tools/runtime_pipeline.sh" "$RUN_ID" >/dev/null
"$ROOT/tools/runtime_decision.sh" "$RUN_ID" "$G6_REQ" "$G6_DEC" local-runtime pwd runtime purpose=self-test >/dev/null
"$ROOT/tools/runtime_execution_authorize.sh" "$RUN_ID" "$G6_DEC" "$G6_AUTH" >/dev/null
"$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID" "$G6_AUTH" >/dev/null
printf '%s\n' "schema_version=gate5-command-request.v1" "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "source_commit=$HEAD" > "$G5_REQ"
printf '%s\n' "schema_version=gate5-command-decision.v1" 'decision_status=PASS' 'authorization_status=AUTHORIZED' "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "source_commit=$HEAD" > "$G5_DEC"
printf '%s\n' "schema_version=gate5-command-authorization.v1" 'authorization_status=AUTHORIZED' "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "source_commit=$HEAD" > "$G5_AUTH"
printf '%s\n' 'schema_version=gate15-runtime-command-policy.v1' 'policy_status=PASS' "source_commit=$HEAD" 'command=pwd' 'command_semantics=POSIX_PWD' "request_id=pwd-request-$RUN_ID" > "$G15"
"$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$G15" "$G5_AUTH" "$G16" >/dev/null

# From this point G17 is executed only by the installed Android package under instrumentation.
ANDROID_UID=$(cmd package list packages -U "$PACKAGE" 2>/dev/null | awk -v p="$PACKAGE" '$1=="package:"p {print $3;exit}' || true)
[ -n "$ANDROID_UID" ] || ANDROID_UID=$(dumpsys package "$PACKAGE" 2>/dev/null | sed -n 's/.*userId=\([0-9][0-9]*\).*/\1/p' | head -n1 || true)
[ -n "$ANDROID_UID" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android package identity could not be proven'; exit 1; }
APK_PATH=$(pm path "$PACKAGE" 2>/dev/null | sed -n '1s/^package://p')
[ -n "$APK_PATH" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android installed APK identity could not be proven'; exit 1; }

INSTRUMENTATION=$(pm list instrumentation 2>/dev/null | awk -v p="$PACKAGE" '$0 ~ "target="p {line=$0; sub(/^instrumentation:/,"",line); sub(/ \(target=.*/,"",line); print line; exit}')
[ -n "$INSTRUMENTATION" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android instrumentation targeting package not installed'; exit 1; }
TEST_CLASS='com.alfa.device_ctrl.G17AndroidRuntimeExecutionTest#exactGate16AuthorizationExecutesPwdInsidePackagedRuntime'
APP_G17_DIR='files/g17'
APP_G16="$APP_G17_DIR/gate16.authorization"
run-as "$PACKAGE" sh -c "mkdir -p '$APP_G17_DIR' && rm -f '$APP_G16'" 
run-as "$PACKAGE" sh -c "cat > '$APP_G16'" < "$G16"
run-as "$PACKAGE" sh -c "test -s '$APP_G16'"

RESULT=$(am instrument -w -r \
  -e class "$TEST_CLASS" \
  -e gate16_path "/data/user/0/$PACKAGE/$APP_G16" \
  -e source_commit "$HEAD" \
  "$INSTRUMENTATION" 2>&1) || {
    printf '%s\n' "$RESULT"
    echo 'G17_LIVE_STATUS=FAIL'
    echo 'G17_LIVE_REASON=Android instrumentation execution failed'
    exit 1
}
printf '%s\n' "$RESULT"
printf '%s\n' "$RESULT" | grep -Fqx 'G17_LIVE_STATUS=PASS' || {
    echo 'G17_LIVE_STATUS=FAIL'
    echo 'G17_LIVE_REASON=instrumentation did not produce objective PASS marker'
    exit 1
}
printf '%s\n' "$RESULT" | grep -Fqx 'G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION' || {
    echo 'G17_LIVE_STATUS=FAIL'
    echo 'G17_LIVE_REASON=instrumentation did not prove real Android DUT execution'
    exit 1
}

G17_DEVICE='files/g17/gate17-runtime-execution.v1'
run-as "$PACKAGE" sh -c "test -s '$G17_DEVICE'"
DEVICE_ARTIFACT=$(run-as "$PACKAGE" sh -c "cat '$G17_DEVICE'")
printf '%s\n' "$DEVICE_ARTIFACT" > "$TMP/g17.device"
grep -Fqx 'gate_status=PASS' "$TMP/g17.device"
grep -Fqx 'execution_status=EXECUTED' "$TMP/g17.device"
grep -Fqx 'result_status=PASS' "$TMP/g17.device"
grep -Fqx 'command=pwd' "$TMP/g17.device"
grep -Fqx 'command_result=/root' "$TMP/g17.device"
grep -Fqx 'command_returncode=0' "$TMP/g17.device"
grep -Fq "source_commit=$HEAD" "$TMP/g17.device"
grep -Fq 'pipeline_run_id=' "$TMP/g17.device"
grep -Fq 'gate4_contract_sha256=' "$TMP/g17.device"
grep -Fq 'profile_sha256=' "$TMP/g17.device"
grep -Fq 'gate16_authorization_sha256=' "$TMP/g17.device"
grep -Fq 'guest_pid=' "$TMP/g17.device"
grep -Fq 'guest_proc_cwd=/root' "$TMP/g17.device"
grep -Fq 'android_package=com.alfa.device_ctrl' "$TMP/g17.device"
grep -Fq "android_uid=$ANDROID_UID" "$TMP/g17.device"

NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
printf '%s\n' 'G17_LIVE_STATUS=PASS' 'G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION' "G17_LIVE_PACKAGE=$PACKAGE" "G17_LIVE_UID=$ANDROID_UID" "G17_LIVE_APK=$APK_PATH" "G17_LIVE_INSTRUMENTATION=$INSTRUMENTATION" "G17_LIVE_ARTIFACT=$G17_DEVICE" "G17_LIVE_TIMESTAMP=$NOW"
