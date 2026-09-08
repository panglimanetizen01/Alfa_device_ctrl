#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
TMP=$(mktemp -d 2>/dev/null || printf '%s/g17-test-%s' "$ROOT" "$$"); trap 'rm -rf "$TMP"' EXIT
RUN_ID='run_20260908_093200_17001'; HEAD=$(git -C "$ROOT" rev-parse HEAD)
G15="$TMP/g15.txt"; G5="$TMP/g5.txt"; AUTH="$TMP/g16.txt"; OUT="$TMP/g17.txt"; GUEST="$TMP/rootfs"
printf '%s\n' g15 > "$G15"; printf '%s\n' g5 > "$G5"; mkdir -p "$GUEST/root" "$GUEST/etc" "$GUEST/bin" "$GUEST/usr/bin"
G15_SHA=$(sha256sum "$G15"|awk '{print $1}'); G5_SHA=$(sha256sum "$G5"|awk '{print $1}'); G4=$(printf g4|sha256sum|awk '{print $1}'); PROFILE=$(printf profile|sha256sum|awk '{print $1}'); CMD_SHA=$(printf '%s\n' pwd|sha256sum|awk '{print $1}')
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
PROOT_EXEC=${ALFA_G17_PROOT:-$(command -v proot 2>/dev/null || true)}; [ -x "$PROOT_EXEC" ] || { echo G17_CONTRACT_TEST=BLOCKED; exit 1; }
cp -L /etc/os-release "$GUEST/etc/os-release"; cp -L /bin/sh "$GUEST/bin/sh"; cp -L /usr/bin/readlink "$GUEST/usr/bin/readlink"; chmod +x "$GUEST/bin/sh" "$GUEST/usr/bin/readlink"
copy_libs(){ ldd "$1" 2>/dev/null | awk '/=> \/|^[[:space:]]*\// {for(i=1;i<=NF;i++) if($i ~ /^\//) print $i}' | sort -u | while IFS= read -r lib; do [ -f "$lib" ] || continue; mkdir -p "$GUEST$(dirname "$lib")"; cp -L "$lib" "$GUEST$lib"; done; }
copy_libs /bin/sh; copy_libs /usr/bin/readlink
printf '%s\n' '=== G17 STATIC BOUNDARY ==='
! grep -Eq 'cd "\$ROOT".*pwd|RESULT=\$\(pwd' "$ROOT/tools/gate17_runtime_execution.sh"
! grep -Eq 'runtime_stage\.sh|runtime_execution\.sh' "$ROOT/tools/gate17_runtime_execution.sh"
! grep -Eq 'eval[[:space:]]|\$\(.*field.*command' "$ROOT/tools/gate17_runtime_execution.sh"
grep -Fq 'PROOT_EXEC=${4:-}' "$ROOT/tools/gate17_runtime_execution.sh"; grep -Fq 'ROOTFS=${5:-}' "$ROOT/tools/gate17_runtime_execution.sh"; grep -Fq '"$PROOT_EXEC" -r "$ROOTFS" -b /proc:/proc -w /root /bin/sh -c "$GUEST_EVIDENCE_SCRIPT"' "$ROOT/tools/gate17_runtime_execution.sh"
echo NO_G17_HOST_PWD=PASS; echo G17_PROOT_BOUNDARY=PASS
printf '%s\n' '=== G17 REAL POSITIVE EXECUTION ==='
if ! bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$AUTH" "$OUT" "$PROOT_EXEC" "$GUEST" >"$TMP/positive.stdout" 2>"$TMP/positive.stderr"; then
  echo '--- G17 EXECUTOR STDOUT ---'; cat "$TMP/positive.stdout"
  echo '--- G17 EXECUTOR STDERR ---'; cat "$TMP/positive.stderr"
  echo '--- G17 ROOTFS EXECUTABLES ---'; ls -l "$GUEST/bin/sh" "$GUEST/usr/bin/readlink" "$GUEST/lib64/ld-linux-x86-64.so.2" || true
  echo '--- G17 ROOTFS INTERPRETERS/LIBS ---'; ldd /bin/sh; ldd /usr/bin/readlink
  echo '--- DIRECT PROOT DIAGNOSTIC ---'; "$PROOT_EXEC" -v 2 -r "$GUEST" -b /proc:/proc -w /root /bin/sh -c 'printf "DIRECT_PWD=%s\\n" "$(pwd)"; /usr/bin/readlink /proc/self/exe; /usr/bin/readlink /proc/self/cwd; /usr/bin/readlink /proc/self/root' || true
  exit 1
