#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
G8="$ROOT/tools/gate8_runtime_task.sh"
TMP_ROOT="$ROOT/artifacts/.g8-test-tmp-$$"
RUN_ID="run_20990101_010101_8080"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G7="$RUN_DIR/gate7/session.txt"
OUT="$RUN_DIR/gate8/task.txt"

cleanup() { rm -rf "$TMP_ROOT" "$RUN_DIR"; }
trap cleanup EXIT HUP INT TERM

rm -rf "$TMP_ROOT" "$RUN_DIR"
mkdir -p "$RUN_DIR/gate4" "$RUN_DIR/gate7" "$TMP_ROOT"

HEAD=$(git -C "$ROOT" rev-parse HEAD)
PROFILE_SHA=$(printf '%s' 'g8-test-profile' | sha256sum | awk '{print $1}')

cat > "$G4" <<EOF
schema_version=environment-contract.v1
pipeline_run_id=$RUN_ID
source_commit=$HEAD
profile_sha256=$PROFILE_SHA
contract_result=VALID
project_id=alfa-device-ctrl
EOF
G4_SHA=$(sha256sum "$G4" | awk '{print $1}')
cat > "$G7" <<EOF
schema_version=gate7-runtime-session.v1
gate=gate7
gate_status=PASS
session_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_SHA
profile_sha256=$PROFILE_SHA
session_id=session-g8-positive
request_id=request-g8-positive
runtime_id=ubuntu
process_pid=4242
pty_status=PASS
prompt_observed=PASS
engine_path=/private/runtime/libproot.so
rootfs_path=/private/runtime/rootfs
runtime_evidence=/private/runtime/READY.evidence
source_evidence=/private/evidence/session.properties
EOF

printf '%s\n' '=== G8 STATIC BOUNDARY CHECK ==='
if grep -q 'runtime_stage.sh' "$G8" || grep -q 'run_20260823_171233_30513' "$G8" "$ROOT/tools/runtime_task.sh"; then
    echo 'NO_G8_PROPAGATION=FAIL'
    exit 1
fi
echo 'NO_G8_PROPAGATION=PASS'

printf '%s\n' '=== G8 POSITIVE ==='
if bash "$G8" "$RUN_ID" "$G7" "$OUT" >/dev/null && grep -q '^G8_STATUS=PASS$' <(bash "$G8" "$RUN_ID" "$G7" "$OUT") 2>/dev/null; then
    echo 'POSITIVE_VALID_SESSION=PASS'
else
    echo 'POSITIVE_VALID_SESSION=FAIL'
    exit 1
fi

echo '=== G8 ARTIFACT CHECK ==='
for expected in \
    'schema_version=gate8-runtime-task.v1' \
    'gate=gate8' \
    'gate_status=PASS' \
    'task_status=PASS' \
    "pipeline_run_id=$RUN_ID" \
    "source_commit=$HEAD" \
    'task_name=runtime_self_test' \
    'task_type=SESSION_TASK' \
    'execution_status=DEFERRED' \
    'execution_authority=G17'; do
    grep -Fqx "$expected" "$OUT" || { echo "ARTIFACT_FIELD_FAIL=$expected"; exit 1; }
done
echo 'ARTIFACT_SCHEMA=PASS'

printf '%s\n' '=== G8 NEGATIVE PROVENANCE ==='
cp "$G7" "$TMP_ROOT/wrong-run.txt"
sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_20990101_010102_8080/' "$TMP_ROOT/wrong-run.txt"
if bash "$G8" "$RUN_ID" "$TMP_ROOT/wrong-run.txt" "$TMP_ROOT/wrong-run-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_WRONG_RUN=FAIL'
    exit 1
fi
echo 'NEGATIVE_WRONG_RUN=PASS'

cp "$G7" "$TMP_ROOT/stale.txt"
sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$TMP_ROOT/stale.txt"
if bash "$G8" "$RUN_ID" "$TMP_ROOT/stale.txt" "$TMP_ROOT/stale-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_STALE_SOURCE=FAIL'
    exit 1
fi
echo 'NEGATIVE_STALE_SOURCE=PASS'

cp "$G7" "$TMP_ROOT/not-ready.txt"
sed -i 's/^session_status=.*/session_status=BLOCKED/' "$TMP_ROOT/not-ready.txt"
if bash "$G8" "$RUN_ID" "$TMP_ROOT/not-ready.txt" "$TMP_ROOT/not-ready-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_SESSION_NOT_PASS=FAIL'
    exit 1
fi
echo 'NEGATIVE_SESSION_NOT_PASS=PASS'

printf '%s\n' '=== G8 PROTECTION CHECK ==='
for path in \
    tools/gate1_foundation_v2.sh \
    tools/gate2_canonical_source_build_boundary.sh \
    tools/execution_capability.sh \
    tools/gate3_execution_capability.sh \
    tools/environment_contract.sh \
    tools/gate5_common.sh \
    tools/runtime_bootstrap.sh \
    tools/gate7_runtime_session.sh; do
    test -f "$ROOT/$path" || { echo "PROTECTED_FILE_MISSING=$path"; exit 1; }
done
echo 'G1_G7_PROTECTION=PASS'

echo 'G8_CONTRACT_TEST=PASS'
