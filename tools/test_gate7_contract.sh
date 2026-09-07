#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
VALIDATOR="$ROOT/tools/gate7_runtime_session.sh"
TMP="$ROOT/artifacts/.g7-test-tmp-$$"
RUN_ID="run_g7_test_$$"
EVIDENCE="$TMP/session.properties"
BOOTSTRAP="$TMP/bootstrap.txt"

cleanup() { rm -rf "$TMP"; }
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

cat > "$EVIDENCE" <<EOF
schema_version=operation-evidence.v1
session_id=session-g7-test
request_id=request-g7-test
pipeline_run_id=$RUN_ID
runtime_id=ubuntu
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

expect_green() {
    if ! bash "$VALIDATOR" "$RUN_ID" "$BOOTSTRAP" "$EVIDENCE" >/dev/null 2>&1; then
        return 1
    fi
}

expect_blocked() {
    if bash "$VALIDATOR" "$RUN_ID" "$BOOTSTRAP" "$EVIDENCE" >/dev/null 2>&1; then
        return 1
    fi
}

printf '%s\n' '=== G7 CONTRACT TEST ==='
if expect_green; then printf '%s\n' 'POSITIVE_VALID_SESSION=PASS'; else printf '%s\n' 'POSITIVE_VALID_SESSION=FAIL'; exit 1; fi

sed 's/^pipeline_run_id=.*/pipeline_run_id=wrong-run/' "$EVIDENCE" > "$EVIDENCE.bad"
mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_WRONG_RUN=PASS'; else printf '%s\n' 'NEGATIVE_WRONG_RUN=FAIL'; exit 1; fi

sed 's/^pipeline_run_id=.*/pipeline_run_id='"$RUN_ID"'/' "$EVIDENCE" | sed 's/^process_pid=.*/process_pid=0/' > "$EVIDENCE.bad"
mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_INVALID_PID=PASS'; else printf '%s\n' 'NEGATIVE_INVALID_PID=FAIL'; exit 1; fi

sed 's/^process_pid=.*/process_pid=4242/' "$EVIDENCE" | sed 's/^state=.*/state=PTY_CREATED/' > "$EVIDENCE.bad"
mv "$EVIDENCE.bad" "$EVIDENCE"
if expect_blocked; then printf '%s\n' 'NEGATIVE_PROMPT_NOT_READY=PASS'; else printf '%s\n' 'NEGATIVE_PROMPT_NOT_READY=FAIL'; exit 1; fi

printf '%s\n' 'G7_CONTRACT_TEST=PASS'
