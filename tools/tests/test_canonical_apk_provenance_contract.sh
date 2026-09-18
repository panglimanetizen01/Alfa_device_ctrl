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

if grep -R -n -F "PRODUCER_RUN_ID:" "$WORKFLOW_DIR"; then
  echo "HARDCODED_PRODUCER_REFERENCE_FOUND"
  exit 1
fi

CANONICAL_NAME_COUNT="$(grep -R -l -F 'name: alfa-device-ctrl-stitch-' "$WORKFLOW_DIR" | wc -l)"
test "$CANONICAL_NAME_COUNT" -eq 1

grep -Fq 'name: Alfa APK Readiness' "$CANONICAL_WORKFLOW"
grep -Fq 'actions/upload-artifact' "$CANONICAL_WORKFLOW"
grep -Fq 'artifact_policy=ONE_APK_PLUS_ONE_PROVENANCE_MANIFEST' "$CANONICAL_WORKFLOW"

grep -Fq 'workflows: ["Alfa APK Readiness"]' "$CANONICAL_CONSUMER"
grep -Fq 'actions/download-artifact' "$CANONICAL_CONSUMER"
grep -Fq 'run-id: ${{ env.PRODUCER_RUN_ID }}' "$CANONICAL_CONSUMER"
grep -Fq 'apk-artifact-provenance.txt' "$CANONICAL_CONSUMER"
grep -Fq 'workflow_run_id=' "$CANONICAL_CONSUMER

echo "CANONICAL_APK_PRODUCER_COUNT=1"
echo "CANONICAL_APK_PROVENANCE_POLICY=PASS"
