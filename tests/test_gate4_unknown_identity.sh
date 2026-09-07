#!/usr/bin/env bash
set -u

TARGET="tools/environment_contract.sh"

echo "=== G4 UNKNOWN IDENTITY REGRESSION TEST ==="

if grep -Eq 'allowed_ede_value.*UNKNOWN|container_environment' "$TARGET"; then
    :
else
    echo "TEST_RESULT=RED"
    echo "RED_REASON=UNKNOWN_IDENTITY_RULE_NOT_FOUND"
    exit 1
fi

# G4 contract rule: UNKNOWN must remain UNKNOWN.
# Current validator must therefore accept UNKNOWN for
# container_environment when EDE has legitimately reported it.
if grep -A35 '^validate_ede_payload()' "$TARGET" |
   grep -q 'value.*UNKNOWN.*return 1'; then
    echo "UNKNOWN_CONTAINER_ENVIRONMENT=REJECTED"
    echo "EXPECTED=ACCEPTED_AS_UNKNOWN"
    echo "TEST_RESULT=RED"
    echo "RED_REASON=G4_REJECTS_LEGITIMATE_UNKNOWN_IDENTITY"
    exit 1
fi

echo "UNKNOWN_CONTAINER_ENVIRONMENT=ACCEPTED"
echo "EXPECTED=ACCEPTED_AS_UNKNOWN"
echo "TEST_RESULT=GREEN"
