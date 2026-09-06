#!/usr/bin/env bash
# Shared current-run helpers for Gate 6-19.

chain_field() {
    local FILE KEY VALUE
    FILE=${1:-}
    KEY=${2:-}
    [ -f "$FILE" ] || return 1
    VALUE=$(awk -F= -v key="$KEY" '$1 == key { value=substr($0, index($0,"=")+1); count++ } END { if (count == 1) print value }' "$FILE")
    [ -n "$VALUE" ] || return 1
    printf '%s\n' "$VALUE"
}

chain_hash() {
    sha256sum "$1" 2>/dev/null | awk '{print $1}'
}

chain_now() {
    date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf '%s' 'UNKNOWN'
}

chain_gate4() {
    local ROOT RUN CONTRACT
    ROOT=$1
    RUN=$2
    CONTRACT="$ROOT/artifacts/pipeline/$RUN/gate4/environment_contract.txt"
    [ -f "$CONTRACT" ] || return 1
    [ "$(chain_field "$CONTRACT" pipeline_run_id 2>/dev/null)" = "$RUN" ] || return 1
    [ "$(chain_field "$CONTRACT" contract_result 2>/dev/null)" = 'VALID' ] || return 1
    chain_field "$CONTRACT" source_commit >/dev/null || return 1
    chain_field "$CONTRACT" profile_sha256 >/dev/null || return 1
    printf '%s\n' "$CONTRACT"
}

chain_identity_ok() {
    local EXPECTED FILE EXPECTED_HASH
    EXPECTED=$1
    FILE=$2
    [ "$(chain_field "$EXPECTED" pipeline_run_id 2>/dev/null)" = "$(chain_field "$FILE" pipeline_run_id 2>/dev/null)" ] || return 1
    [ "$(chain_field "$EXPECTED" source_commit 2>/dev/null)" = "$(chain_field "$FILE" source_commit 2>/dev/null)" ] || return 1
    [ "$(chain_field "$EXPECTED" profile_sha256 2>/dev/null)" = "$(chain_field "$FILE" profile_sha256 2>/dev/null)" ] || return 1
    EXPECTED_HASH=$(chain_hash "$EXPECTED") || return 1
    [ "$EXPECTED_HASH" = "$(chain_field "$FILE" gate4_contract_sha256 2>/dev/null)" ] || return 1
}
