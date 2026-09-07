#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
TMP=$(mktemp -d 2>/dev/null || printf '%s/g15-test-%s' "$ROOT" "$$")
mkdir -p "$TMP/run/gate14" "$TMP/run/gate15" "$TMP/run/gate12"
RUN_ID='run_20260907_151500_15001'
HEAD=$(git -C "$ROOT" rev-parse HEAD)
INPUT="$TMP/run/gate14/validation.txt"
OUTPUT="$TMP/run/gate15/policy.txt"
G13_ARTIFACT="$TMP/g13.txt"
G12_ARTIFACT="$TMP/g12.txt"

cleanup(){ rm -rf "$TMP"; }
trap cleanup EXIT

printf '%s\n' synthetic-g13 > "$G13_ARTIFACT"
printf '%s\n' synthetic-g12 > "$G12_ARTIFACT"
G13_SHA=$(sha256sum "$G13_ARTIFACT" | awk '{print $1}')
G12_SHA=$(sha256sum "$G12_ARTIFACT" | awk '{print $1}')
G4=$(printf g4 | sha256sum | awk '{print $1}')
PROFILE=$(printf profile | sha256sum | awk '{print $1}')

cat > "$INPUT" <<EOF
schema_version=gate14-runtime-command-validation.v1
gate=gate14
gate_status=PASS
validation_status=ALLOWED
validation_id=pwd-validation-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4
profile_sha256=$PROFILE
gate13_request_sha256=$G13_SHA
gate13_artifact=$G13_ARTIFACT
gate12_kernel_sha256=$G12_SHA
gate12_artifact=$G12_ARTIFACT
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

echo '=== G15 STATIC BOUNDARY CHECK ==='
! grep -Eq 'runtime_stage\.sh|runtime_execution\.sh|/bin/sh[[:space:]]+-c|[[:space:]]eval[[:space:]]' "$ROOT/tools/gate15_runtime_command_policy.sh"
echo 'NO_G15_EXECUTION=PASS'

echo '=== G15 POSITIVE ==='
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
mutate_and_expect_block NEGATIVE_G13_HASH gate13_request_sha256 0000000000000000000000000000000000000000000000000000000000000000

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
