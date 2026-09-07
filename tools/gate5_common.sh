#!/usr/bin/env bash
# Shared Gate 5 V1 helpers. No newest-artifact discovery is permitted here.

gate5_root() {
    CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")"/.. 2>/dev/null && pwd
}

gate5_field() {
    local FILE KEY VALUE
    FILE=${1:-}
    KEY=${2:-}
    [ -f "$FILE" ] || return 1
    VALUE=$(awk -F= -v key="$KEY" '$1 == key { value=substr($0, index($0,"=")+1); count++ } END { if (count == 1) print value }' "$FILE")
    [ -n "$VALUE" ] || return 1
    printf '%s\n' "$VALUE"
}

gate5_valid_token() {
    case "${1:-}" in
        ''|*[!A-Za-z0-9_.-]*) return 1 ;;
        *) return 0 ;;
    esac
}

gate5_valid_sha256() {
    case "${1:-}" in
        ''|*[!0-9a-fA-F]*) return 1 ;;
        *) [ "${#1}" -eq 64 ] ;;
    esac
}

gate5_valid_commit() {
    case "${1:-}" in
        ''|*[!0-9a-fA-F]*) return 1 ;;
        *) [ "${#1}" -eq 40 ] ;;
    esac
}

gate5_hash() {
    sha256sum "$1" 2>/dev/null | awk '{print $1}'
}

gate5_now() {
    date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' 'UNKNOWN'
}

gate5_fresh() {
    local VALUE WHEN NOW AGE
    VALUE=${1:-}
    WHEN=$(date -u -d "$VALUE" '+%s' 2>/dev/null) || return 1
    NOW=$(date -u '+%s' 2>/dev/null) || return 1
    AGE=$((NOW - WHEN))
    [ "$AGE" -ge 0 ] && [ "$AGE" -le 300 ]
}

