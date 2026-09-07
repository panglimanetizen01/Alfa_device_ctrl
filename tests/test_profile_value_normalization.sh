#!/usr/bin/env bash

set -u

PROFILE_VALUE_FUNCTIONS="$(
    awk '
        /^trim_value\(\)/ { capture=1 }
        /^path_is_safe_relative\(\)/ { capture=0 }
        capture { print }
    ' tools/runtime_profile_generator.sh
)"

FIXTURE="$(mktemp)"
trap 'rm -f "$FIXTURE"' EXIT

cat > "$FIXTURE" <<'FIXTURE_EOF'
STORAGE_READ=PASS | verification=Read test | scope=current_environment
EXEC_SHARED=ERROR | verification=execution test
FIXTURE_EOF

RESULT="$(
    {
        printf '%s\n' "$PROFILE_VALUE_FUNCTIONS"
        printf '%s\n' 'printf "STORAGE_READ_RESULT=[%s]\n" "$(profile_value STORAGE_READ "$1")"'
        printf '%s\n' 'printf "EXEC_SHARED_RESULT=[%s]\n" "$(profile_value EXEC_SHARED "$1")"'
    } | bash -s -- "$FIXTURE"
)"

printf '%s\n' '=== PROFILE VALUE NORMALIZATION RED TEST ==='
printf '%s\n' "$RESULT"

STORAGE_RESULT="$(printf '%s\n' "$RESULT" | sed -n 's/^STORAGE_READ_RESULT=\[\(.*\)\]$/\1/p')"
EXEC_RESULT="$(printf '%s\n' "$RESULT" | sed -n 's/^EXEC_SHARED_RESULT=\[\(.*\)\]$/\1/p')"

printf 'EXPECTED_STORAGE=[PASS]\n'
printf 'EXPECTED_EXEC_SHARED=[ERROR]\n'

if [ "$STORAGE_RESULT" = 'PASS' ] && [ "$EXEC_RESULT" = 'ERROR' ]; then
    printf '%s\n' 'TEST_RESULT=GREEN'
    exit 0
fi

printf '%s\n' 'TEST_RESULT=RED'
printf '%s\n' 'RED_REASON=PROFILE_VALUE_RETURNS_COMPOSITE_CAPABILITY_STATUS'
exit 1
