#!/usr/bin/env bash
# Gate 6 V1: explicit runtime control-plane bootstrap.
# This gate does not claim Linux guest execution or PRoot execution.
set -u

main() {
    local ROOT RUN_ID RUNTIME_ID REQUEST_ID REQUEST DECISION AUTH OUTPUT STATE_DIR MARKER READY_TMP IMPLEMENTATION_COMMIT RUNTIME_REGISTRY_SHA
    local CONTRACT SOURCE_COMMIT PROFILE_SHA CONTRACT_SHA DECISION_ID
    local AUTH_STATUS AUTH_DECISION_ID AUTH_REQUEST_ID AUTH_RUN AUTH_SOURCE AUTH_PROFILE AUTH_CONTRACT
    local STATUS REASON PROBE NOW MARKER_CONTENT MARKER_READ POLICY ACTION RESOURCE CONTEXT REQUEST_ID_IN_REQUEST

    ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
    RUN_ID=${1:-}
    RUNTIME_ID=${2:-}
    REQUEST_ID=${3:-}
    if [ -z "$RUN_ID" ] || [ -z "$RUNTIME_ID" ] || [ -z "$REQUEST_ID" ]; then
        printf '%s\n' 'GATE6_STATUS=BLOCKED'
        printf '%s\n' 'GATE6_REASON=explicit pipeline_run_id, runtime_id and Gate 5 request_id are required'
        return 1
    fi

    . "$ROOT/tools/runtime_chain_common.sh"
    if ! chain_runtime_supported "$ROOT" "$RUNTIME_ID"; then
        printf '%s\n' 'GATE6_STATUS=BLOCKED'
        printf '%s\n' 'GATE6_REASON=unsupported-runtime-id'
        return 1
    fi
    RUNTIME_REGISTRY_SHA=$(sha256sum "$ROOT/runtime/runtimes.v1.json" 2>/dev/null | awk '{print $1}')
    if [ -z "$RUNTIME_REGISTRY_SHA" ] || ! printf '%s' "$RUNTIME_REGISTRY_SHA" | grep -Eq '^[0-9a-f]{64}$'; then
        printf '%s\n' 'GATE6_STATUS=BLOCKED'
        printf '%s\n' 'GATE6_REASON=runtime-registry-integrity-unavailable'
        return 1
    fi

    IMPLEMENTATION_COMMIT=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '%s' 'UNKNOWN')
    REQUEST="$ROOT/artifacts/pipeline/$RUN_ID/gate5/requests/$REQUEST_ID.txt"
    DECISION="$ROOT/artifacts/pipeline/$RUN_ID/gate5/decisions/$REQUEST_ID.txt"
    AUTH="$ROOT/artifacts/pipeline/$RUN_ID/gate5/authorizations/$REQUEST_ID.txt"
    OUTPUT="$ROOT/artifacts/pipeline/$RUN_ID/gate6/bootstrap.txt"
    STATE_DIR="$ROOT/artifacts/pipeline/$RUN_ID/gate6/bootstrap-state"
    MARKER="$STATE_DIR/bootstrap.marker"

    CONTRACT=$(chain_gate4 "$ROOT" "$RUN_ID" 2>/dev/null || printf '%s' '')
    SOURCE_COMMIT=$(chain_field "$CONTRACT" source_commit 2>/dev/null || printf '%s' '')
    PROFILE_SHA=$(chain_field "$CONTRACT" profile_sha256 2>/dev/null || printf '%s' '')
    CONTRACT_SHA=$(chain_hash "$CONTRACT" 2>/dev/null || printf '%s' '')
    DECISION_ID=$(chain_field "$DECISION" decision_id 2>/dev/null || printf '%s' '')
    POLICY=$(chain_field "$REQUEST" policy_version 2>/dev/null || printf '%s' '')
    ACTION=$(chain_field "$REQUEST" action 2>/dev/null || printf '%s' '')
    RESOURCE=$(chain_field "$REQUEST" resource 2>/dev/null || printf '%s' '')
    CONTEXT=$(chain_field "$REQUEST" context 2>/dev/null || printf '%s' '')
    REQUEST_ID_IN_REQUEST=$(chain_field "$REQUEST" request_id 2>/dev/null || printf '%s' '')
    AUTH_STATUS=$(chain_field "$AUTH" authorization_status 2>/dev/null || printf '%s' '')
    AUTH_DECISION_ID=$(chain_field "$AUTH" decision_id 2>/dev/null || printf '%s' '')
    AUTH_REQUEST_ID=$(chain_field "$AUTH" request_id 2>/dev/null || printf '%s' '')
    AUTH_RUN=$(chain_field "$AUTH" pipeline_run_id 2>/dev/null || printf '%s' '')
    AUTH_SOURCE=$(chain_field "$AUTH" source_commit 2>/dev/null || printf '%s' '')
    AUTH_PROFILE=$(chain_field "$AUTH" profile_sha256 2>/dev/null || printf '%s' '')
    AUTH_CONTRACT=$(chain_field "$AUTH" gate4_contract_sha256 2>/dev/null || printf '%s' '')

    STATUS=PASS
    REASON='Gate 4 VALID, explicit production Gate 5 request ALLOW, authorization AUTHORIZED, bootstrap probe passed'
    PROBE=PASS
    NOW=$(chain_now)

    if [ -z "$CONTRACT" ] || [ ! -f "$CONTRACT" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 4 contract missing, malformed, or not VALID'
    elif [ ! -f "$REQUEST" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 request artifact missing for explicit request_id'
    elif [ ! -f "$DECISION" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 decision artifact missing for explicit request_id'
    elif [ ! -f "$AUTH" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 authorization artifact missing for explicit request_id'
    elif [ "$REQUEST_ID_IN_REQUEST" != "$REQUEST_ID" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 request_id mismatch'
    elif [ "$(chain_field "$REQUEST" runtime_id 2>/dev/null || printf '%s')" != "$RUNTIME_ID" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 runtime_id mismatch'
    elif [ "$POLICY" != 'gate5-apk-readiness-v1' ] || [ "$ACTION" != 'assembleDebug' ] || [ "$RESOURCE" != 'apk' ] || [ "$CONTEXT" != 'purpose=apk-readiness' ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 request is not the approved APK readiness policy'
    elif ! chain_identity_ok "$CONTRACT" "$DECISION"; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 decision provenance mismatch'
    elif ! chain_identity_ok "$CONTRACT" "$AUTH"; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 authorization provenance mismatch'
    elif [ "$(chain_field "$DECISION" request_id 2>/dev/null || printf '%s' '')" != "$REQUEST_ID" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 decision request_id mismatch'
    elif [ "$(chain_field "$DECISION" policy_version 2>/dev/null || printf '%s' '')" != "$POLICY" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 decision policy mismatch'
    elif [ "$(chain_field "$DECISION" decision 2>/dev/null || printf '%s' '')" != 'ALLOW' ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 decision is not ALLOW'
    elif [ "$AUTH_STATUS" != 'AUTHORIZED' ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 authorization is not AUTHORIZED'
    elif [ -z "$DECISION_ID" ] || [ "$AUTH_DECISION_ID" != "$DECISION_ID" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 decision_id mismatch'
    elif [ "$AUTH_REQUEST_ID" != "$REQUEST_ID" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 authorization request_id mismatch'
    elif [ "$AUTH_RUN" != "$RUN_ID" ] || [ "$AUTH_SOURCE" != "$SOURCE_COMMIT" ] || [ "$AUTH_PROFILE" != "$PROFILE_SHA" ] || [ "$AUTH_CONTRACT" != "$CONTRACT_SHA" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 5 authorization identity is incomplete or stale'
    elif [ "$SOURCE_COMMIT" != "$IMPLEMENTATION_COMMIT" ]; then
        STATUS=BLOCKED; PROBE=BLOCKED; REASON='Gate 6 source commit differs from current implementation commit'
    fi

    if [ "$STATUS" = 'PASS' ]; then
        if ! mkdir -p "$STATE_DIR"; then
            STATUS=ERROR; PROBE=ERROR; REASON='bootstrap state directory creation failed'
        else
            MARKER_CONTENT="pipeline_run_id=$RUN_ID|request_id=$REQUEST_ID|runtime_id=$RUNTIME_ID|runtime_registry_sha256=$RUNTIME_REGISTRY_SHA|source_commit=$SOURCE_COMMIT|profile_sha256=$PROFILE_SHA|gate4_contract_sha256=$CONTRACT_SHA|decision_id=$DECISION_ID"
            if ! printf '%s\n' "$MARKER_CONTENT" > "$MARKER"; then
                STATUS=ERROR; PROBE=ERROR; REASON='bootstrap marker write failed'
            elif ! MARKER_READ=$(cat "$MARKER" 2>/dev/null); then
                STATUS=ERROR; PROBE=ERROR; REASON='bootstrap marker read failed'
            elif [ "$MARKER_READ" != "$MARKER_CONTENT" ]; then
                STATUS=ERROR; PROBE=ERROR; REASON='bootstrap marker content mismatch'
            elif ! rm -f "$MARKER"; then
                STATUS=ERROR; PROBE=ERROR; REASON='bootstrap marker delete failed'
            elif [ -e "$MARKER" ]; then
                STATUS=ERROR; PROBE=ERROR; REASON='bootstrap marker deletion not observed'
            else
                READY_TMP="$STATE_DIR/bootstrap.ready.partial.$$"
                if ! printf '%s\n' "$MARKER_CONTENT" > "$READY_TMP"; then
                    STATUS=ERROR; PROBE=ERROR; REASON='bootstrap ready-state write failed'; rm -f "$READY_TMP" 2>/dev/null || true
                elif ! mv "$READY_TMP" "$STATE_DIR/bootstrap.ready"; then
                    STATUS=ERROR; PROBE=ERROR; REASON='bootstrap ready-state publish failed'; rm -f "$READY_TMP" 2>/dev/null || true
                fi
            fi
        fi
    fi

    mkdir -p "$(dirname -- "$OUTPUT")" 2>/dev/null || true
    {
        printf '%s\n' 'schema_version=gate6-bootstrap.v1'
        printf '%s\n' 'gate=gate6'
        printf '%s\n' "gate_status=$STATUS"
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "runtime_id=$RUNTIME_ID"
        printf '%s\n' "request_id=$REQUEST_ID"
        printf '%s\n' "runtime_registry_sha256=$RUNTIME_REGISTRY_SHA"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "implementation_commit=$IMPLEMENTATION_COMMIT"
        printf '%s\n' "gate4_contract_sha256=$CONTRACT_SHA"
        printf '%s\n' "profile_sha256=$PROFILE_SHA"
        printf '%s\n' "decision_id=$DECISION_ID"
        printf '%s\n' "authorization_status=$AUTH_STATUS"
        printf '%s\n' "bootstrap_status=$STATUS"
        printf '%s\n' "bootstrap_state_path=$STATE_DIR"
        printf '%s\n' "bootstrap_probe=$PROBE"
        printf '%s\n' "created_at=$NOW"
        printf '%s\n' "execution_path=$ROOT"
        printf '%s\n' "stage_reason=$REASON"
    } > "$OUTPUT.partial.$$" 2>/dev/null && mv "$OUTPUT.partial.$$" "$OUTPUT" 2>/dev/null

    printf '%s\n' '=== ALFA GATE 6 V1 BOOTSTRAP ==='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "runtime_id=$RUNTIME_ID"
    printf '%s\n' "request_id=$REQUEST_ID"
    printf '%s\n' "GATE6_STATUS=$STATUS"
    printf '%s\n' "GATE6_RESULT=$([ "$STATUS" = 'PASS' ] && printf '%s' 'BOOTSTRAP_PASS' || printf '%s' "$STATUS")"
    printf '%s\n' "bootstrap_status=$STATUS"
    printf '%s\n' "bootstrap_probe=$PROBE"
    printf '%s\n' "bootstrap_state_path=$STATE_DIR"
    printf '%s\n' "bootstrap_artifact=$OUTPUT"
    printf '%s\n' "bootstrap_reason=$REASON"

    [ "$STATUS" = 'PASS' ]
}

main "$@"
