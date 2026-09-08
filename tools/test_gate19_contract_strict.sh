#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID="run_20260908_121900_19000"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G16="$RUN_DIR/gate16/live-authorization.txt"
G17="$RUN_DIR/gate17/execution.txt"
G18="$RUN_DIR/gate18/result.txt"
GOOD_OUT="$RUN_DIR/gate19/good-consumer.txt"
BAD_OUT="$RUN_DIR/gate19/bad-consumer.txt"
CROSS_OUT="$RUN_DIR/gate19/cross-run.txt"
HASH_OUT="$RUN_DIR/gate19/hash-mismatch.txt"
SOURCE_OUT="$RUN_DIR/gate19/source-mismatch.txt"

rm -rf "$RUN_DIR"
mkdir -p "$RUN_DIR/gate4" "$RUN_DIR/gate16" "$RUN_DIR/gate17" "$RUN_DIR/gate18"

HEAD=$(git -C "$ROOT" rev-parse HEAD)
printf '%s\n' "pipeline_run_id=$RUN_ID" 'contract_result=VALID' "source_commit=$HEAD" 'profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' > "$G4"
printf '%s\n' 'authorization_status=AUTHORIZED' > "$G16"
CONTRACT_SHA=$(sha256sum "$G4" | awk '{print $1}')
G16_SHA=$(sha256sum "$G16" | awk '{print $1}')
RESULT_SHA=e7748a2b5e00e724562c9d7688fa9dc1aecbf20dba00096d6e2d91bf37696449
CMD_SHA=b1119ecaf54feb68aa5ca8387277e0a929c71cd99fc70f1985b1de1b3b8724b6

printf '%s\n' "schema_version=gate17-runtime-execution.v1" 'gate=gate17' 'gate_status=PASS' "execution_id=execution-pwd-$RUN_ID" "pipeline_run_id=$RUN_ID" "source_commit=$HEAD" "gate4_contract_sha256=$CONTRACT_SHA" 'profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' "gate16_authorization_sha256=$G16_SHA" "gate16_authorization_artifact=$G16" "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "command_sha256=$CMD_SHA" 'authorization_status=AUTHORIZED' 'execution_status=PASS' 'result_status=PASS' 'command_result=/expected/runtime/path' 'command_returncode=0' "command_result_sha256=$RESULT_SHA" 'created_at=2026-09-08T05:00:26Z' 'execution_path=/expected/runtime/path' > "$G17"

printf '%s\n' 'schema_version=gate18-artifact.v1' 'gate=gate18' 'gate_status=PASS' "pipeline_run_id=$RUN_ID" "source_commit=$HEAD" "gate4_contract_sha256=$CONTRACT_SHA" 'profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' "input_artifact=$G17" 'created_at=2026-09-08T05:01:00Z' "execution_id=execution-pwd-$RUN_ID" "request_id=pwd-request-$RUN_ID" 'command=pwd' 'command_semantics=POSIX_PWD' "command_sha256=$CMD_SHA" 'authorization_status=AUTHORIZED' 'execution_status=PASS' 'result_status=PASS' 'command_result=/expected/runtime/path' 'command_returncode=0' "command_result_sha256=$RESULT_SHA" "gate16_authorization_sha256=$G16_SHA" "gate16_authorization_artifact=$G16" 'execution_path=/expected/runtime/path' > "$G18"

bash "$ROOT/tools/runtime_result_consumer.sh" "$RUN_ID" "$G18" "$GOOD_OUT" >/dev/null
[ "$(awk -F= '$1=="GATE19_STATUS"{print $2}' <(bash "$ROOT/tools/runtime_result_consumer.sh" "$RUN_ID" "$G18" "$GOOD_OUT" 2>/dev/null))" = PASS ] || { printf '%s\n' 'G19_VALID_EVIDENCE=FAIL'; exit 1; }
[ "$(awk -F= '$1=="consume_status"{print $2}' "$GOOD_OUT")" = ACCEPTED ] || { printf '%s\n' 'G19_CONSUME_STATUS=FAIL'; exit 1; }
[ "$(awk -F= '$1=="next_phase_status"{print $2}' "$GOOD_OUT")" = UNLOCKED ] || { printf '%s\n' 'G19_NEXT_PHASE_UNLOCK=FAIL'; exit 1; }
[ "$(awk -F= '$1=="apk_build_status"{print $2}' "$GOOD_OUT")" = OPEN ] || { printf '%s\n' 'G19_APK_BUILD_OPEN=FAIL'; exit 1; }
for field in pipeline_run_id source_commit gate4_contract_sha256 profile_sha256 execution_id request_id command command_semantics command_sha256 authorization_status execution_status result_status command_result command_returncode command_result_sha256 gate16_authorization_sha256 gate16_authorization_artifact execution_path; do
    [ -n "$(awk -F= -v k="$field" '$1==k{print substr($0,index($0,"=")+1)}' "$GOOD_OUT")" ] || { printf '%s\n' "G19_${field}_PRESENCE=FAIL"; exit 1; }
done

cp "$G18" "$RUN_DIR/gate18/bad.txt"
sed -i 's/^result_status=PASS/result_status=ERROR/' "$RUN_DIR/gate18/bad.txt"
if bash "$ROOT/tools/runtime_result_consumer.sh" "$RUN_ID" "$RUN_DIR/gate18/bad.txt" "$BAD_OUT" >/dev/null 2>&1; then printf '%s\n' 'G19_NONPASS=FAIL'; exit 1; fi
[ "$(awk -F= '$1=="consume_status"{print $2}' "$BAD_OUT")" = REJECTED ] || { printf '%s\n' 'G19_NONPASS_REJECT=FAIL'; exit 1; }

cp "$G18" "$RUN_DIR/gate18/cross-run.txt"
sed -i 's/^pipeline_run_id=.*/pipeline_run_id=run_wrong/' "$RUN_DIR/gate18/cross-run.txt"
if bash "$ROOT/tools/runtime_result_consumer.sh" "$RUN_ID" "$RUN_DIR/gate18/cross-run.txt" "$CROSS_OUT" >/dev/null 2>&1; then printf '%s\n' 'G19_CROSS_RUN=FAIL'; exit 1; fi

cp "$G18" "$RUN_DIR/gate18/hash-mismatch.txt"
sed -i 's/^command_result_sha256=.*/command_result_sha256=ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff/' "$RUN_DIR/gate18/hash-mismatch.txt"
if bash "$ROOT/tools/runtime_result_consumer.sh" "$RUN_ID" "$RUN_DIR/gate18/hash-mismatch.txt" "$HASH_OUT" >/dev/null 2>&1; then printf '%s\n' 'G19_HASH_MISMATCH=FAIL'; exit 1; fi

cp "$G18" "$RUN_DIR/gate18/source-mismatch.txt"
sed -i 's/^source_commit=.*/source_commit=1933ee2208c8d29f7fa285572c1ba864ab98faec/' "$RUN_DIR/gate18/source-mismatch.txt"
if bash "$ROOT/tools/runtime_result_consumer.sh" "$RUN_ID" "$RUN_DIR/gate18/source-mismatch.txt" "$SOURCE_OUT" >/dev/null 2>&1; then printf '%s\n' 'G19_SOURCE_MISMATCH=FAIL'; exit 1; fi

printf '%s\n' 'G19_CONTRACT_TEST=PASS'
