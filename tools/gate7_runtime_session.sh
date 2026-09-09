#!/usr/bin/env bash
# Gate 7 V1: verify an Android-produced runtime session attestation for one exact Gate 6 run.
# This verifier never creates a session and never promotes Gate 6 bootstrap into session PASS.
set -u

ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
RUN_ID=${1:-}
BOOTSTRAP=${2:-}
EVIDENCE=${3:-}
OUTPUT="$ROOT/artifacts/pipeline/${RUN_ID}/gate7/session.txt"

fail() {
    local reason=$1
    printf '%s\n' "GATE7_STATUS=BLOCKED"
    printf '%s\n' "GATE7_RESULT=SESSION_BLOCKED"
    printf '%s\n' "GATE7_FAILURE_REASON=$reason"
    return 1
}

field() {
    local file=$1 key=$2
    awk -F= -v k="$key" '$1 == k {sub(/^[^=]*=/, ""); print; found=1; exit} END {if (!found) exit 1}' "$file" 2>/dev/null
}

is_sha256() { printf '%s' "$1" | grep -Eq '^[0-9a-f]{64}$'; }
is_commit() { printf '%s' "$1" | grep -Eq '^[0-9a-f]{40}$'; }
is_token() { printf '%s' "$1" | grep -Eq '^[A-Za-z0-9._:-]+$'; }

