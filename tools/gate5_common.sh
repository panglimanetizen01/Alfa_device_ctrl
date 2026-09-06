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
    local ROOT RUN CONTRACT EXPECTED_RUN EXPECTED_RESULT
    ROOT=$1
    RUN=$2
    CONTRACT="$ROOT/artifacts/pipeline/$RUN/gate4/environment_contract.txt"
    [ -f "$CONTRACT" ] || return 1
    EXPECTED_RUN=$(gate5_field "$CONTRACT" pipeline_run_id) || return 1
    EXPECTED_RESULT=$(gate5_field "$CONTRACT" contract_result) || return 1
    [ "$EXPECTED_RUN" = "$RUN" ] || return 1
    [ "$EXPECTED_RESULT" = 'VALID' ] || return 1
    gate5_field "$CONTRACT" source_commit >/dev/null || return 1
    gate5_field "$CONTRACT" profile_sha256 >/dev/null || return 1
    gate5_field "$CONTRACT" project_id >/dev/null || return 1
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
