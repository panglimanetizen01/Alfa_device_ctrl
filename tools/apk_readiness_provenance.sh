#!/usr/bin/env bash
# Generate production APK-readiness Gate 5 request/decision/authorization and Gate 6.
# This is intentionally distinct from gate5_selftest.sh: it binds the build to the
# exact current source commit and exact current-run Gate 4 contract.
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RUN_ID="${1:-}"
RUNTIME_ID="${2:-debian}"
REQUEST_ID="${3:-apk-readiness}"

fail() { echo "APK_PROVENANCE_STATUS=BLOCKED"; echo "APK_PROVENANCE_REASON=$1"; exit 20; }

[ -n "$RUN_ID" ] || fail "PIPELINE_RUN_ID_REQUIRED"
[ -n "$RUNTIME_ID" ] || fail "RUNTIME_ID_REQUIRED"
[ -n "$REQUEST_ID" ] || fail "REQUEST_ID_REQUIRED"

. "$ROOT/tools/gate5_common.sh"

CONTRACT="$ROOT/artifacts/pipeline/$RUN_ID/gate4/environment_contract.txt"
RUN_SOURCE="$(git -C "$ROOT" rev-parse HEAD)"
CONTRACT_SOURCE="$(gate5_field "$CONTRACT" source_commit 2>/dev/null || true)"
PROFILE_SHA="$(gate5_field "$CONTRACT" profile_sha256 2>/dev/null || true)"
CONTRACT_SHA="$(gate5_hash "$CONTRACT" 2>/dev/null || true)"

[ -f "$CONTRACT" ] || fail "GATE4_CONTRACT_MISSING"
[ "$CONTRACT_SOURCE" = "$RUN_SOURCE" ] || fail "GATE4_SOURCE_COMMIT_MISMATCH"
gate5_valid_commit "$RUN_SOURCE" || fail "CURRENT_SOURCE_COMMIT_INVALID"
gate5_valid_sha256 "$PROFILE_SHA" || fail "GATE4_PROFILE_SHA_INVALID"
gate5_valid_sha256 "$CONTRACT_SHA" || fail "GATE4_CONTRACT_SHA_INVALID"
gate5_gate4_load "$ROOT" "$RUN_ID" >/dev/null || fail "GATE4_VALIDATION_FAILED"

case "$RUNTIME_ID" in debian) ;; *) fail "UNSUPPORTED_RUNTIME_ID" ;; esac

G5="$ROOT/artifacts/pipeline/$RUN_ID/gate5"
REQUEST="$G5/requests/$REQUEST_ID.txt"
DECISION="$G5/decisions/$REQUEST_ID.txt"
AUTH="$G5/authorizations/$REQUEST_ID.txt"
mkdir -p "$(dirname "$REQUEST")" "$(dirname "$DECISION")" "$(dirname "$AUTH")"
NOW="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
POLICY='gate5-apk-readiness-v1'
DECISION_ID="decision-$REQUEST_ID-$(printf '%s' "$CONTRACT_SHA$REQUEST_ID$RUN_SOURCE" | sha256sum | awk '{print substr($1,1,16)}')"

cat > "$REQUEST.tmp" <<EOF
schema_version=gate5-request.v1
request_id=$REQUEST_ID
pipeline_run_id=$RUN_ID
runtime_id=$RUNTIME_ID
source_commit=$RUN_SOURCE
gate4_contract_sha256=$CONTRACT_SHA
profile_sha256=$PROFILE_SHA
subject=android-build
action=assembleDebug
resource=apk
context=purpose=apk-readiness
policy_version=$POLICY
created_at=$NOW
EOF
mv "$REQUEST.tmp" "$REQUEST"