main() {
    local STATUS=PASS REASON='validated Android runtime session attestation'
    local CURRENT_HEAD SOURCE_COMMIT PROFILE_SHA CONTRACT_SHA IMPLEMENTATION_COMMIT
    local BOOT_RUN BOOT_STATUS BOOT_SOURCE BOOT_PROFILE BOOT_CONTRACT
    local E_SCHEMA E_SESSION E_REQUEST E_RUN E_RUNTIME E_SOURCE E_PROFILE E_CONTRACT E_IMPL
    local E_STATE E_RESULT E_PID E_PTY E_PROMPT E_ENGINE E_ROOTFS E_READY

    [ -n "$RUN_ID" ] || { fail 'explicit pipeline_run_id is required'; return 1; }
    [ -f "$BOOTSTRAP" ] || { fail 'Gate 6 bootstrap artifact is missing'; return 1; }
    [ -f "$EVIDENCE" ] || { fail 'Android session evidence artifact is missing'; return 1; }
    is_token "$RUN_ID" || { fail 'invalid pipeline_run_id'; return 1; }

    CURRENT_HEAD=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || printf '')
    BOOT_RUN=$(field "$BOOTSTRAP" pipeline_run_id || printf '')
    BOOT_STATUS=$(field "$BOOTSTRAP" bootstrap_status || printf '')
    BOOT_SOURCE=$(field "$BOOTSTRAP" source_commit || printf '')
    BOOT_PROFILE=$(field "$BOOTSTRAP" profile_sha256 || printf '')
    BOOT_CONTRACT=$(field "$BOOTSTRAP" gate4_contract_sha256 || printf '')
    SOURCE_COMMIT="$BOOT_SOURCE"
    PROFILE_SHA="$BOOT_PROFILE"
    CONTRACT_SHA="$BOOT_CONTRACT"
    IMPLEMENTATION_COMMIT=$(field "$BOOTSTRAP" implementation_commit || printf '')

    E_SCHEMA=$(field "$EVIDENCE" schema_version || printf '')
    E_SESSION=$(field "$EVIDENCE" session_id || printf '')
    E_REQUEST=$(field "$EVIDENCE" request_id || printf '')
    E_RUN=$(field "$EVIDENCE" pipeline_run_id || printf '')
    E_RUNTIME=$(field "$EVIDENCE" runtime_id || printf '')
    E_SOURCE=$(field "$EVIDENCE" source_commit || printf '')
    E_PROFILE=$(field "$EVIDENCE" profile_sha256 || printf '')
    E_CONTRACT=$(field "$EVIDENCE" gate4_contract_sha256 || printf '')
    E_IMPL=$(field "$EVIDENCE" implementation_commit || printf '')
    E_STATE=$(field "$EVIDENCE" state || printf '')
    E_RESULT=$(field "$EVIDENCE" result || printf '')
    E_PID=$(field "$EVIDENCE" process_pid || printf '')
    E_PTY=$(field "$EVIDENCE" pty_status || printf '')
    E_PROMPT=$(field "$EVIDENCE" prompt_observed || printf '')
    E_ENGINE=$(field "$EVIDENCE" engine_path || printf '')
    E_ROOTFS=$(field "$EVIDENCE" rootfs_path || printf '')
    E_READY=$(field "$EVIDENCE" runtime_evidence || printf '')

    if [ "$BOOT_RUN" != "$RUN_ID" ] || [ "$BOOT_STATUS" != 'PASS' ]; then
        STATUS=BLOCKED; REASON='Gate 6 bootstrap is not PASS for the requested run'
    elif ! is_commit "$CURRENT_HEAD" || [ "$SOURCE_COMMIT" != "$CURRENT_HEAD" ]; then
        STATUS=BLOCKED; REASON='Gate 6 source_commit is stale relative to current HEAD'
    elif ! is_commit "$SOURCE_COMMIT" || ! is_sha256 "$PROFILE_SHA" || ! is_sha256 "$CONTRACT_SHA" || ! is_commit "$IMPLEMENTATION_COMMIT"; then
        STATUS=BLOCKED; REASON='Gate 6 provenance fields are missing or malformed'
    elif [ "$E_SCHEMA" != 'operation-evidence.v1' ]; then
        STATUS=BLOCKED; REASON='session evidence schema is missing or unsupported'
    elif ! is_token "$E_SESSION" || ! is_token "$E_REQUEST" || [ "$E_RUN" != "$RUN_ID" ]; then
        STATUS=BLOCKED; REASON='session identity is missing or cross-run'
    elif [ "$E_RUNTIME" != 'debian' ]; then
        STATUS=BLOCKED; REASON='session runtime_id is not the Debian acceptance runtime'
    elif [ "$E_SOURCE" != "$SOURCE_COMMIT" ] || [ "$E_PROFILE" != "$PROFILE_SHA" ] || [ "$E_CONTRACT" != "$CONTRACT_SHA" ] || ! is_commit "$E_IMPL"; then
        STATUS=BLOCKED; REASON='session evidence provenance does not match Gate 6'
    elif [ "$E_STATE" != 'READY' ] || [ "$E_RESULT" != 'PROMPT_OBSERVED' ]; then
        STATUS=BLOCKED; REASON='session did not reach verified READY state'
    elif ! printf '%s' "$E_PID" | grep -Eq '^[1-9][0-9]*$'; then
        STATUS=BLOCKED; REASON='session process PID is missing or invalid'
    elif [ "$E_PTY" != 'PASS' ] || [ "$E_PROMPT" != 'PASS' ]; then
        STATUS=BLOCKED; REASON='PTY/session readiness evidence is incomplete'
    elif [ -z "$E_ENGINE" ] || [ -z "$E_ROOTFS" ] || [ -z "$E_READY" ]; then
        STATUS=BLOCKED; REASON='runtime execution identity paths are incomplete'
    fi

    mkdir -p "$(dirname -- "$OUTPUT")" 2>/dev/null || true
    if [ "$STATUS" = 'PASS' ]; then
        {
            printf '%s\n' 'schema_version=gate7-runtime-session.v1'
            printf '%s\n' 'gate=gate7'
            printf '%s\n' "gate_status=$STATUS"
            printf '%s\n' "session_status=$STATUS"
            printf '%s\n' "pipeline_run_id=$RUN_ID"
            printf '%s\n' "source_commit=$SOURCE_COMMIT"
            printf '%s\n' "implementation_commit=$E_IMPL"
            printf '%s\n' "gate4_contract_sha256=$CONTRACT_SHA"
            printf '%s\n' "profile_sha256=$PROFILE_SHA"
            printf '%s\n' "session_id=$E_SESSION"
            printf '%s\n' "request_id=$E_REQUEST"
            printf '%s\n' "runtime_id=$E_RUNTIME"
            printf '%s\n' "process_pid=$E_PID"
            printf '%s\n' "pty_status=$E_PTY"
            printf '%s\n' "prompt_observed=$E_PROMPT"
            printf '%s\n' "engine_path=$E_ENGINE"
            printf '%s\n' "rootfs_path=$E_ROOTFS"
            printf '%s\n' "runtime_evidence=$E_READY"
            printf '%s\n' "source_evidence=$EVIDENCE"
            printf '%s\n' "stage_reason=$REASON"
        } > "$OUTPUT.partial.$$" 2>/dev/null && mv "$OUTPUT.partial.$$" "$OUTPUT" 2>/dev/null
    fi

    printf '%s\n' '=== ALFA GATE 7 V1 RUNTIME SESSION ==='
    printf '%s\n' "pipeline_run_id=$RUN_ID"
    printf '%s\n' "GATE7_STATUS=$STATUS"
    printf '%s\n' "GATE7_RESULT=$([ "$STATUS" = 'PASS' ] && printf '%s' 'SESSION_PASS' || printf '%s' 'SESSION_BLOCKED')"
    printf '%s\n' "session_status=$STATUS"
    printf '%s\n' "session_id=$E_SESSION"
    printf '%s\n' "process_pid=$E_PID"
    printf '%s\n' "stage_reason=$REASON"

    [ "$STATUS" = 'PASS' ]
}

main "$@"
