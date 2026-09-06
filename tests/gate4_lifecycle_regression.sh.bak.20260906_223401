#!/usr/bin/env bash

set -u

ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)"
PIPELINE="$ROOT/tools/runtime_pipeline.sh"
EVIDENCE="$ROOT/artifacts/pipeline/gate4-lifecycle-regression.txt"

mkdir -p "$(dirname "$EVIDENCE")"

echo '=== G4 LIFECYCLE REGRESSION TEST ==='
echo "ROOT=$ROOT"
echo "PIPELINE=$PIPELINE"

if [ ! -f "$PIPELINE" ]; then
    echo 'TEST_RESULT=INVALID'
    echo 'TEST_REASON=PIPELINE_NOT_FOUND'
    exit 2
fi

TMP_OUTPUT="$(mktemp "${TMPDIR:-/data/data/com.termux/files/usr/tmp}/alfa-g4-regression.XXXXXX")"

if bash "$PIPELINE" >"$TMP_OUTPUT" 2>&1; then
    PIPELINE_RC=0
else
    PIPELINE_RC=$?
fi

RUN_ID=$(grep '^pipeline_run_id=' "$TMP_OUTPUT" | tail -n 1 | cut -d= -f2-)
PIPELINE_STATUS=$(grep '^pipeline_status=' "$TMP_OUTPUT" | tail -n 1 | cut -d= -f2-)

if [ -n "$RUN_ID" ] && [ -f "$ROOT/artifacts/pipeline/$RUN_ID/pipeline_state.txt" ]; then
    STATE="$ROOT/artifacts/pipeline/$RUN_ID/pipeline_state.txt"
else
    STATE=''
fi

PROFILE_RC=$(grep '^profile_rc=' "$STATE" 2>/dev/null | cut -d= -f2-)
GATE4_RC=$(grep '^gate4_rc=' "$STATE" 2>/dev/null | cut -d= -f2-)
PROFILE_FILE=$(grep '^profile_file=' "$STATE" 2>/dev/null | cut -d= -f2-)
GATE4_ARTIFACT=$(grep '^gate4_artifact=' "$STATE" 2>/dev/null | cut -d= -f2-)
STAGE_COMPLETION=$(grep '^stage_completion=' "$STATE" 2>/dev/null | cut -d= -f2-)

{
    echo '=== PIPELINE OUTPUT ==='
    cat "$TMP_OUTPUT"
    echo
    echo '=== REGRESSION OBSERVATION ==='
    echo "PIPELINE_RC=$PIPELINE_RC"
    echo "RUN_ID=$RUN_ID"
    echo "PIPELINE_STATUS=$PIPELINE_STATUS"
    echo "PROFILE_RC=${PROFILE_RC:-UNKNOWN}"
    echo "GATE4_RC=${GATE4_RC:-UNKNOWN}"
    echo "PROFILE_FILE=${PROFILE_FILE:-UNKNOWN}"
    echo "GATE4_ARTIFACT=${GATE4_ARTIFACT:-UNKNOWN}"
    echo "STAGE_COMPLETION=${STAGE_COMPLETION:-UNKNOWN}"
} | tee "$EVIDENCE"

rm -f "$TMP_OUTPUT"

if [ "$PROFILE_RC" != '0' ]; then
    echo 'PROFILE_STAGE=RED'
    echo 'TEST_RESULT=RED'
    echo 'RED_REASON=PROFILE_DID_NOT_COMPLETE_BEFORE_FINAL_PIPELINE_STATE'
    exit 1
fi

if [ "$GATE4_RC" != '0' ]; then
    echo 'GATE4_STAGE=RED'
    echo 'TEST_RESULT=RED'
    echo 'RED_REASON=GATE4_DID_NOT_COMPLETE'
    exit 1
fi

if [ "$PIPELINE_STATUS" != 'SUCCESS' ]; then
    echo 'PIPELINE_STAGE=RED'
    echo 'TEST_RESULT=RED'
    echo 'RED_REASON=PIPELINE_NOT_SUCCESS'
    exit 1
fi

if [ -z "$PROFILE_FILE" ] || [ "$PROFILE_FILE" = 'UNKNOWN' ]; then
    echo 'PROFILE_ARTIFACT=RED'
    echo 'TEST_RESULT=RED'
    echo 'RED_REASON=PROFILE_ARTIFACT_MISSING'
    exit 1
fi

if [ -z "$GATE4_ARTIFACT" ] || [ "$GATE4_ARTIFACT" = 'UNKNOWN' ]; then
    echo 'GATE4_ARTIFACT=RED'
    echo 'TEST_RESULT=RED'
    echo 'RED_REASON=GATE4_ARTIFACT_MISSING'
    exit 1
fi

echo 'PROFILE_STAGE=PASS'
echo 'GATE4_STAGE=PASS'
echo 'PIPELINE_STAGE=PASS'
echo 'TEST_RESULT=GREEN'
exit 0
