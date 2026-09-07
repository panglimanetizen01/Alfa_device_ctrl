#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
G11="$ROOT/tools/gate11_runtime_orchestrator.sh"
TMP="$ROOT/artifacts/.g11-test-tmp-$$"
RUN_ID="run_20990101_040404_1111"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G10="$RUN_DIR/gate10/workflow.txt"
OUT="$RUN_DIR/gate11/orchestrator.txt"
cleanup(){ rm -rf "$TMP" "$RUN_DIR"; }; trap cleanup EXIT HUP INT TERM
rm -rf "$TMP" "$RUN_DIR"; mkdir -p "$TMP" "$RUN_DIR/gate4" "$RUN_DIR/gate10"
HEAD=$(git -C "$ROOT" rev-parse HEAD)
PROFILE_SHA=$(printf '%s' g11-test-profile | sha256sum | awk '{print $1}')
cat > "$G4" <<EOF
schema_version=environment-contract.v1
pipeline_run_id=$RUN_ID
source_commit=$HEAD
profile_sha256=$PROFILE_SHA
contract_result=VALID
project_id=alfa-device-ctrl
EOF
G4_SHA=$(sha256sum "$G4" | awk '{print $1}')
cat > "$G10" <<EOF
schema_version=gate10-runtime-workflow.v1
gate=gate10
gate_status=PASS
workflow_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_SHA
profile_sha256=$PROFILE_SHA
gate9_artifact=/private/evidence/g9-action.txt
action_id=action-$RUN_ID
workflow_id=workflow-$RUN_ID
workflow_name=runtime_self_workflow
workflow_type=RUNTIME_WORKFLOW
execution_status=DEFERRED
execution_authority=G17
created_at=2099-01-01T04:04:04Z
execution_path=/private/alfa
EOF
printf '%s\n' '=== G11 STATIC BOUNDARY CHECK ==='
if grep -q 'runtime_stage.sh' "$G11" "$ROOT/tools/runtime_orchestrator.sh" || grep -q 'run_20260823_171233_30513' "$G11" "$ROOT/tools/runtime_orchestrator.sh"; then echo 'NO_G11_PROPAGATION=FAIL'; exit 1; fi
echo 'NO_G11_PROPAGATION=PASS'
printf '%s\n' '=== G11 POSITIVE ==='
if bash "$G11" "$RUN_ID" "$G10" "$OUT" >/dev/null 2>&1; then echo 'POSITIVE_VALID_WORKFLOW=PASS'; else echo 'POSITIVE_VALID_WORKFLOW=FAIL'; exit 1; fi
printf '%s\n' '=== G11 ARTIFACT CHECK ==='
for x in 'schema_version=gate11-runtime-orchestrator.v1' 'gate=gate11' 'gate_status=PASS' 'orchestrator_status=PASS' "pipeline_run_id=$RUN_ID" "source_commit=$HEAD" "workflow_id=workflow-$RUN_ID" "orchestrator_id=orchestrator-$RUN_ID" 'orchestrator_name=runtime_self_orchestrator' 'orchestrator_type=RUNTIME_ORCHESTRATOR' 'execution_status=DEFERRED' 'execution_authority=G17'; do grep -Fqx "$x" "$OUT" || { echo "ARTIFACT_FIELD_FAIL=$x"; exit 1; }; done
echo 'ARTIFACT_SCHEMA=PASS'
printf '%s\n' '=== G11 NEGATIVE PROVENANCE ==='
cp "$G10" "$TMP/wrong-run"; sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_20990101_040405_1111/' "$TMP/wrong-run"; if bash "$G11" "$RUN_ID" "$TMP/wrong-run" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_WRONG_RUN=FAIL'; exit 1; fi; echo 'NEGATIVE_WRONG_RUN=PASS'
cp "$G10" "$TMP/stale"; sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$TMP/stale"; if bash "$G11" "$RUN_ID" "$TMP/stale" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_STALE_SOURCE=FAIL'; exit 1; fi; echo 'NEGATIVE_STALE_SOURCE=PASS'
cp "$G10" "$TMP/not-ready"; sed -i 's/^workflow_status=.*/workflow_status=BLOCKED/' "$TMP/not-ready"; if bash "$G11" "$RUN_ID" "$TMP/not-ready" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_WORKFLOW_NOT_PASS=FAIL'; exit 1; fi; echo 'NEGATIVE_WORKFLOW_NOT_PASS=PASS'
cp "$G10" "$TMP/bad-id"; sed -i 's/^workflow_id=.*/workflow_id=workflow-wrong/' "$TMP/bad-id"; if bash "$G11" "$RUN_ID" "$TMP/bad-id" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_WORKFLOW_ID=FAIL'; exit 1; fi; echo 'NEGATIVE_WORKFLOW_ID=PASS'
cp "$G10" "$TMP/executing"; sed -i 's/^execution_status=.*/execution_status=EXECUTED/' "$TMP/executing"; if bash "$G11" "$RUN_ID" "$TMP/executing" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_EXECUTION_IN_G11=FAIL'; exit 1; fi; echo 'NEGATIVE_EXECUTION_IN_G11=PASS'
printf '%s\n' '=== G11 PROTECTION CHECK ==='
for p in tools/gate1_foundation_v2.sh tools/gate2_canonical_source_build_boundary.sh tools/execution_capability.sh tools/gate3_execution_capability.sh tools/environment_contract.sh tools/gate5_common.sh tools/runtime_bootstrap.sh tools/gate7_runtime_session.sh tools/gate8_runtime_task.sh tools/gate9_runtime_action.sh tools/gate10_runtime_workflow.sh; do test -f "$ROOT/$p" || { echo "PROTECTED_FILE_MISSING=$p"; exit 1; }; done
echo 'G1_G10_PROTECTION=PASS'
echo 'G11_CONTRACT_TEST=PASS'
