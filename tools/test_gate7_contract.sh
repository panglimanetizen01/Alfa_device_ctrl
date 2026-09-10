#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
VALIDATOR="$ROOT/tools/gate7_runtime_session.sh"
TMP="$ROOT/artifacts/.g7-test-tmp-$$"
RUN_ID="run_g7_test_$$"
EVIDENCE="$TMP/session.properties"
BOOTSTRAP="$TMP/bootstrap.txt"
cleanup(){ rm -rf "$TMP"; }
trap cleanup EXIT HUP INT TERM
mkdir -p "$TMP"
SOURCE_COMMIT=$(git -C "$ROOT" rev-parse HEAD)
GATE4_SHA=$(printf '%s' 'gate4-test-contract' | sha256sum | awk '{print $1}')
PROFILE_SHA=$(printf '%s' 'profile-test' | sha256sum | awk '{print $1}')
IMPLEMENTATION_COMMIT="$SOURCE_COMMIT"
cat > "$BOOTSTRAP" <<EOF
schema_version=gate6-bootstrap.v1
gate=gate6
gate_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$SOURCE_COMMIT
implementation_commit=$IMPLEMENTATION_COMMIT
gate4_contract_sha256=$GATE4_SHA
profile_sha256=$PROFILE_SHA
decision_id=decision-g7-test
request_id=request-g7-test
authorization_status=AUTHORIZED
bootstrap_status=PASS
bootstrap_probe=PASS
EOF
write_evidence(){ cat > "$EVIDENCE" <<EOF
schema_version=operation-evidence.v1
session_id=session-g7-test
request_id=request-g7-test
pipeline_run_id=$RUN_ID
runtime_id=$1
source_commit=$SOURCE_COMMIT
gate4_contract_sha256=$GATE4_SHA
profile_sha256=$PROFILE_SHA
implementation_commit=$IMPLEMENTATION_COMMIT
state=READY
result=PROMPT_OBSERVED
process_pid=4242
pty_status=PASS
prompt_observed=PASS
engine_path=/app/lib/libproot.so
rootfs_path=/app/runtime/rootfs
runtime_evidence=/app/runtime/READY.evidence
EOF
}
write_evidence ubuntu
expect_green(){ bash "$VALIDATOR" "$RUN_ID" "$BOOTSTRAP" "$EVIDENCE" >/dev/null 2>&1; }
expect_blocked(){ ! bash "$VALIDATOR" "$RUN_ID" "$BOOTSTRAP" "$EVIDENCE" >/dev/null 2>&1; }
printf '%s\n' '=== G7 STATIC BOUNDARY CHECK ==='
if grep -q 'runtime_stage.sh' "$ROOT/tools/runtime_session.sh"; then printf '%s\n' 'NO_G7_PROPAGATION=FAIL'; exit 1; fi
if ! grep -q 'ReleaseStringUTFChars(env, cwd, cmd_cwd)' "$ROOT/terminal-emulator/src/main/jni/termux.c"; then printf '%s\n' 'JNI_CWD_RELEASE=FAIL'; exit 1; fi
if grep -q 'ReleaseStringUTFChars(env, cmd, cmd_cwd)' "$ROOT/terminal-emulator/src/main/jni/termux.c"; then printf '%s\n' 'JNI_WRONG_CWD_RELEASE=FAIL'; exit 1; fi
if ! grep -q 'gate7-launch.properties' "$ROOT/app/src/main/java/com/alfa/device_ctrl/InteractiveSessionContract.java"; then printf '%s\n' 'ANDROID_LAUNCH_PROVENANCE=FAIL'; exit 1; fi
printf '%s\n' 'NO_G7_PROPAGATION=PASS' 'JNI_CWD_RELEASE=PASS' 'ANDROID_LAUNCH_PROVENANCE=PASS'
printf '%s\n' '=== G7 MULTI-DISTRO CONTRACT TEST ==='
for runtime in debian ubuntu alpine kali; do
  write_evidence "$runtime"
  if expect_green; then printf 'POSITIVE_RUNTIME_%s=PASS\n' "$(printf '%s' "$runtime" | tr '[:lower:]' '[:upper:]')"; else printf 'POSITIVE_RUNTIME_%s=FAIL\n' "$(printf '%s' "$runtime" | tr '[:lower:]' '[:upper:]')"; exit 1; fi
done
write_evidence debian
sed 's/^pipeline_run_id=.*/pipeline_run_id=wrong-run/' "$EVIDENCE" > "$EVIDENCE.bad"; mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_WRONG_RUN=PASS'; else printf '%s\n' 'NEGATIVE_WRONG_RUN=FAIL'; exit 1; fi
write_evidence debian
sed 's/^process_pid=.*/process_pid=0/' "$EVIDENCE" > "$EVIDENCE.bad"; mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_INVALID_PID=PASS'; else printf '%s\n' 'NEGATIVE_INVALID_PID=FAIL'; exit 1; fi
write_evidence debian
sed 's/^state=.*/state=PTY_CREATED/' "$EVIDENCE" > "$EVIDENCE.bad"; mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_PROMPT_NOT_READY=PASS'; else printf '%s\n' 'NEGATIVE_PROMPT_NOT_READY=FAIL'; exit 1; fi
write_evidence debian
sed 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$EVIDENCE" > "$EVIDENCE.bad"; mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_STALE_SOURCE=PASS'; else printf '%s\n' 'NEGATIVE_STALE_SOURCE=FAIL'; exit 1; fi
printf '%s\n' '=== G1-G6 PROTECTION DELEGATION ==='
printf '%s\n' 'G1_G6_PROTECTION=DELEGATED' 'G7_CONTRACT_TEST=PASS'
