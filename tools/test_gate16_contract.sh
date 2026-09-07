#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
TMP=$(mktemp -d 2>/dev/null || printf '%s/g16-test-%s' "$ROOT" "$$")
RUN_ID='run_20260907_161600_16001'
HEAD=$(git -C "$ROOT" rev-parse HEAD)
G12="$TMP/g12.txt"
G13="$TMP/g13.txt"
G14="$TMP/g14.txt"
POLICY="$TMP/g15-policy.txt"
AUTH="$TMP/g5-auth.txt"
OUTPUT="$TMP/g16.txt"

cleanup(){ rm -rf "$TMP"; }
trap cleanup EXIT

printf '%s\n' synthetic-g12 > "$G12"
printf '%s\n' synthetic-g13 > "$G13"
printf '%s\n' synthetic-g14 > "$G14"
G12_SHA=$(sha256sum "$G12" | awk '{print $1}')
G13_SHA=$(sha256sum "$G13" | awk '{print $1}')
G14_SHA=$(sha256sum "$G14" | awk '{print $1}')
G4=$(printf g4 | sha256sum | awk '{print $1}')
PROFILE=$(printf profile | sha256sum | awk '{print $1}')
CMD_SHA=$(printf '%s\n' pwd | sha256sum | awk '{print $1}')

cat > "$POLICY" <<EOF
schema_version=gate15-runtime-command-policy.v1
gate=gate15
gate_status=PASS
policy_status=ALLOWED
policy_id=policy-$RUN_ID
policy_version=gate15-policy-v1
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4
profile_sha256=$PROFILE
gate14_validation_sha256=$G14_SHA
gate14_artifact=$G14
gate13_request_sha256=$G13_SHA
gate13_artifact=$G13
gate12_kernel_sha256=$G12_SHA
gate12_artifact=$G12
validation_id=pwd-validation-$RUN_ID
request_id=pwd-request-$RUN_ID
command=pwd
command_semantics=POSIX_PWD
command_sha256=$CMD_SHA
policy_reason=synthetic-valid-policy
execution_status=DEFERRED
execution_authority=G17
execution_path=DEFERRED:G17
created_at=2026-09-07T16:16:00Z
policy_path=$ROOT
EOF

cat > "$AUTH" <<EOF
schema_version=gate5-authorization.v1
authorization_id=authorization-pwd-request-$RUN_ID-abcdef1234567890
request_id=pwd-request-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$G4
profile_sha256=$PROFILE
policy_version=gate5-policy-v1
decision_id=decision-pwd-request-$RUN_ID-1234567890abcdef
authorization_status=AUTHORIZED
authorization_reason=synthetic-valid-gate5-authorization
created_at=2026-09-07T16:16:00Z
EOF

echo '=== G16 STATIC BOUNDARY CHECK ==='
! grep -Eq 'runtime_stage\.sh|runtime_execution\.sh|/bin/sh[[:space:]]+-c|[[:space:]]eval[[:space:]]' "$ROOT/tools/gate16_runtime_execution_authorization.sh"
! grep -Eq 'command_result=|command_returncode=|guest_stdout=|guest_stderr=' "$ROOT/tools/gate16_runtime_execution_authorization.sh"
echo 'NO_G16_EXECUTION=PASS'

echo '=== G16 POSITIVE ==='
bash "$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$POLICY" "$AUTH" "$OUTPUT" >/dev/null
[ -f "$OUTPUT" ]
grep -Fqx 'gate_status=PASS' "$OUTPUT"
grep -Fqx 'authorization_status=AUTHORIZED' "$OUTPUT"
grep -Fqx 'policy_status=ALLOWED' "$OUTPUT"
grep -Fqx 'execution_status=DEFERRED' "$OUTPUT"
grep -Fqx 'execution_authority=G17' "$OUTPUT"
echo 'POSITIVE_VALID_AUTHORIZATION=PASS'
grep -Fqx "gate15_policy_sha256=$(sha256sum "$POLICY" | awk '{print $1}')" "$OUTPUT"
grep -Fqx "gate5_authorization_sha256=$(sha256sum "$AUTH" | awk '{print $1}')" "$OUTPUT"
echo 'AUTHORIZATION_SCHEMA=PASS'
! grep -Eq 'command_result=|command_returncode=|guest_stdout=|guest_stderr=' "$OUTPUT"
echo 'NO_EXECUTION_EVIDENCE=PASS'

mutate_policy_expect_block(){
    local name=$1 key=$2 value=$3
    cp "$POLICY" "$TMP/mut-policy.txt"
    sed -i "s|^${key}=.*$|${key}=${value}|" "$TMP/mut-policy.txt"
    rm -f "$TMP/out.txt"
    if bash "$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$TMP/mut-policy.txt" "$AUTH" "$TMP/out.txt" >/dev/null 2>&1; then
        echo "$name=FAIL"
        return 1
    fi
    echo "$name=PASS"
}
mutate_auth_expect_block(){
    local name=$1 key=$2 value=$3
    cp "$AUTH" "$TMP/mut-auth.txt"
    sed -i "s|^${key}=.*$|${key}=${value}|" "$TMP/mut-auth.txt"
    rm -f "$TMP/out.txt"
    if bash "$ROOT/tools/gate16_runtime_execution_authorization.sh" "$RUN_ID" "$POLICY" "$TMP/mut-auth.txt" "$TMP/out.txt" >/dev/null 2>&1; then
        echo "$name=FAIL"
        return 1
    fi
    echo "$name=PASS"
}

echo '=== G16 NEGATIVE CASES ==='
mutate_policy_expect_block NEGATIVE_POLICY_NOT_ALLOWED policy_status BLOCKED
mutate_policy_expect_block NEGATIVE_POLICY_RUN pipeline_run_id run_20260907_161600_16002
mutate_policy_expect_block NEGATIVE_POLICY_STALE source_commit 00000000000000000000000000000000000000000000000000000000000000000000
mutate_policy_expect_block NEGATIVE_POLICY_REQUEST_ID request_id wrong-request
mutate_policy_expect_block NEGATIVE_POLICY_COMMAND command sh
mutate_policy_expect_block NEGATIVE_POLICY_SEMANTICS command_semantics SHELL_COMMAND
mutate_policy_expect_block NEGATIVE_POLICY_EXECUTION execution_status EXECUTED
mutate_policy_expect_block NEGATIVE_POLICY_AUTHORITY execution_authority G16
mutate_policy_expect_block NEGATIVE_POLICY_HASH gate14_validation_sha256 0000000000000000000000000000000000000000000000000000000000000000
mutate_auth_expect_block NEGATIVE_G5_AUTH_DENIED authorization_status DENIED
mutate_auth_expect_block NEGATIVE_G5_AUTH_RUN pipeline_run_id run_20260907_161600_16002
mutate_auth_expect_block NEGATIVE_G5_AUTH_SOURCE source_commit 00000000000000000000000000000000000000000000000000000000000000000000
mutate_auth_expect_block NEGATIVE_G5_AUTH_G4 gate4_contract_sha256 0000000000000000000000000000000000000000000000000000000000000000
mutate_auth_expect_block NEGATIVE_G5_AUTH_REQUEST request_id wrong-request

echo '=== G16 PROTECTION CHECK ==='
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
  tools/gate15_runtime_command_policy.sh \
  tools/test_gate15_contract.sh; do
  test -f "$ROOT/$p"
done
echo 'G1_G15_PROTECTION=PASS'
echo 'G16_CONTRACT_TEST=PASS'
