#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID="run_20260908_121700_18000"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G17="$RUN_DIR/gate17/execution.txt"
G16="$RUN_DIR/gate16/live-authorization.txt"
GOOD_OUT="$RUN_DIR/gate18/good-result.txt"
BAD_OUT="$RUN_DIR/gate18/bad-result.txt"
CROSS_OUT="$RUN_DIR/gate18/cross-run.txt"
HASH_OUT="$RUN_DIR/gate18/hash-mismatch.txt"

rm -rf "$RUN_DIR"
mkdir -p "$RUN_DIR/gate4" "$RUN_DIR/gate16" "$RUN_DIR/gate17"

HEAD=$(git -C "$ROOT" rev-parse HEAD)
printf '%s\n' "pipeline_run_id=$RUN_ID" 'contract_result=VALID' "source_commit=$HEAD" 'profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' > "$G4"
printf '%s\n' 'authorization_status=AUTHORIZED' > "$G16"

CONTRACT_SHA=$(sha256sum "$G4" | awk '{print $1}')
G16_SHA=$(sha256sum "$G16" | awk '{print $1}')
cat > "$G17" <<EOF
schema_version=gate17-runtime-execution.v1
gate=gate17
gate_status=PASS
execution_id=execution-pwd-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$CONTRACT_SHA
profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
gate16_authorization_sha256=$G16_SHA
gate16_authorization_artifact=$G16
request_id=pwd-request-$RUN_ID
command=pwd
command_semantics=POSIX_PWD
command_sha256=b1119ecaf54feb68aa5ca8387277e0a929c71cd99fc70f1985b1de1b3b8724b6
authorization_status=AUTHORIZED
execution_status=PASS
result_status=PASS
command_result=/expected/runtime/path
command_returncode=0
command_result_sha256=e7748a2b5e00e724562c9d7688fa9dc1aecbf20dba00096d6e2d91bf37696449
created_at=2026-09-08T05:00:26Z
execution_path=/expected/runtime/path
execution_reason=G16 authorization verified; exact pwd command executed
EOF

cp "$G17" "$RUN_DIR/gate17/malformed.txt"
sed -i '/^authorization_status=/d' "$RUN_DIR/gate17/malformed.txt"
bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$RUN_DIR/gate17/malformed.txt" '' "$BAD_OUT" >/dev/null
[ "$(awk -F= '$1=="gate_status"{print $2}' "$BAD_OUT")" = BLOCKED ] || { printf '%s\n' 'G18_MALFORMED_EVIDENCE=FAIL'; exit 1; }

cp "$G17" "$RUN_DIR/gate17/cross-run.txt"
sed -i "s/^execution_id=.*/execution_id=execution-pwd-run_wrong/; s/^request_id=.*/request_id=pwd-request-run_wrong/" "$RUN_DIR/gate17/cross-run.txt"
bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$RUN_DIR/gate17/cross-run.txt" '' "$CROSS_OUT" >/dev/null
[ "$(awk -F= '$1=="gate_status"{print $2}' "$CROSS_OUT")" = BLOCKED ] || { printf '%s\n' 'G18_CROSS_RUN=FAIL'; exit 1; }

cp "$G17" "$RUN_DIR/gate17/hash-mismatch.txt"
sed -i 's/^command_result_sha256=.*/command_result_sha256=ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff/' "$RUN_DIR/gate17/hash-mismatch.txt"
bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$RUN_DIR/gate17/hash-mismatch.txt" '' "$HASH_OUT" >/dev/null
[ "$(awk -F= '$1=="gate_status"{print $2}' "$HASH_OUT")" = BLOCKED ] || { printf '%s\n' 'G18_HASH_MISMATCH=FAIL'; exit 1; }

bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$G17" '' "$GOOD_OUT" >/dev/null
[ "$(awk -F= '$1=="gate_status"{print $2}' "$GOOD_OUT")" = PASS ] || { printf '%s\n' 'G18_VALID_EVIDENCE=FAIL'; exit 1; }
for field in execution_id request_id command_semantics command_sha256 authorization_status execution_status command_result command_returncode command_result_sha256 gate16_authorization_sha256 gate16_authorization_artifact; do
    [ "$(awk -F= -v k="$field" '$1==k{print substr($0,index($0,"=")+1)}' "$GOOD_OUT")" = "$(awk -F= -v k="$field" '$1==k{print substr($0,index($0,"=")+1)}' "$G17")" ] || { printf '%s\n' "G18_${field}_PRESERVATION=FAIL"; exit 1; }
done
[ "$(awk -F= '$1=="execution_path"{print substr($0,index($0,"=")+1)}' "$GOOD_OUT")" = /expected/runtime/path ] || { printf '%s\n' 'G18_PATH_PRESERVATION=FAIL'; exit 1; }
[ "$(awk -F= '$1=="created_at"{print substr($0,index($0,"=")+1)}' "$GOOD_OUT")" = 2026-09-08T05:00:26Z ] || { printf '%s\n' 'G18_TIMESTAMP_PRESERVATION=FAIL'; exit 1; }
[ "$(awk -F= '$1=="result_status"{print $2}' "$GOOD_OUT")" = PASS ] || { printf '%s\n' 'G18_RESULT_STATUS=FAIL'; exit 1; }
printf '%s\n' 'G18_CONTRACT_TEST=PASS'
