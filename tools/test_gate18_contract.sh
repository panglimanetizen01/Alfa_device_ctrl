#!/usr/bin/env bash
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID="run_20260908_121700_18000"
RUN_DIR="$ROOT/artifacts/pipeline/$RUN_ID"
G4="$RUN_DIR/gate4/environment_contract.txt"
G17="$RUN_DIR/gate17/execution.txt"
BAD="$RUN_DIR/gate17/malformed.txt"
GOOD_OUT="$RUN_DIR/gate18/good-result.txt"
BAD_OUT="$RUN_DIR/gate18/bad-result.txt"

rm -rf "$RUN_DIR"
mkdir -p "$RUN_DIR/gate4" "$RUN_DIR/gate17"

HEAD=$(git -C "$ROOT" rev-parse HEAD)
printf '%s\n' \
  "pipeline_run_id=$RUN_ID" \
  "contract_result=VALID" \
  "source_commit=$HEAD" \
  'profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' \
  > "$G4"

CONTRACT_SHA=$(sha256sum "$G4" | awk '{print $1}')
cat > "$G17" <<EOF
schema_version=gate17-runtime-execution.v1
gate=gate17
gate_status=PASS
execution_id=execution-pwd-$RUN_ID
pipeline_run_id=$RUN_ID
source_commit=$HEAD
gate4_contract_sha256=$CONTRACT_SHA
profile_sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
gate16_authorization_sha256=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
gate16_authorization_artifact=$RUN_DIR/gate16/live-authorization.txt
request_id=pwd-request-$RUN_ID
command=pwd
command_semantics=POSIX_PWD
command_sha256=b1119ecaf54feb68aa5ca8387277e0a929c71cd99fc70f1985b1de1b3b8724b6
authorization_status=AUTHORIZED
execution_status=PASS
result_status=PASS
command_result=/expected/runtime/path
command_returncode=0
command_result_sha256=cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc
created_at=2026-09-08T05:00:26Z
execution_path=/expected/runtime/path
execution_reason=G16 authorization verified; exact pwd command executed
EOF

cp "$G17" "$BAD"
sed -i '/^authorization_status=/d' "$BAD"

bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$BAD" '' "$BAD_OUT" >/dev/null
BAD_STATUS=$(awk -F= '$1=="gate_status"{print $2}' "$BAD_OUT")
[ "$BAD_STATUS" = BLOCKED ] || { printf 'G18_MALFORMED_EVIDENCE=FAIL\n'; exit 1; }

bash "$ROOT/tools/runtime_stage.sh" gate18 "$RUN_ID" "$G17" '' "$GOOD_OUT" >/dev/null
GOOD_STATUS=$(awk -F= '$1=="gate_status"{print $2}' "$GOOD_OUT")
GOOD_PATH=$(awk -F= '$1=="execution_path"{print substr($0,index($0,"=")+1)}' "$GOOD_OUT")
GOOD_RESULT=$(awk -F= '$1=="command_result"{print substr($0,index($0,"=")+1)}' "$GOOD_OUT")
[ "$GOOD_STATUS" = PASS ] || { printf 'G18_VALID_EVIDENCE=FAIL\n'; exit 1; }
[ "$GOOD_PATH" = /expected/runtime/path ] || { printf 'G18_EXECUTION_PATH_PRESERVATION=FAIL\n'; exit 1; }
[ "$GOOD_RESULT" = /expected/runtime/path ] || { printf 'G18_COMMAND_RESULT_PRESERVATION=FAIL\n'; exit 1; }

printf '%s\n' 'G18_CONTRACT_TEST=PASS'
