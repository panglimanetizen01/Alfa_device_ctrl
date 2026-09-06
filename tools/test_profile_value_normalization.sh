#!/usr/bin/env bash

ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)"
PROFILE="$ROOT/tools/runtime_profile_generator.sh"
TMP_DIR="$ROOT/artifacts/tmp"
FIXTURE="$TMP_DIR/profile_value_fixture.$$"

mkdir -p "$TMP_DIR"

printf '%s\n' \
  'STORAGE_READ=PASS | verification=Read test | scope=current_environment' \
  'EXEC_SHARED=ERROR | verification=Execution test' \
  > "$FIXTURE"

PROFILE_FUNCTIONS="$(
    sed -n '29,78p' "$PROFILE"
)"

RESULT="$(
    {
        printf '%s\n' "$PROFILE_FUNCTIONS"
        printf '%s\n' 'printf "STORAGE_READ_RESULT=[%s]\n" "$(profile_value STORAGE_READ "$1")"'
        printf '%s\n' 'printf "EXEC_SHARED_RESULT=[%s]\n" "$(profile_value EXEC_SHARED "$1")"'
    } | bash -s -- "$FIXTURE"
)"

printf '%s\n' '=== PROFILE VALUE NORMALIZATION RED TEST ==='
printf '%s\n' "$RESULT"
printf '%s\n' 'EXPECTED_STORAGE=[PASS]'
printf '%s\n' 'EXPECTED_EXEC_SHARED=[ERROR]'

STORAGE_RESULT="$(printf '%s\n' "$RESULT" | sed -n 's/^STORAGE_READ_RESULT=\[\(.*\)\]$/\1/p')"
EXEC_RESULT="$(printf '%s\n' "$RESULT" | sed -n 's/^EXEC_SHARED_RESULT=\[\(.*\)\]$/\1/p')"

if [ "$STORAGE_RESULT" = 'PASS' ] && [ "$EXEC_RESULT" = 'ERROR' ]; then
    printf '%s\n' 'TEST_RESULT=GREEN_UNEXPECTED'
else
    printf '%s\n' 'TEST_RESULT=RED'
    printf '%s\n' 'RED_REASON=PROFILE_VALUE_RETURNS_COMPOSITE_CAPABILITY_STATUS'
fi

rm -f "$FIXTURE"
