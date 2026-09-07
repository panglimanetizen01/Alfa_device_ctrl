#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
G9="$ROOT/tools/gate9_runtime_action.sh"
TMP_ROOT="$ROOT/artifacts/.g9-test-tmp-$$"
RUN_ID="run_20990101_020202_9090"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G8="$RUN_DIR/gate8/task.txt"
OUT="$RUN_DIR/gate9/action.txt"

cleanup() { rm -rf "$TMP_ROOT" "$RUN_DIR"; }
trap cleanup EXIT HUP INT TERM

rm -rf "$TMP_ROOT" "$RUN_DIR"
mkdir -p "$RUN_DIR/gate4" "$RUN_DIR/gate8" "$TMP_ROOT"

HEAD=$(git -C "$ROOT" rev-parse HEAD)
PROFILE_SHA=$(printf '%s' 'g9-test-profile' | sha256sum | awk '{print $1}')

cat > "$G4" <<EOF
schema_version=environment-contract.v1
pipeline_run_id=$RUN_ID
source_commit=$HEAD
profile_sha256=$PROFILE_SHA
contract_result=VALID
project_id=alfa-device-ctrl
EOF
G4_SHA=$(sha256sum "$G4" | awk '{print $1}')
cat > "$G8" <<EOF
schema_version=gate8-runtime-task.v1
gate=gate8
gate_status=PASS
task_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_SHA
profile_sha256=$PROFILE_SHA
gate7_artifact=/private/evidence/g7-session.txt
session_id=session-g9-positive
request_id=request-g9-positive
runtime_id=ubuntu
task_id=task-$RUN_ID
task_name=runtime_self_test
task_type=SESSION_TASK
execution_status=DEFERRED
execution_authority=G17
created_at=2099-01-01T02:02:02Z
execution_path=/private/alfa
EOF

printf '%s\n' '=== G9 STATIC BOUNDARY CHECK ==='
if grep -q 'runtime_stage.sh' "$G9" || grep -q 'run_20260823_171233_30513' "$G9" "$ROOT/tools/runtime_action.sh"; then
    echo 'NO_G9_PROPAGATION=FAIL'
    exit 1
fi
echo 'NO_G9_PROPAGATION=PASS'

printf '%s\n' '=== G9 POSITIVE ==='
if bash "$G9" "$RUN_ID" "$G8" "$OUT" >/dev/null && grep -q '^G9_STATUS=PASS$' <(bash "$G9" "$RUN_ID" "$G8" "$OUT") 2>/dev/null; then
    echo 'POSITIVE_VALID_TASK=PASS'
else
    echo 'POSITIVE_VALID_TASK=FAIL'
    exit 1
fi

printf '%s\n' '=== G9 ARTIFACT CHECK ==='
for expected in \
    'schema_version=gate9-runtime-action.v1' \
    'gate=gate9' \
    'gate_status=PASS' \
    'action_status=PASS' \
    'task_status=PASS' \
    "pipeline_run_id=$RUN_ID" \
    "source_commit=$HEAD" \
    "task_id=task-$RUN_ID" \
    'action_name=runtime_self_action' \
    'action_type=RUNTIME_ACTION' \
    'execution_status=DEFERRED' \
    'execution_authority=G17'; do
    grep -Fqx "$expected" "$OUT" || { echo "ARTIFACT_FIELD_FAIL=$expected"; exit 1; }
done
echo 'ARTIFACT_SCHEMA=PASS'

printf '%s\n' '=== G9 NEGATIVE PROVENANCE ==='
cp "$G8" "$TMP_ROOT/wrong-run.txt"
sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_20990101_020203_9090/' "$TMP_ROOT/wrong-run.txt"
if bash "$G9" "$RUN_ID" "$TMP_ROOT/wrong-run.txt" "$TMP_ROOT/wrong-run-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_WRONG_RUN=FAIL'
    exit 1
fi
echo 'NEGATIVE_WRONG_RUN=PASS'

cp "$G8" "$TMP_ROOT/stale.txt"
sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$TMP_ROOT/stale.txt"
if bash "$G9" "$RUN_ID" "$TMP_ROOT/stale.txt" "$TMP_ROOT/stale-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_STALE_SOURCE=FAIL'
    exit 1
fi
echo 'NEGATIVE_STALE_SOURCE=PASS'

cp "$G8" "$TMP_ROOT/not-ready.txt"
sed -i 's/^task_status=.*/task_status=BLOCKED/' "$TMP_ROOT/not-ready.txt"
if bash "$G9" "$RUN_ID" "$TMP_ROOT/not-ready.txt" "$TMP_ROOT/not-ready-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_TASK_NOT_PASS=FAIL'
    exit 1
fi
echo 'NEGATIVE_TASK_NOT_PASS=PASS'

cp "$G8" "$TMP_ROOT/executing.txt"
sed -i 's/^execution_status=.*/execution_status=EXECUTED/' "$TMP_ROOT/executing.txt"
if bash "$G9" "$RUN_ID" "$TMP_ROOT/executing.txt" "$TMP_ROOT/executing-out.txt" >/dev/null 2>&1; then
    echo 'NEGATIVE_EXECUTION_IN_G9=FAIL'
    exit 1
fi
echo 'NEGATIVE_EXECUTION_IN_G9=PASS'

printf '%s\n' '=== G9 PROTECTION CHECK ==='
for path in \
    tools/gate1_foundation_v2.sh \
    tools/gate2_canonical_source_build_boundary.sh \
    tools/execution_capability.sh \
    tools/gate3_execution_capability.sh \
    tools/environment_contract.sh \
    tools/gate5_common.sh \
    tools/runtime_bootstrap.sh \
    tools/gate7_runtime_session.sh \
    tools/gate8_runtime_task.sh; do
    test -f "$ROOT/$path" || { echo "PROTECTED_FILE_MISSING=$path"; exit 1; }
done
echo 'G1_G8_PROTECTION=PASS'

echo 'G9_CONTRACT_TEST=PASS'
