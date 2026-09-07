#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
G13="$ROOT/tools/gate13_runtime_command_request.sh"
TMP="$ROOT/artifacts/.g13-test-tmp-$$"
RUN_ID="run_20990101_060606_1313"
RUN_DIR="$TMP/pipeline/$RUN_ID"
G12="$RUN_DIR/gate12/kernel.txt"
OUT="$RUN_DIR/gate13/request.txt"
cleanup(){ rm -rf "$TMP"; }
trap cleanup EXIT HUP INT TERM
rm -rf "$TMP"
mkdir -p "$(dirname "$G12")"
HEAD=$(git -C "$ROOT" rev-parse HEAD)
G4_HASH=$(printf '%s' g4 | sha256sum | awk '{print $1}')
PROFILE_HASH=$(printf '%s' profile | sha256sum | awk '{print $1}')
cat > "$G12" <<EOF
schema_version=gate12-runtime-kernel.v1
gate=gate12
gate_status=PASS
kernel_status=PASS
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4_HASH
profile_sha256=$PROFILE_HASH
gate11_artifact=$RUN_DIR/gate11/orchestrator.txt
orchestrator_id=orchestrator-$RUN_ID
kernel_id=kernel-$RUN_ID
kernel_name=runtime_self_kernel
kernel_type=RUNTIME_KERNEL
execution_status=DEFERRED
execution_authority=G17
EOF
printf '%s\n' '=== G13 STATIC BOUNDARY CHECK ==='
if grep -Eq 'runtime_stage\.sh|runtime_bootstrap\.sh|runtime_execution\.sh|/bin/sh[[:space:]]+-c|\bexec[[:space:]]+|\beval[[:space:]]+' "$G13"; then echo 'NO_G13_EXECUTION=FAIL'; exit 1; fi
echo 'NO_G13_EXECUTION=PASS'
printf '%s\n' '=== G13 POSITIVE ==='
if bash "$G13" "$RUN_ID" "$G12" "$OUT" >/dev/null 2>&1; then echo 'POSITIVE_VALID_KERNEL=PASS'; else echo 'POSITIVE_VALID_KERNEL=FAIL'; exit 1; fi
for x in 'schema_version=gate13-runtime-command-request.v1' 'gate=gate13' 'gate_status=PASS' 'request_status=REQUESTED' "pipeline_run_id=$RUN_ID" "source_commit=$HEAD" "gate4_contract_sha256=$G4_HASH" "profile_sha256=$PROFILE_HASH" "kernel_id=kernel-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "request_id=pwd-request-$RUN_ID" 'execution_status=DEFERRED' 'execution_authority=G17'; do grep -Fqx "$x" "$OUT" || { echo "ARTIFACT_FIELD_FAIL=$x"; exit 1; }; done
echo 'ARTIFACT_SCHEMA=PASS'
if grep -Eq 'command_result=|command_returncode=|execution_result=|stdout=|stderr=' "$OUT"; then echo 'NO_EXECUTION_EVIDENCE=FAIL'; exit 1; fi
echo 'NO_EXECUTION_EVIDENCE=PASS'
printf '%s\n' '=== G13 NEGATIVE CASES ==='
cp "$G12" "$TMP/wrong-run"; sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_20990101_060607_1313/' "$TMP/wrong-run"; if bash "$G13" "$RUN_ID" "$TMP/wrong-run" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_WRONG_RUN=FAIL'; exit 1; fi; echo 'NEGATIVE_WRONG_RUN=PASS'
cp "$G12" "$TMP/stale"; sed -i 's/^source_commit=.*/source_commit=0000000000000000000000000000000000000000/' "$TMP/stale"; if bash "$G13" "$RUN_ID" "$TMP/stale" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_STALE_SOURCE=FAIL'; exit 1; fi; echo 'NEGATIVE_STALE_SOURCE=PASS'
cp "$G12" "$TMP/not-pass"; sed -i 's/^kernel_status=.*/kernel_status=BLOCKED/' "$TMP/not-pass"; if bash "$G13" "$RUN_ID" "$TMP/not-pass" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_KERNEL_NOT_PASS=FAIL'; exit 1; fi; echo 'NEGATIVE_KERNEL_NOT_PASS=PASS'
cp "$G12" "$TMP/executed"; sed -i 's/^execution_status=.*/execution_status=EXECUTED/' "$TMP/executed"; if bash "$G13" "$RUN_ID" "$TMP/executed" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_UPSTREAM_EXECUTED=FAIL'; exit 1; fi; echo 'NEGATIVE_UPSTREAM_EXECUTED=PASS'
cp "$G12" "$TMP/bad-id"; sed -i 's/^kernel_id=.*/kernel_id=kernel-wrong/' "$TMP/bad-id"; if bash "$G13" "$RUN_ID" "$TMP/bad-id" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_KERNEL_ID=FAIL'; exit 1; fi; echo 'NEGATIVE_KERNEL_ID=PASS'
cp "$G12" "$TMP/bad-g4"; sed -i 's/^gate4_contract_sha256=.*/gate4_contract_sha256=bad/' "$TMP/bad-g4"; if bash "$G13" "$RUN_ID" "$TMP/bad-g4" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_GATE4_HASH=FAIL'; exit 1; fi; echo 'NEGATIVE_GATE4_HASH=PASS'
cp "$G12" "$TMP/bad-profile"; sed -i 's/^profile_sha256=.*/profile_sha256=bad/' "$TMP/bad-profile"; if bash "$G13" "$RUN_ID" "$TMP/bad-profile" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_PROFILE_HASH=FAIL'; exit 1; fi; echo 'NEGATIVE_PROFILE_HASH=PASS'
cp "$G12" "$TMP/bad-schema"; sed -i 's/^schema_version=.*/schema_version=wrong/' "$TMP/bad-schema"; if bash "$G13" "$RUN_ID" "$TMP/bad-schema" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_SCHEMA=FAIL'; exit 1; fi; echo 'NEGATIVE_SCHEMA=PASS'
cp "$G12" "$TMP/bad-authority"; sed -i 's/^execution_authority=.*/execution_authority=G13/' "$TMP/bad-authority"; if bash "$G13" "$RUN_ID" "$TMP/bad-authority" "$TMP/o" >/dev/null 2>&1; then echo 'NEGATIVE_AUTHORITY=FAIL'; exit 1; fi; echo 'NEGATIVE_AUTHORITY=PASS'
printf '%s\n' '=== G13 PROTECTION CHECK ==='
for p in tools/gate1_foundation_v2.sh tools/gate2_canonical_source_build_boundary.sh tools/execution_capability.sh tools/gate3_execution_capability.sh tools/environment_contract.sh tools/gate5_common.sh tools/runtime_bootstrap.sh tools/gate7_runtime_session.sh tools/gate8_runtime_task.sh tools/gate9_runtime_action.sh tools/gate10_runtime_workflow.sh tools/gate11_runtime_orchestrator.sh tools/gate12_runtime_kernel.sh tools/test_gate12_contract.sh; do test -f "$ROOT/$p" || { echo "PROTECTED_FILE_MISSING=$p"; exit 1; }; done
echo 'G1_G12_PROTECTION=PASS'
echo 'G13_CONTRACT_TEST=PASS'