cat > "$DECISION.tmp" <<EOF
schema_version=gate5-decision.v1
decision_id=$DECISION_ID
request_id=$REQUEST_ID
pipeline_run_id=$RUN_ID
runtime_id=$RUNTIME_ID
source_commit=$RUN_SOURCE
gate4_contract_sha256=$CONTRACT_SHA
profile_sha256=$PROFILE_SHA
policy_version=$POLICY
decision=ALLOW
decision_reason=Gate 4 VALID and current canonical source identity matched APK readiness policy
created_at=$NOW
EOF
mv "$DECISION.tmp" "$DECISION"

# Reuse the existing authorization enforcement; it independently revalidates
# Gate 4, identity, freshness and decision=ALLOW.
bash "$ROOT/tools/runtime_execution_authorize.sh" "$REQUEST" "$DECISION" "$AUTH" >/tmp/alfa-apk-g5-auth.$$.out

grep -Fx 'authorization_status=AUTHORIZED' "$AUTH" >/dev/null || fail "GATE5_AUTHORIZATION_DENIED"
grep -Fx "request_id=$REQUEST_ID" "$AUTH" >/dev/null || fail "GATE5_REQUEST_ID_MISMATCH"
grep -Fx "pipeline_run_id=$RUN_ID" "$AUTH" >/dev/null || fail "GATE5_RUN_ID_MISMATCH"
grep -Fx "source_commit=$RUN_SOURCE" "$AUTH" >/dev/null || fail "GATE5_SOURCE_COMMIT_MISMATCH"

authorization_policy="$(gate5_field "$REQUEST" policy_version)"
[ "$authorization_policy" = "$POLICY" ] || fail "GATE5_POLICY_MISMATCH"

grep -Fx "runtime_id=$RUNTIME_ID" "$REQUEST" >/dev/null || fail "GATE5_RUNTIME_ID_MISSING"

grep -Fx "runtime_id=$RUNTIME_ID" "$DECISION" >/dev/null || fail "GATE5_DECISION_RUNTIME_ID_MISSING"

bash "$ROOT/tools/runtime_bootstrap.sh" "$RUN_ID" "$REQUEST_ID" >/tmp/alfa-apk-g6.$$.out
GATE6="$ROOT/artifacts/pipeline/$RUN_ID/gate6/bootstrap.txt"
[ -f "$GATE6" ] || fail "GATE6_BOOTSTRAP_MISSING"
grep -Fx 'gate_status=PASS' "$GATE6" >/dev/null || fail "GATE6_NOT_PASS"
grep -Fx 'bootstrap_status=PASS' "$GATE6" >/dev/null || fail "GATE6_BOOTSTRAP_NOT_PASS"
grep -Fx "request_id=$REQUEST_ID" "$GATE6" >/dev/null || fail "GATE6_REQUEST_ID_MISMATCH"
grep -Fx "runtime_id=$RUNTIME_ID" "$GATE6" >/dev/null || fail "GATE6_RUNTIME_ID_MISMATCH"
grep -Fx "source_commit=$RUN_SOURCE" "$GATE6" >/dev/null || fail "GATE6_SOURCE_COMMIT_MISMATCH"
grep -Fx 'authorization_status=AUTHORIZED' "$GATE6" >/dev/null || fail "GATE6_NOT_AUTHORIZED"

rm -f /tmp/alfa-apk-g5-auth.$$ /tmp/alfa-apk-g6.$$ || true

echo 'APK_PROVENANCE_STATUS=PASS'
echo "PIPELINE_RUN_ID=$RUN_ID"
echo "REQUEST_ID=$REQUEST_ID"
echo "RUNTIME_ID=$RUNTIME_ID"
echo "SOURCE_COMMIT=$RUN_SOURCE"
echo "GATE4_CONTRACT_SHA256=$CONTRACT_SHA"
echo "PROFILE_SHA256=$PROFILE_SHA"
echo "GATE5_REQUEST=$REQUEST"
echo "GATE5_DECISION=$DECISION"
echo "GATE5_AUTHORIZATION=$AUTH"
echo "GATE6_BOOTSTRAP=$GATE6"
