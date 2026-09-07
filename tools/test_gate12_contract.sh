#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
G12="$ROOT/tools/gate12_runtime_kernel.sh"
TMP="$ROOT/artifacts/.g12-test-tmp-$$"
RUN_ID="run_20990101_050505_1212"
RUN_DIR="$TMP/pipeline/$RUN_ID"
G11="$RUN_DIR/gate11/orchestrator.txt"
OUT="$RUN_DIR/gate12/kernel.txt"
cleanup(){ rm -rf "$TMP"; }; trap cleanup EXIT HUP INT TERM
rm -rf "$TMP"; mkdir -p "$(dirname "$G11")"
HEAD=$(git -C "$ROOT" rev-parse HEAD)
cat > "$G11" <<EOF
schema_version=gate11-runtime-orchestrator.v1
gate=gate11
gate_status=PASS
orchestrator_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$(printf '%s' g4 | sha256sum | awk '{print $1}')
profile_sha256=$(printf '%s' profile | sha256sum | awk '{print $1}')
orchestrator_id=orchestrator-$RUN_ID
execution_status=DEFERRED
EOF
printf '%s\n' '=== G12 STATIC BOUNDARY CHECK ==='
if grep -q 'runtime_stage.sh' "$G12" "$ROOT/tools/runtime_kernel.sh" || grep -q 'run_20260823_171233_30513' "$G12" "$ROOT/tools/runtime_kernel.sh"; then echo 'NO_G12_PROPAGATION=FAIL'; exit 1; fi
echo 'NO_G12_PROPAGATION=PASS'
printf '%s\n' '=== G12 POSITIVE ==='
if bash "$G12" "$RUN_ID" "$G11" "$OUT" >/dev/null 2>&1; then echo 'POSITIVE_VALID_ORCHESTRATOR=PASS'; else echo 'POSITIVE_VALID_ORCHESTRATOR=FAIL'; exit 1; fi
for x in 'schema_version=gate12-runtime-kernel.v1' 'gate=gate12' 'gate_status=PASS' 'kernel_status=PASS' "pipeline_run_id=$RUN_ID" "source_commit=$HEAD" "orchestrator_id=orchestrator-$RUN_ID" "kernel_id=kernel-$RUN_ID" 'execution_status=DEFERRED' 'execution_authority=G17'; do grep -Fqx "$x" "$OUT" || { echo "ARTIFACT_FIELD_FAIL=$x"; exit 1; }; done
echo 'ARTIFACT_SCHEMA=PASS'
cp "$G11" "$TMP/wrong"; sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_20990101_050506_1212/' "$TMP/wrong"; if bash "$G12" "$RUN_ID" "$TMP/wrong" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_WRONG_RUN=FAIL'; exit 1; fi; echo 'NEGATIVE_WRONG_RUN=PASS'
cp "$G11" "$TMP/stale"; sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$TMP/stale"; if bash "$G12" "$RUN_ID" "$TMP/stale" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_STALE_SOURCE=FAIL'; exit 1; fi; echo 'NEGATIVE_STALE_SOURCE=PASS'
cp "$G11" "$TMP/not-pass"; sed -i 's/^orchestrator_status=.*/orchestrator_status=BLOCKED/' "$TMP/not-pass"; if bash "$G12" "$RUN_ID" "$TMP/not-pass" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_ORCHESTRATOR_NOT_PASS=FAIL'; exit 1; fi; echo 'NEGATIVE_ORCHESTRATOR_NOT_PASS=PASS'
cp "$G11" "$TMP/bad-id"; sed -i 's/^orchestrator_id=.*/orchestrator_id=orchestrator-wrong/' "$TMP/bad-id"; if bash "$G12" "$RUN_ID" "$TMP/bad-id" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_ORCHESTRATOR_ID=FAIL'; exit 1; fi; echo 'NEGATIVE_ORCHESTRATOR_ID=PASS'
cp "$G11" "$TMP/executed"; sed -i 's/^execution_status=.*/execution_status=EXECUTED/' "$TMP/executed"; if bash "$G12" "$RUN_ID" "$TMP/executed" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_EXECUTION_IN_G12=FAIL'; exit 1; fi; echo 'NEGATIVE_EXECUTION_IN_G12=PASS'
printf '%s\n' '=== G12 PROTECTION CHECK ==='
for p in tools/gate1_foundation_v2.sh tools/gate2_canonical_source_build_boundary.sh tools/execution_capability.sh tools/gate3_execution_capability.sh tools/environment_contract.sh tools/gate5_common.sh tools/runtime_bootstrap.sh tools/gate7_runtime_session.sh tools/gate8_runtime_task.sh tools/gate9_runtime_action.sh tools/gate10_runtime_workflow.sh tools/gate11_runtime_orchestrator.sh; do test -f "$ROOT/$p" || { echo "PROTECTED_FILE_MISSING=$p"; exit 1; }; done
echo 'G1_G11_PROTECTION=PASS'
echo 'G12_CONTRACT_TEST=PASS'
