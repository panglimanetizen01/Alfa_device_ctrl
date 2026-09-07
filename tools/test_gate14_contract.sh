#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
RUN_ID="run_20990101_000000_14001"
G13="$TMP/g13.txt"
OUT="$TMP/g14.txt"
HEAD=$(git -C "$ROOT" rev-parse HEAD)
G4=$(printf '%064d' 1)
PROFILE=$(printf '%064d' 2)
printf 'kernel\n' > "$TMP/g12.txt"
KERNEL_SHA=$(sha256sum "$TMP/g12.txt" | awk '{print $1}')
COMMAND_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')
cat > "$G13" <<EOF
schema_version=gate13-runtime-command-request.v1
gate=gate13
gate_status=PASS
request_status=REQUESTED
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4
profile_sha256=$PROFILE
gate12_kernel_sha256=$KERNEL_SHA
gate12_artifact=$TMP/g12.txt
kernel_id=kernel-$RUN_ID
request_id=pwd-request-$RUN_ID
command=pwd
command_semantics=POSIX_PWD
command_sha256=$COMMAND_SHA
execution_status=DEFERRED
execution_authority=G17
execution_path=DEFERRED:G17
created_at=2099-01-01T00:00:00Z
construction_path=$ROOT
stage_reason=test
EOF

echo '=== G14 STATIC BOUNDARY CHECK ==='
if grep -Eq 'runtime_stage\\.sh|runtime_execution\\.sh|/bin/sh[[:space:]]+-c|[[:space:]]eval[[:space:]]|command_result|command_returncode|execution_result' tools/gate14_runtime_command_validation.sh 2>/dev/null; then
  echo 'NO_G14_EXECUTION=FAIL'; exit 1
fi
echo 'NO_G14_EXECUTION=PASS'

echo '=== G14 POSITIVE ==='
if bash tools/gate14_runtime_command_validation.sh "$RUN_ID" "$G13" "$OUT" >/dev/null && grep -Fqx 'gate_status=PASS' "$OUT" && grep -Fqx 'validation_status=ALLOWED' "$OUT" && grep -Fqx 'command=pwd' "$OUT" && grep -Fqx "gate12_kernel_sha256=$KERNEL_SHA" "$OUT"; then
  echo 'POSITIVE_VALID_REQUEST=PASS'
else
  echo 'POSITIVE_VALID_REQUEST=FAIL'; exit 1
fi
if grep -Eq 'command_result=|command_returncode=|execution_result=|guest_stdout=|guest_stderr=' "$OUT"; then
  echo 'NO_EXECUTION_EVIDENCE=FAIL'; exit 1
else
  echo 'NO_EXECUTION_EVIDENCE=PASS'
fi

gen_negative() {
  local name="$1" field="$2" value="$3" expected="$4"
  local f="$TMP/$name.txt" o="$TMP/$name.out"
  cp "$G13" "$f"
  awk -F= -v k="$field" -v v="$value" 'BEGIN{OFS="="} $1==k {$0=k"="v} {print}' "$f" > "$f.new" && mv "$f.new" "$f"
  if bash tools/gate14_runtime_command_validation.sh "$RUN_ID" "$f" "$o" >/dev/null 2>&1; then
    echo "$expected=FAIL"; exit 1
  else
    echo "$expected=PASS"
  fi
}

echo '=== G14 NEGATIVE CASES ==='
gen_negative wrong_run pipeline_run_id run_20990101_000000_99999 NEGATIVE_WRONG_RUN
gen_negative stale_source source_commit 0000000000000000000000000000000000000000 NEGATIVE_STALE_SOURCE
gen_negative not_pass gate_status BLOCKED NEGATIVE_REQUEST_NOT_PASS
gen_negative wrong_request request_status INVALID NEGATIVE_REQUEST_STATUS
gen_negative wrong_command command id NEGATIVE_COMMAND
gen_negative wrong_semantics command_semantics SHELL_COMMAND NEGATIVE_COMMAND_SEMANTICS
gen_negative wrong_exec execution_status EXECUTED NEGATIVE_UPSTREAM_EXECUTED
gen_negative wrong_authority execution_authority G14 NEGATIVE_AUTHORITY
gen_negative wrong_kernel_hash gate12_kernel_sha256 $(printf '%064d' 9) NEGATIVE_KERNEL_HASH

echo '=== G14 PROTECTION CHECK ==='
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
  tools/test_gate13_contract.sh; do
  test -f "$p"
done
echo 'G1_G13_PROTECTION=PASS'
echo 'G14_CONTRACT_TEST=PASS'