fi
grep -Fqx gate_status=PASS "$OUT"; grep -Fqx execution_status=EXECUTED "$OUT"; grep -Fqx result_status=PASS "$OUT"; grep -Fqx command=pwd "$OUT"; grep -Fqx command_returncode=0 "$OUT"; grep -Fqx command_result=/root "$OUT"
for k in engine_path engine_sha256 rootfs_path rootfs_os_id guest_pid guest_internal_pid host_tracee_pid guest_proc_exe guest_proc_cwd guest_proc_root; do grep -Fq "${k}=" "$OUT"; done
[ "$(awk -F= '$1=="guest_pid"{print substr($0,index($0,"=")+1);exit}' "$OUT")" = "$(awk -F= '$1=="guest_internal_pid"{print substr($0,index($0,"=")+1);exit}' "$OUT")" ]; [ "$(awk -F= '$1=="guest_proc_cwd"{print substr($0,index($0,"=")+1);exit}' "$OUT")" = /root ]; [ "$(awk -F= '$1=="guest_proc_root"{print substr($0,index($0,"=")+1);exit}' "$OUT")" != / ]
echo POSITIVE_REAL_GUEST_EXECUTION=PASS; echo EXECUTION_EVIDENCE=PASS
mutate_expect_block(){ local n=$1 k=$2 v=$3; cp "$AUTH" "$TMP/mut"; sed -i "s|^${k}=.*$|${k}=${v}|" "$TMP/mut"; rm -f "$TMP/out"; if bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$TMP/mut" "$TMP/out" "$PROOT_EXEC" "$GUEST" >/dev/null 2>&1; then echo "$n=FAIL"; return 1; fi; echo "$n=PASS"; }
mutate_expect_block NEGATIVE_WRONG_RUN pipeline_run_id run_20260908_093200_17002
mutate_expect_block NEGATIVE_STALE_SOURCE source_commit 0000000000000000000000000000000000000000
mutate_expect_block NEGATIVE_NOT_AUTHORIZED authorization_status DENIED
mutate_expect_block NEGATIVE_EXECUTED execution_status EXECUTED
mutate_expect_block NEGATIVE_AUTHORITY execution_authority G16
mutate_expect_block NEGATIVE_COMMAND command sh
mutate_expect_block NEGATIVE_SEMANTICS command_semantics SHELL_COMMAND
mutate_expect_block NEGATIVE_COMMAND_HASH command_sha256 0000000000000000000000000000000000000000000000000000000000000000
mutate_expect_block NEGATIVE_G15_HASH gate15_policy_sha256 0000000000000000000000000000000000000000000000000000000000000000
if bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$AUTH" "$TMP/out" "$TMP/no-proot" "$GUEST" >/dev/null 2>&1; then echo NEGATIVE_MISSING_ENGINE=FAIL; exit 1; fi; echo NEGATIVE_MISSING_ENGINE=PASS
if bash "$ROOT/tools/gate17_runtime_execution.sh" "$RUN_ID" "$AUTH" "$TMP/out" "$PROOT_EXEC" "$TMP/no-rootfs" >/dev/null 2>&1; then echo NEGATIVE_MISSING_ROOTFS=FAIL; exit 1; fi; echo NEGATIVE_MISSING_ROOTFS=PASS
for p in tools/gate1_foundation_v2.sh tools/gate2_canonical_source_build_boundary.sh tools/execution_capability.sh tools/gate3_execution_capability.sh tools/environment_contract.sh tools/gate5_common.sh tools/runtime_bootstrap.sh tools/gate7_runtime_session.sh tools/gate8_runtime_task.sh tools/gate9_runtime_action.sh tools/gate10_runtime_workflow.sh tools/gate11_runtime_orchestrator.sh tools/gate12_runtime_kernel.sh tools/gate13_runtime_command_request.sh tools/gate14_runtime_command_validation.sh tools/gate15_runtime_command_policy.sh tools/test_gate15_contract.sh tools/gate16_runtime_execution_authorization.sh tools/test_gate16_contract.sh; do test -f "$ROOT/$p"; done
echo G1_G16_PROTECTION=PASS; echo G17_CONTRACT_TEST=PASS
