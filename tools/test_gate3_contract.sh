#!/usr/bin/env bash

set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
CAP="$ROOT/tools/execution_capability.sh"
VAL="$ROOT/tools/gate3_execution_capability.sh"

TMP=$(mktemp -d 2>/dev/null || printf '%s/g3-test.%s' "${TMPDIR:-$HOME}" "$$")
cleanup() { rm -rf "$TMP" 2>/dev/null || true; }
trap cleanup EXIT

mkdir -p "$TMP/shared" "$TMP/repo/artifacts/gates/g2" "$TMP/repo/tools" || exit 1
cp "$CAP" "$TMP/repo/tools/execution_capability.sh" || exit 1
cp "$VAL" "$TMP/repo/tools/gate3_execution_capability.sh" || exit 1
chmod +x "$TMP/repo/tools/execution_capability.sh" "$TMP/repo/tools/gate3_execution_capability.sh" || exit 1

cd "$TMP/repo" || exit 1
git init -q
git config user.email test@example.invalid
git config user.name G3-Test
printf '%s\n' 'g3-test' > marker.txt
git add marker.txt tools
git commit -qm 'g3 test base'
SOURCE=$(git rev-parse HEAD)
printf '%s\n' \
  'schema_version=g2-canonical-source-build-boundary.v1' \
  'gate=G2' \
  'gate_status=GREEN' \
  "source_commit=$SOURCE" \
  > artifacts/gates/g2/canonical-source-build-boundary.txt

ALFA_EXEC_SHARED_ROOT="$TMP/shared" bash tools/execution_capability.sh > "$TMP/pass.txt" 2>&1
PASS_RC=$?
grep -q '^EXEC_PRIVATE=PASS ' "$TMP/pass.txt" || exit 1
grep -q '^EXEC_SHARED=PASS ' "$TMP/pass.txt" || exit 1
grep -q '^SCRIPT_BASH=PASS ' "$TMP/pass.txt" || exit 1
grep -q '^SCRIPT_PYTHON=PASS ' "$TMP/pass.txt" || exit 1
grep -q '^PROCESS_SPAWN=PASS ' "$TMP/pass.txt" || exit 1
grep -q '^GATE3_STATUS=PASS$' "$TMP/pass.txt" || exit 1
[ "$PASS_RC" -eq 0 ] || exit 1

if ALFA_EXEC_SHARED_ROOT="$TMP/does-not-exist" bash tools/execution_capability.sh > "$TMP/fail.txt" 2>&1; then
    exit 1
fi
grep -q '^EXEC_SHARED=ERROR ' "$TMP/fail.txt" || exit 1
grep -q '^GATE3_STATUS=ERROR$' "$TMP/fail.txt" || exit 1

if G3_EXPECT_SOURCE_COMMIT=0000000000000000000000000000000000000000 \
   ALFA_EXEC_SHARED_ROOT="$TMP/shared" bash tools/gate3_execution_capability.sh > "$TMP/stale.txt" 2>&1; then
    exit 1
fi
grep -q '^G3_REASON=STALE_EXPECTED_SOURCE_COMMIT$' "$TMP/stale.txt" || exit 1

ALFA_EXEC_SHARED_ROOT="$TMP/shared" bash tools/gate3_execution_capability.sh > "$TMP/validator.txt" 2>&1
VALIDATOR_RC=$?
[ "$VALIDATOR_RC" -eq 0 ] || exit 1
grep -q '^G3_STATUS=GREEN$' "$TMP/validator.txt" || exit 1
grep -q '^gate_status=GREEN$' artifacts/gates/g3/execution-capability.txt || exit 1
grep -q "^source_commit=$SOURCE$" artifacts/gates/g3/execution-capability.txt || exit 1

echo 'G3_CONTRACT_TEST=PASS'
echo 'NEGATIVE_STALE_SOURCE=PASS'
echo 'NEGATIVE_CAPABILITY_FAILURE=PASS'
echo 'VALIDATOR_GREEN=PASS'