gate5_gate4_load() {
    local ROOT RUN CONTRACT ENVELOPE EXPECTED_RUN EXPECTED_RESULT SOURCE_COMMIT PROFILE_SHA PROFILE_PATH
    local CONTRACT_SCHEMA PROJECT_ID PROJECT_ROOT COMPLETION FAILURE_STATUS FAILURE_REASON
    local PROFILE_NAME PROFILE_CANONICAL AUTHORITY_PATH AUTHORITY_NAME
    ROOT=${1:-}
    RUN=${2:-}
    [ -n "$ROOT" ] && [ -n "$RUN" ] || return 1
    CONTRACT="$ROOT/artifacts/pipeline/$RUN/gate4/environment_contract.txt"
    ENVELOPE="$ROOT/artifacts/pipeline/$RUN/gate4/environment_contract.envelope.txt"
    [ -f "$CONTRACT" ] || return 1
    [ -f "$ENVELOPE" ] || return 1

    CONTRACT_SCHEMA=$(gate5_field "$CONTRACT" schema_version) || return 1
    [ "$CONTRACT_SCHEMA" = 'environment-contract.v1' ] || return 1
    EXPECTED_RUN=$(gate5_field "$CONTRACT" pipeline_run_id) || return 1
    EXPECTED_RESULT=$(gate5_field "$CONTRACT" contract_result) || return 1
    [ "$EXPECTED_RUN" = "$RUN" ] || return 1
    [ "$EXPECTED_RESULT" = 'VALID' ] || return 1
    PROJECT_ID=$(gate5_field "$CONTRACT" project_id) || return 1
    [ "$PROJECT_ID" = 'alfa_device_ctrl' ] || return 1
    PROJECT_ROOT=$(gate5_field "$CONTRACT" project_root) || return 1
    [ -n "$PROJECT_ROOT" ] || return 1
    SOURCE_COMMIT=$(gate5_field "$CONTRACT" source_commit) || return 1
    gate5_valid_commit "$SOURCE_COMMIT" || return 1
    PROFILE_SHA=$(gate5_field "$CONTRACT" profile_sha256) || return 1
    gate5_valid_sha256 "$PROFILE_SHA" || return 1
    PROFILE_PATH=$(gate5_field "$CONTRACT" runtime_profile) || return 1

    case "$PROFILE_PATH" in
        artifacts/runtime_profiles/profile_*.txt)
            PROFILE_NAME=${PROFILE_PATH##*/}
            ;;
        /*/artifacts/runtime_profiles/profile_*.txt)
            PROFILE_NAME=${PROFILE_PATH##*/}
            ;;
        *)
            return 1
            ;;
    esac
    case "$PROFILE_NAME" in
        profile_*.txt) ;;
        *) return 1 ;;
    esac

    AUTHORITY_PATH="$ROOT/artifacts/runtime_profiles/profile_authority.txt"
    [ -f "$AUTHORITY_PATH" ] || return 1
    AUTHORITY_NAME=$(awk -v target="$RUN" 'BEGIN{RS=""}{r=0;v="";for(i=1;i<=NF;i++){split($i,p,"=");if(p[1]=="pipeline_run_id"&&p[2]==target)r=1;if(p[1]=="profile_path")v=p[2]}if(r&&v!="")print v}' "$AUTHORITY_PATH" | sed -n '1p')
    case "$AUTHORITY_NAME" in
        artifacts/runtime_profiles/profile_*.txt) ;;
        *) return 1 ;;
    esac
    [ "${AUTHORITY_NAME##*/}" = "$PROFILE_NAME" ] || return 1

    PROFILE_CANONICAL="$ROOT/artifacts/runtime_profiles/$PROFILE_NAME"
    [ -f "$PROFILE_CANONICAL" ] || return 1
    [ "$(sha256sum "$PROFILE_CANONICAL" | awk '{print $1}')" = "$PROFILE_SHA" ] || return 1
    [ "$(sha256sum "$PROFILE_CANONICAL" | awk '{print $1}')" = "$(awk -v target="$RUN" 'BEGIN{RS=""}{r=0;v="";for(i=1;i<=NF;i++){split($i,p,"=");if(p[1]=="pipeline_run_id"&&p[2]==target)r=1;if(p[1]=="profile_sha256")v=p[2]}if(r&&v!="")print v}' "$AUTHORITY_PATH" | sed -n '1p')" ] || return 1

    COMPLETION=$(gate5_field "$CONTRACT" completion_status) || return 1
    FAILURE_STATUS=$(gate5_field "$CONTRACT" failure_status) || return 1
    FAILURE_REASON=$(gate5_field "$CONTRACT" failure_reason) || return 1
    [ "$COMPLETION" = 'COMPLETE' ] || return 1
    [ "$FAILURE_STATUS" = 'COMPLETE' ] || return 1
    [ "$FAILURE_REASON" = 'NONE' ] || return 1

    [ "$(gate5_field "$ENVELOPE" schema_version)" = 'environment-contract-envelope.v1' ] || return 1
    [ "$(gate5_field "$ENVELOPE" pipeline_run_id)" = "$RUN" ] || return 1
    [ "$(gate5_field "$ENVELOPE" project_id)" = 'alfa_device_ctrl' ] || return 1
    [ -n "$(gate5_field "$ENVELOPE" project_root)" ] || return 1
    [ "$(gate5_field "$ENVELOPE" stage_id)" = 'gate4' ] || return 1
    [ "$(gate5_field "$ENVELOPE" producer)" = 'tools/environment_contract.sh' ] || return 1
    [ "$(gate5_field "$ENVELOPE" source_commit)" = "$SOURCE_COMMIT" ] || return 1
    [ "$(gate5_field "$ENVELOPE" artifact_path)" = "artifacts/pipeline/$RUN/gate4/environment_contract.txt" ] || return 1
    [ "$(gate5_field "$ENVELOPE" completion_status)" = 'COMPLETE' ] || return 1
    [ "$(gate5_field "$ENVELOPE" sha256)" = "$(gate5_hash "$CONTRACT")" ] || return 1

    printf '%s\n' "$CONTRACT"
}

gate5_compare_identity() {
    local LEFT RIGHT KEY LV RV
    LEFT=$1
    RIGHT=$2
    for KEY in request_id pipeline_run_id source_commit gate4_contract_sha256 profile_sha256 policy_version
    do
        LV=$(gate5_field "$LEFT" "$KEY" 2>/dev/null) || return 1
        RV=$(gate5_field "$RIGHT" "$KEY" 2>/dev/null) || return 1
        [ "$LV" = "$RV" ] || return 1
    done
}
