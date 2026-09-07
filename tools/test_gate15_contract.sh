#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
TMP=$(mktemp -d 2>/dev/null || printf '%s/g15-test-%s' "$ROOT" "$$")
mkdir -p "$TMP/run/gate14" "$TMP/run/gate15" "$TMP/run/gate12"
RUN_ID='run_20260907_151500_15001'
HEAD=$(git -C "$ROOT" rev-parse HEAD)
INPUT="$TMP/run/gate14/validation.txt"
OUTPUT="$TMP/run/gate15/policy.txt"

cleanup(){ rm -rf "$TMP"; }
trap cleanup EXIT

cat > "$INPUT" <<EOF
schema_version=gate14-runtime-command-validation.v1
gate=gate14
gate_status=PASS
validation_status=ALLOWED
validation_id=pwd-validation-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$(printf g4 | sha256sum | awk '{print $1}')
profile_sha256=$(printf profile | sha256sum | awk '{print $1}')
gate13_request_sha256=$(printf g13 | sha256sum | awk '{print $1}')
gate13_artifact=$TMP/g13.txt
gate12_kernel_sha256=$(printf g12 | sha256sum | awk '{print $1}')
gate12_artifact=$TMP/g12.txt
request_id=pwd-request-$RUN_ID
command=pwd
command_semantics=POSIX_PWD
command_sha256=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
execution_status=DEFERRED
execution_authority=G17
execution_path=DEFERRED:G17
created_at=2026-09-07T15:15:00Z
validation_path=$ROOT
EOF
printf '%s\n' synthetic > "$TMP/g13.txt"
printf '%s\n' synthetic > "$TMP/g12.txt"

fail_if(){
    local name=$1; shift
    if "$@" >/dev/null 2>&1; then
        echo "$name=FAIL"
        return 1
    fi
    echo "$name=PASS"
}


echo '=== G15 STATIC BOUNDARY CHECK ==='
! grep -Eq 'runtime_stage\.sh|runtime_execution\.sh|/bin/sh[[:space:]]+-c|[[:space:]]eval[[:space:]]' "$ROOT/tools/gate15_runtime_command_policy.sh"
echo 'NO_G15_EXECUTION=PASS'

echo '=== G15 POSITIVE ==='
G4=$(awk -F= '$1=="gate4_contract_sha256"{print $2}' "$INPUT")
PROFILE=$(awk -F= '$1=="profile_sha256"{print $2}' "$INPUT")
G13=$(awk -F= '$1=="gate13_request_sha256"{print $2}' "$INPUT")
G12=$(awk -F= '$1=="gate12_kernel_sha256"{print $2}' "$INPUT")
G14_HASH=$(sha256sum "$INPUT" | awk '{print $1}')
bash "$ROOT/tools/gate15_runtime_command_policy.sh" "$RUN_ID" "$INPUT" "$OUTPUT" >/dev/null
[ -f "$OUTPUT" ]
echo 'POSITIVE_VALID_VALIDATION=PASS'
grep -Fqx 'policy_status=ALLOWED' "$OUTPUT"
grep -Fqx 'execution_status=DEFERRED' "$OUTPUT"
grep -Fqx 'execution_authority=G17' "$OUTPUT"
echo 'POLICY_SCHEMA=PASS'
! grep -Eq 'command_result=|command_returncode=|guest_stdout=|guest_stderr=' "$OUTPUT"
echo 'NO_EXECUTION_EVIDENCE=PASS'

mutate_and_expect_block(){
    local name=$1 key=$2 value=$3
    cp "$INPUT" "$TMP/mut.txt"
    sed -i "s|^${key}=.*$|${key}=${value}|" "$TMP/mut.txt"
    rm -f "$TMP/out.txt"
    if bash "$ROOT/tools/gate15_runtime_command_policy.sh" "$RUN_ID" "$TMP/mut.txt" "$TMP/out.txt" >/dev/null 2>&1; then
        echo "$name=FAIL"
        return 1
    fi
    echo "$name=PASS"
}

echo '=== G15 NEGATIVE CASES ==='
mutate_and_expect_block NEGATIVE_WRONG_RUN pipeline_run_id run_20260907_151500_15002
mutate_and_expect_block NEGATIVE_STALE_SOURCE source_commit 0000000000000000000000000000000000000000000000000000000000000000
mutate_and_expect_block NEGATIVE_VALIDATION_NOT_ALLOWED validation_status BLOCKED
mutate_and_expect_block NEGATIVE_REQUEST_ID request_id wrong-request
mutate_and_expect_block NEGATIVE_COMMAND command sh
mutate_and_expect_block NEGATIVE_COMMAND_SEMANTICS command_semantics SHELL_COMMAND
mutate_and_expect_block NEGATIVE_EXECUTION_STATUS execution_status EXECUTED
mutate_and_expect_block NEGATIVE_AUTHORITY execution_authority G16
mutate_and_expect_block NEGATIVE_KERNEL_HASH gate12_kernel_sha256 0000000000000000000000000000000000000000000000000000000000000000

echo '=== G15 PROTECTION CHECK ==='
for p in \
  tools/gate1_foundation_v2.sh \
  tools/gate2_canonical_source_build_boundary.sh \
  tools/execution_capability.sh \
  tools/gate3_execution_capability.sh \
  tools/environment_contract.sh \
  tools/gate5_common.sh \
  tools/runtime_bootstrap.sh \
  tools/gate7_runtime_session.sh \
  tools/gate8_runtime_task.sh \
  tools/gate9_runtime_action.sh \
  tools/gate10_runtime_workflow.sh \
  tools/gate11_runtime_orchestrator.sh \
  tools/gate12_runtime_kernel.sh \
  tools/gate13_runtime_command_request.sh \
  tools/gate14_runtime_command_validation.sh \
  tools/test_gate14_contract.sh; do
  test -f "$ROOT/$p"
done
echo 'G1_G14_PROTECTION=PASS'
echo 'G15_CONTRACT_TEST=PASS'
