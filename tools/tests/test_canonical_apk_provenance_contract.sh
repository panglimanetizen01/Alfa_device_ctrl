#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_DIR=".github/workflows"
CANONICAL_WORKFLOW="$WORKFLOW_DIR/apk-readiness.yml"
CANONICAL_CONSUMER="$WORKFLOW_DIR/g2-four-session-lifecycle-instrumentation.yml"

test -f "$CANONICAL_WORKFLOW"
test -f "$CANONICAL_CONSUMER"

for stale in \
  ".github/workflows/g2-canonical-artifact-dut.yml" \
  ".github/workflows/production-apk-provenance-ci.yml" \
  ".github/workflows/production-transport-bridge-ci.yml"; do
  test ! -e "$stale"
done

for literal in \
  "97a1e5a8e3ce5d2de63508af0c57de2dd0873117" \
  "97ea316ecc260d821941c9fe8555ee7cdad1e6b6" \
  "10539809915" \
  "f53f4ddb17de3bdeaf2125b87c0c13697674a19dcc343a97d6b13d37a515555d"; do
  if grep -R -F -n --exclude-dir=.git "$literal" "$WORKFLOW_DIR"; then
    echo "LEGACY_CANONICAL_REFERENCE_FOUND=$literal"
    exit 1
  fi
done

assert_dynamic_producer_run_id() {
  local file="$1"
  if grep -Eq 'PRODUCER_RUN_ID:[[:space:]].*[0-9]{6,}' "$file"; then
    echo "HARDCODED_PRODUCER_REFERENCE_FOUND=$file"
    grep -En 'PRODUCER_RUN_ID:[[:space:]].*[0-9]{6,}' "$file"
    return 1
  fi
  grep -Eq 'PRODUCER_RUN_ID:[[:space:]].*github[.]event[.]workflow_run[.]id' "$file"
}

assert_dynamic_producer_run_id "$CANONICAL_CONSUMER"

assert_dynamic_source_identity() {
  local file="$1"
  grep -Eq 'ALFA_SOURCE_COMMIT:[[:space:]].*github[.]event[.]pull_request[.]head[.]sha' "$file"
}
assert_dynamic_source_identity "$CANONICAL_WORKFLOW"

TMP_CASE_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_CASE_DIR"' EXIT

printf '%s\\n' 'env:' '  PRODUCER_RUN_ID: github.event.workflow_run.id' > "$TMP_CASE_DIR/dynamic.yml"
assert_dynamic_producer_run_id "$TMP_CASE_DIR/dynamic.yml"
echo "TEST_DYNAMIC_PRODUCER_RUN_ID=PASS"

printf '%s\\n' 'env:' '  PRODUCER_RUN_ID: 35300121723' > "$TMP_CASE_DIR/hardcoded-run.yml"
if assert_dynamic_producer_run_id "$TMP_CASE_DIR/hardcoded-run.yml"; then
  echo "TEST_HARDCODED_PRODUCER_RUN_ID=FAIL"
  exit 1
else
  echo "TEST_HARDCODED_PRODUCER_RUN_ID=FAIL_AS_EXPECTED"
fi

printf '%s\\n' 'env:' '  SOURCE_COMMIT: 97a1e5a8e3ce5d2de63508af0c57de2dd0873117' > "$TMP_CASE_DIR/legacy-sha-a.yml"
if grep -Fq '97a1e5a8e3ce5d2de63508af0c57de2dd0873117' "$TMP_CASE_DIR/legacy-sha-a.yml"; then
  echo "TEST_LEGACY_SHA_97A1=FAIL_AS_EXPECTED"
else
  echo "TEST_LEGACY_SHA_97A1=FAIL"
  exit 1
fi

printf '%s\\n' 'env:' '  SOURCE_COMMIT: 97ea316ecc260d821941c9fe8555ee7cdad1e6b6' > "$TMP_CASE_DIR/legacy-sha-b.yml"
if grep -Fq '97ea316ecc260d821941c9fe8555ee7cdad1e6b6' "$TMP_CASE_DIR/legacy-sha-b.yml"; then
  echo "TEST_LEGACY_SHA_97EA=FAIL_AS_EXPECTED"
else
  echo "TEST_LEGACY_SHA_97EA=FAIL"
  exit 1
fi
echo "TEST_DYNAMIC_WORKFLOW_SOURCE_IDENTITY=PASS"

CANONICAL_NAME_COUNT="$(grep -R -l -F 'name: alfa-device-ctrl-stitch-' "$WORKFLOW_DIR" | wc -l)"
test "$CANONICAL_NAME_COUNT" -eq 1

grep -Fq 'name: Alfa APK Readiness' "$CANONICAL_WORKFLOW"
grep -Fq 'actions/upload-artifact' "$CANONICAL_WORKFLOW"
grep -Fq 'artifact_policy=ONE_APK_PLUS_ONE_PROVENANCE_MANIFEST' "$CANONICAL_WORKFLOW"

grep -Fq 'workflows: ["Alfa APK Readiness"]' "$CANONICAL_CONSUMER"
grep -Fq 'actions/download-artifact' "$CANONICAL_CONSUMER"
grep -Fq 'run-id: ${{ env.PRODUCER_RUN_ID }}' "$CANONICAL_CONSUMER"
grep -Fq 'apk-artifact-provenance.txt' "$CANONICAL_CONSUMER"
grep -Fq 'workflow_run_id=' "$CANONICAL_CONSUMER"

echo "CANONICAL_APK_PRODUCER_COUNT=1"
echo "CANONICAL_APK_PROVENANCE_POLICY=PASS"
