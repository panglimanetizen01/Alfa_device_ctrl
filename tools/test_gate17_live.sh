#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
PACKAGE=${ALFA_ANDROID_PACKAGE:-com.alfa.device_ctrl}
EXPECTED_APK_SHA256=${ALFA_EXPECTED_APK_SHA256:-}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/alfa-g17-live.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

# Build only the authorization/provenance chain on the host. G17 itself is never
# executed here; the Android instrumentation test is the sole live executor.
[ -n "$EXPECTED_APK_SHA256" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=exact APK artifact SHA256 is required'; exit 1; }
printf '%s' "$EXPECTED_APK_SHA256" | grep -Eq '^[0-9a-fA-F]{64}$' || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=invalid expected APK SHA256'; exit 1; }
PIPE_OUT="$TMP/pipeline.out"
if ! bash "$ROOT/tools/runtime_pipeline.sh" >"$PIPE_OUT" 2>&1; then
    cat "$PIPE_OUT"
    echo 'G17_LIVE_STATUS=BLOCKED'
    echo 'G17_LIVE_REASON=G4 pipeline did not complete'
    exit 1
fi
RUN_ID=$(awk -F= '/^pipeline_run_id=/{v=$2} END{print v}' "$PIPE_OUT")
HEAD=$(git -C "$ROOT" rev-parse HEAD)
[ -n "$RUN_ID" ] || { cat "$PIPE_OUT"; echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=pipeline run id missing'; exit 1; }
PIPE_SOURCE=$(awk -F= '/^source_commit=/{v=$2} END{print v}' "$PIPE_OUT")
[ "$PIPE_SOURCE" = "$HEAD" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=pipeline source is stale'; exit 1; }
G4_ART="$ROOT/artifacts/pipeline/$RUN_ID/gate4/environment_contract.txt"
PROFILE_REL=$(awk -F= '/^PROFILE_PATH=/{v=$2} END{print v}' "$PIPE_OUT")
PROFILE_ART="$ROOT/$PROFILE_REL"
[ -s "$G4_ART" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=G4 artifact missing'; exit 1; }
[ -s "$PROFILE_ART" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=profile artifact missing'; exit 1; }
G4_SHA=$(sha256sum "$G4_ART" | awk '{print $1}')
PROFILE_SHA=$(sha256sum "$PROFILE_ART" | awk '{print $1}')

G11="$TMP/g11.kernel-input"; G12="$TMP/g12.kernel"; G13="$TMP/g13.request"; G14="$TMP/g14.validation"; G15="$TMP/g15.policy"; G5="$TMP/g5.authorization"; G16="$TMP/g16.authorization"
cat > "$G11" <<EOF
schema_version=gate11-runtime-orchestrator.v1
gate=gate11
gate_status=PASS
orchestrator_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_SHA
profile_sha256=$PROFILE_SHA
orchestrator_id=orchestrator-$RUN_ID
execution_status=DEFERRED
EOF
bash "$ROOT/tools/gate12_runtime_kernel.sh" "$RUN_ID" "$G11" "$G12" >/dev/null
bash "$ROOT/tools/gate13_runtime_command_request.sh" "$RUN_ID" "$G12" "$G13" >/dev/null
bash "$ROOT/tools/gate14_runtime_command_validation.sh" "$RUN_ID" "$G13" "$G14" >/dev/null
bash "$ROOT/tools/gate15_runtime_command_policy.sh" "$RUN_ID" "$G14" "$G15" >/dev/null

DECISION_ID="decision-$RUN_ID"
cat > "$G5" <<EOF
schema_version=gate5-authorization.v1
authorization_id=authorization-$RUN_ID
request_id=pwd-request-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_SHA
profile_sha256=$PROFILE_SHA
policy_version=gate15-policy-v1
decision_id=$DECISION_ID
authorization_status=AUTHORIZED
EOF
bash "$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$G15" "$G5" "$G16" >/dev/null

ANDROID_UID=$(cmd package list packages -U "$PACKAGE" 2>/dev/null | awk -v p="$PACKAGE" '$1=="package:"p {print $3;exit}' || true)
[ -n "$ANDROID_UID" ] || ANDROID_UID=$(dumpsys package "$PACKAGE" 2>/dev/null | sed -n 's/.*userId=\([0-9][0-9]*\).*/\1/p' | head -n1 || true)
[ -n "$ANDROID_UID" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android package identity could not be proven'; exit 1; }
APK_PATH=$(pm path "$PACKAGE" 2>/dev/null | sed -n '1s/^package://p')
[ -n "$APK_PATH" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android installed APK identity could not be proven'; exit 1; }
INSTRUMENTATION=$(pm list instrumentation 2>/dev/null | awk -v p="$PACKAGE" '$0 ~ "target="p {line=$0; sub(/^instrumentation:/,"",line); sub(/ \(target=.*/,"",line); print line; exit}')
[ -n "$INSTRUMENTATION" ] || { echo 'G17_LIVE_STATUS=BLOCKED'; echo 'G17_LIVE_REASON=Android instrumentation targeting package not installed'; exit 1; }

APP_G17_DIR='files/g17'; APP_G16="$APP_G17_DIR/gate16.authorization"
run-as "$PACKAGE" sh -c "mkdir -p '$APP_G17_DIR' && rm -f '$APP_G16'"
run-as "$PACKAGE" sh -c "cat > '$APP_G16'" < "$G16"
run-as "$PACKAGE" sh -c "test -s '$APP_G16'"
TEST_CLASS='com.alfa.device_ctrl.G17AndroidRuntimeExecutionTest#exactGate16AuthorizationExecutesPwdInsidePackagedRuntime'
RESULT=$(am instrument -w -r -e class "$TEST_CLASS" -e gate16_path "/data/user/0/$PACKAGE/$APP_G16" -e source_commit "$HEAD" -e expected_apk_sha256 "$EXPECTED_APK_SHA256" "$INSTRUMENTATION" 2>&1) || {
    printf '%s\n' "$RESULT"
    echo 'G17_LIVE_STATUS=FAIL'
    echo 'G17_LIVE_REASON=Android instrumentation execution failed'
    exit 1
}
printf '%s\n' "$RESULT"
printf '%s\n' "$RESULT" | grep -Fqx 'G17_LIVE_STATUS=PASS' || { echo 'G17_LIVE_STATUS=FAIL'; echo 'G17_LIVE_REASON=instrumentation did not produce objective PASS marker'; exit 1; }
printf '%s\n' "$RESULT" | grep -Fqx 'G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION' || { echo 'G17_LIVE_STATUS=FAIL'; echo 'G17_LIVE_REASON=instrumentation did not prove real Android DUT execution'; exit 1; }
printf '%s\n' "$RESULT" | grep -Fqx "G17_LIVE_APK_SHA256=$(printf '%s' "$EXPECTED_APK_SHA256" | tr '[:upper:]' '[:lower:]')" || { echo 'G17_LIVE_STATUS=FAIL'; echo 'G17_LIVE_REASON=instrumentation did not echo exact APK digest'; exit 1; }

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
grep -Fq "pipeline_run_id=$RUN_ID" "$TMP/g17.device"
grep -Fq "gate4_contract_sha256=$G4_SHA" "$TMP/g17.device"
grep -Fq "profile_sha256=$PROFILE_SHA" "$TMP/g17.device"
grep -Fq 'gate16_authorization_sha256=' "$TMP/g17.device"
grep -Fq 'guest_pid=' "$TMP/g17.device"
grep -Fq 'guest_proc_cwd=/root' "$TMP/g17.device"
grep -Fq 'android_package=com.alfa.device_ctrl' "$TMP/g17.device"
grep -Fq "android_uid=$ANDROID_UID" "$TMP/g17.device"
grep -Fq "installed_apk_sha256=$(printf '%s' "$EXPECTED_APK_SHA256" | tr '[:upper:]' '[:lower:]')" "$TMP/g17.device"
grep -Fq "expected_apk_sha256=$(printf '%s' "$EXPECTED_APK_SHA256" | tr '[:upper:]' '[:lower:]')" "$TMP/g17.device"
NOW=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
printf '%s\n' 'G17_LIVE_STATUS=PASS' 'G17_LIVE_RESULT=REAL_ANDROID_DUT_EXECUTION' "G17_LIVE_PACKAGE=$PACKAGE" "G17_LIVE_UID=$ANDROID_UID" "G17_LIVE_APK=$APK_PATH" "G17_LIVE_APK_SHA256=$(printf '%s' "$EXPECTED_APK_SHA256" | tr '[:upper:]' '[:lower:]')" "G17_LIVE_INSTRUMENTATION=$INSTRUMENTATION" "G17_LIVE_ARTIFACT=$G17_DEVICE" "G17_LIVE_TIMESTAMP=$NOW"
