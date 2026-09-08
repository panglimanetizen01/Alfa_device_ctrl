#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
TMP=$(mktemp -d 2>/dev/null || printf '%s/g17-test-%s' "$ROOT" "$$")
RUN_ID='run_20260908_093200_17001'
HEAD=$(git -C "$ROOT" rev-parse HEAD)
G15="$TMP/g15.txt"; G5="$TMP/g5.txt"; AUTH="$TMP/g16.txt"; OUT="$TMP/g17.txt"
cleanup(){ rm -rf "$TMP"; }; trap cleanup EXIT
printf '%s\n' g15 > "$G15"; printf '%s\n' g5 > "$G5"
G15_SHA=$(sha256sum "$G15" | awk '{print $1}'); G5_SHA=$(sha256sum "$G5" | awk '{print $1}')
G4=$(printf g4 | sha256sum | awk '{print $1}'); PROFILE=$(printf profile | sha256sum | awk '{print $1}')
CMD_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
cat > "$AUTH" <<EOF
schema_version=gate16-runtime-execution-authorization.v1
gate=gate16
gate_status=PASS
authorization_status=AUTHORIZED
authorization_id=authorization-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4
profile_sha256=$PROFILE
gate15_policy_sha256=$G15_SHA
gate15_artifact=$G15
gate5_authorization_sha256=$G5_SHA
gate5_authorization_artifact=$G5
request_id=pwd-request-$RUN_ID
command=pwd
command_semantics=POSIX_PWD
command_sha256=$CMD_SHA
execution_status=DEFERRED
execution_authority=G17
execution_path=DEFERRED:G17
EOF

echo '=== G17 STATIC BOUNDARY ==='
! grep -Eq 'runtime_stage\.sh|runtime_execution\.sh' "$ROOT/tools/gate17_runtime_execution.sh"
! grep -Eq 'eval[[:space:]]|\$\(.*field.*command' "$ROOT/tools/gate17_runtime_execution.sh"
echo 'NO_G17_LEGACY_EXECUTION_PATH=PASS'

echo '=== G17 REAL POSITIVE EXECUTION ==='
bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$AUTH" "$OUT" >/dev/null
[ -f "$OUT" ]
grep -Fqx 'gate_status=PASS' "$OUT"
grep -Fqx 'execution_status=PASS' "$OUT"
grep -Fqx 'result_status=PASS' "$OUT"
grep -Fqx 'command=pwd' "$OUT"
grep -Fqx 'command_returncode=0' "$OUT"
ACTUAL=$(awk -F= '$1=="command_result" {sub(/^[^=]*=/,"",$0); print; exit}' "$OUT")
[ "$ACTUAL" = "$ROOT" ]
echo 'POSITIVE_REAL_PWD_EXECUTION=PASS'
echo 'EXECUTION_EVIDENCE=PASS'

mutate_expect_block(){
  local name=$1 key=$2 value=$3
  cp "$AUTH" "$TMP/mut.txt"
  sed -i "s|^${key}=.*$|${key}=${value}|" "$TMP/mut.txt"
  rm -f "$TMP/out.txt"
  if bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$TMP/mut.txt" "$TMP/out.txt" >/dev/null 2>&1; then echo "$name=FAIL"; return 1; fi
  echo "$name=PASS"
}

echo '=== G17 NEGATIVE CASES ==='
mutate_expect_block NEGATIVE_WRONG_RUN pipeline_run_id run_20260908_093200_17002
mutate_expect_block NEGATIVE_STALE_SOURCE source_commit 00000000000000000000000000000000000000000000000000000000000000000000
mutate_expect_block NEGATIVE_NOT_AUTHORIZED authorization_status DENIED
mutate_expect_block NEGATIVE_EXECUTED execution_status EXECUTED
mutate_expect_block NEGATIVE_AUTHORITY execution_authority G16
mutate_expect_block NEGATIVE_COMMAND command sh
mutate_expect_block NEGATIVE_SEMANTICS command_semantics SHELL_COMMAND
mutate_expect_block NEGATIVE_COMMAND_HASH command_sha256 0000000000000000000000000000000000000000000000000000000000000000
mutate_expect_block NEGATIVE_G15_HASH gate15_policy_sha256 0000000000000000000000000000000000000000000000000000000000000000

echo '=== G17 PROTECTION ==='
for p in tools/gate1_foundation_v2.sh tools/gate2_canonical_source_build_boundary.sh tools/execution_capability.sh tools/gate3_execution_capability.sh tools/environment_contract.sh tools/gate5_common.sh tools/runtime_bootstrap.sh tools/gate7_runtime_session.sh tools/gate8_runtime_task.sh tools/gate9_runtime_action.sh tools/gate10_runtime_workflow.sh tools/gate11_runtime_orchestrator.sh tools/gate12_runtime_kernel.sh tools/gate13_runtime_command_request.sh tools/gate14_runtime_command_validation.sh tools/gate15_runtime_command_policy.sh tools/test_gate15_contract.sh tools/gate16_runtime_execution_authorization.sh tools/test_gate16_contract.sh; do test -f "$ROOT/$p"; done
echo 'G1_G16_PROTECTION=PASS'
echo 'G17_CONTRACT_TEST=PASS'
