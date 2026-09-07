#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
G10="$ROOT/tools/gate10_runtime_workflow.sh"
TMP_ROOT="$ROOT/artifacts/.g10-test-tmp-$$"
RUN_ID="run_20990101_030303_1010"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G9="$RUN_DIR/gate9/action.txt"
OUT="$RUN_DIR/gate10/workflow.txt"

cleanup() { rm -rf "$TMP_ROOT" "$RUN_DIR"; }
trap cleanup EXIT HUP INT TERM
rm -rf "$TMP_ROOT" "$RUN_DIR"
mkdir -p "$RUN_DIR/gate4" "$RUN_DIR/gate9" "$TMP_ROOT"
HEAD=$(git -C "$ROOT" rev-parse HEAD)
PROFILE_SHA=$(printf '%s' 'g10-test-profile' | sha256sum | awk '{print $1}')
cat > "$G4" <<EOF
schema_version=environment-contract.v1
pipeline_run_id=$RUN_ID
source_commit=$HEAD
profile_sha256=$PROFILE_SHA
contract_result=VALID
project_id=alfa-device-ctrl
EOF
G4_SHA=$(sha256sum "$G4" | awk '{print $1}')
cat > "$G9" <<EOF
schema_version=gate9-runtime-action.v1
gate=gate9
gate_status=PASS
action_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_SHA
profile_sha256=$PROFILE_SHA
gate8_artifact=/private/evidence/g8-task.txt
task_id=task-$RUN_ID
action_id=action-$RUN_ID
action_name=runtime_self_action
action_type=RUNTIME_ACTION
execution_status=DEFERRED
execution_authority=G17
created_at=2099-01-01T03:03:03Z
execution_path=/private/alfa
EOF

printf '%s\n' '=== G10 STATIC BOUNDARY CHECK ==='
if grep -q 'runtime_stage.sh' "$G10" "$ROOT/tools/runtime_workflow.sh" || grep -q 'run_20260823_171233_30513' "$G10" "$ROOT/tools/runtime_workflow.sh"; then
    echo 'NO_G10_PROPAGATION=FAIL'; exit 1
fi
echo 'NO_G10_PROPAGATION=PASS'

printf '%s\n' '=== G10 POSITIVE ==='
if bash "$G10" "$RUN_ID" "$G9" "$OUT" >/dev/null 2>&1 && grep -q '^G10_STATUS=PASS$' <(bash "$G10" "$RUN_ID" "$G9" "$OUT" 2>/dev/null); then
    echo 'POSITIVE_VALID_ACTION=PASS'
else
    echo 'POSITIVE_VALID_ACTION=FAIL'; exit 1
fi

printf '%s\n' '=== G10 ARTIFACT CHECK ==='
for expected in \
    'schema_version=gate10-runtime-workflow.v1' \
    'gate=gate10' \
    'gate_status=PASS' \
    'workflow_status=PASS' \
    "pipeline_run_id=$RUN_ID" \
    "source_commit=$HEAD" \
    "action_id=action-$RUN_ID" \
    "workflow_id=workflow-$RUN_ID" \
    'workflow_name=runtime_self_workflow' \
    'workflow_type=RUNTIME_WORKFLOW' \
    'execution_status=DEFERRED' \
    'execution_authority=G17'; do
    grep -Fqx "$expected" "$OUT" || { echo "ARTIFACT_FIELD_FAIL=$expected"; exit 1; }
done
echo 'ARTIFACT_SCHEMA=PASS'

printf '%s\n' '=== G10 NEGATIVE PROVENANCE ==='
cp "$G9" "$TMP_ROOT/wrong-run.txt"
sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_20990101_030304_1010/' "$TMP_ROOT/wrong-run.txt"
if bash "$G10" "$RUN_ID" "$TMP_ROOT/wrong-run.txt" "$TMP_ROOT/wrong-run-out.txt" >/dev/null 2>&1; then echo 'NEGATIVE_WRONG_RUN=FAIL'; exit 1; fi
echo 'NEGATIVE_WRONG_RUN=PASS'

cp "$G9" "$TMP_ROOT/stale.txt"
sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$TMP_ROOT/stale.txt"
if bash "$G10" "$RUN_ID" "$TMP_ROOT/stale.txt" "$TMP_ROOT/stale-out.txt" >/dev/null 2>&1; then echo 'NEGATIVE_STALE_SOURCE=FAIL'; exit 1; fi
echo 'NEGATIVE_STALE_SOURCE=PASS'

cp "$G9" "$TMP_ROOT/not-ready.txt"
sed -i 's/^action_status=.*/action_status=BLOCKED/' "$TMP_ROOT/not-ready.txt"
if bash "$G10" "$RUN_ID" "$TMP_ROOT/not-ready.txt" "$TMP_ROOT/not-ready-out.txt" >/dev/null 2>&1; then echo 'NEGATIVE_ACTION_NOT_PASS=FAIL'; exit 1; fi
echo 'NEGATIVE_ACTION_NOT_PASS=PASS'

cp "$G9" "$TMP_ROOT/bad-id.txt"
sed -i 's/^action_id=.*/action_id=action-wrong/' "$TMP_ROOT/bad-id.txt"
if bash "$G10" "$RUN_ID" "$TMP_ROOT/bad-id.txt" "$TMP_ROOT/bad-id-out.txt" >/dev/null 2>&1; then echo 'NEGATIVE_ACTION_ID=FAIL'; exit 1; fi
echo 'NEGATIVE_ACTION_ID=PASS'

cp "$G9" "$TMP_ROOT/executing.txt"
sed -i 's/^execution_status=.*/execution_status=EXECUTED/' "$TMP_ROOT/executing.txt"
if bash "$G10" "$RUN_ID" "$TMP_ROOT/executing.txt" "$TMP_ROOT/executing-out.txt" >/dev/null 2>&1; then echo 'NEGATIVE_EXECUTION_IN_G10=FAIL'; exit 1; fi
echo 'NEGATIVE_EXECUTION_IN_G10=PASS'

printf '%s\n' '=== G10 PROTECTION CHECK ==='
for path in \
    tools/gate1_foundation_v2.sh \
    tools/gate2_canonical_source_build_boundary.sh \
    tools/execution_capability.sh \
    tools/gate3_execution_capability.sh \
    tools/environment_contract.sh \
    tools/gate5_common.sh \
    tools/runtime_bootstrap.sh \
    tools/gate7_runtime_session.sh \
    tools/gate8_runtime_task.sh \
    tools/gate9_runtime_action.sh; do
    test -f "$ROOT/$path" || { echo "PROTECTED_FILE_MISSING=$path"; exit 1; }
done
echo 'G1_G9_PROTECTION=PASS'
echo 'G10_CONTRACT_TEST=PASS'
