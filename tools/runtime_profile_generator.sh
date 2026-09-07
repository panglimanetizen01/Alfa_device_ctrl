#!/usr/bin/env bash

set -u

RC_OK=0
RC_BLOCKED=20
PROJECT_ID=alfa_device_ctrl
PROFILE_DIR_REL=artifacts/runtime_profiles
ROOT=''
RUN_DIR=''
RUN_ID=''
STATE_FILE=''
MANIFEST_FILE=''
SOURCE_COMMIT=''
PROFILE_FILE=''
PROFILE_REL=''
PROFILE_SHA256=''
AUTHORITY_FILE=''

read_field() {
    local file="$1" key="$2"
    sed -n "s/^${key}=//p" "$file" 2>/dev/null | sed -n '1p'
}

field_count() {
    grep -c "^$2=" "$1" 2>/dev/null || true
}

hash_ok() {
    local payload="$1" envelope="$2" expected="$3" actual
    [ -s "$payload" ] && [ -s "$envelope" ] || return 1
    actual=$(sha256sum "$payload" 2>/dev/null | cut -d ' ' -f1)
    [ "$actual" = "$(read_field "$envelope" sha256)" ] && [ "$actual" = "$expected" ]
}

validate_stage() {
    local stage="$1" rel="$2" env="$3" expected_hash="$4" producer="$5"
    local artifact="$ROOT/$rel" envelope="$ROOT/$env"
    [ -n "$rel" ] && [ -n "$env" ] && [ -n "$expected_hash" ] || return 1
    case "$rel" in artifacts/pipeline/$RUN_ID/*) ;; *) return 1;; esac
    case "$env" in artifacts/pipeline/$RUN_ID/*) ;; *) return 1;; esac
    [ -f "$artifact" ] && [ -f "$envelope" ] || return 1
    [ "$(read_field "$envelope" stage_id)" = "$stage" ] || return 1
    [ "$(read_field "$envelope" pipeline_run_id)" = "$RUN_ID" ] || return 1
    [ "$(read_field "$envelope" source_commit)" = "$SOURCE_COMMIT" ] || return 1
    [ "$(read_field "$envelope" completion_status)" = COMPLETE ] || return 1
    [ "$(read_field "$envelope" artifact_path)" = "$rel" ] || return 1
    [ "$(read_field "$envelope" producer)" = "$producer" ] || return 1
    hash_ok "$artifact" "$envelope" "$expected_hash"
}

validate_run() {
    local schema run project root status completion result
    schema=$(read_field "$STATE_FILE" schema_version)
    run=$(read_field "$STATE_FILE" pipeline_run_id)
    project=$(read_field "$STATE_FILE" project_id)
    root=$(read_field "$STATE_FILE" project_root)
    SOURCE_COMMIT=$(read_field "$STATE_FILE" source_commit)
    status=$(read_field "$STATE_FILE" pipeline_status)
    completion=$(read_field "$STATE_FILE" completion_status)
    result=$(read_field "$STATE_FILE" contract_result)
    [ "$schema" = pipeline-state.v1 ] && [ "$run" = "$RUN_ID" ] && [ "$project" = "$PROJECT_ID" ] || return 1
    [ "$root" = "$ROOT" ] && [ -n "$SOURCE_COMMIT" ] && [ "$SOURCE_COMMIT" != UNKNOWN ] || return 1
    [ "$status" = RUNNING ] && [ "$completion" = RUNNING ] && [ "$result" = BLOCKED ] || return 1
    [ "$(read_field "$STATE_FILE" ede_rc)" = 0 ] && [ "$(read_field "$STATE_FILE" cde_rc)" = 0 ] && [ "$(read_field "$STATE_FILE" gate3_rc)" = 0 ] || return 1

    [ "$(read_field "$MANIFEST_FILE" manifest_schema_version)" = manifest.v1 ] || return 1
    [ "$(read_field "$MANIFEST_FILE" pipeline_run_id)" = "$RUN_ID" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" source_commit)" = "$SOURCE_COMMIT" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" project_id)" = "$PROJECT_ID" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" project_root)" = "$ROOT" ] || return 1

    validate_stage ede "$(read_field "$MANIFEST_FILE" ede_artifact)" "$(read_field "$MANIFEST_FILE" ede_envelope)" "$(read_field "$MANIFEST_FILE" ede_sha256)" tools/ede.sh || return 1
    validate_stage cde "$(read_field "$MANIFEST_FILE" cde_artifact)" "$(read_field "$MANIFEST_FILE" cde_envelope)" "$(read_field "$MANIFEST_FILE" cde_sha256)" tools/cde.sh || return 1
    validate_stage gate3 "$(read_field "$MANIFEST_FILE" gate3_artifact)" "$(read_field "$MANIFEST_FILE" gate3_envelope)" "$(read_field "$MANIFEST_FILE" gate3_sha256)" tools/execution_capability.sh || return 1
}

status_from() {
    local key="$1" file="$2" value
    value=$(read_field "$file" "$key")
    case "${value%% |*}" in PASS|ERROR) printf '%s' "${value%% |*}";; *) printf '%s' UNKNOWN;; esac
}

write_profile() {
    local ede="$ROOT/$(read_field "$MANIFEST_FILE" ede_artifact)"
    local cde="$ROOT/$(read_field "$MANIFEST_FILE" cde_artifact)"
    local g3="$ROOT/$(read_field "$MANIFEST_FILE" gate3_artifact)"
    local device_class android_host container_environment cpu_arch
    local storage_read storage_write exec_private shared_storage_io exec_shared
    local network_dns python3 git java javac gradle tmp
    android_host=$(read_field "$ede" android_host); container_environment=$(read_field "$ede" container_environment)
    cpu_arch=$(read_field "$ede" architecture); device_class=ANDROID_USERLAND
    storage_read=$(status_from STORAGE_READ "$cde"); storage_write=$(status_from STORAGE_WRITE "$cde")
    exec_private=$(status_from EXEC_PRIVATE "$g3")
    shared_storage_io=$(status_from SHARED_STORAGE_IO "$g3")
    exec_shared="$shared_storage_io"
    network_dns=$(status_from NETWORK_DNS "$cde"); python3=$(status_from PYTHON3 "$cde")
    git=$(status_from GIT "$cde"); java=$(status_from JAVA "$cde"); javac=$(status_from JAVAC "$cde"); gradle=$(status_from GRADLE "$cde")
    for v in "$storage_read" "$storage_write" "$exec_private" "$shared_storage_io" "$network_dns" "$python3" "$git" "$java" "$javac" "$gradle"; do
        case "$v" in PASS|ERROR) ;; *) return 1;; esac
    done
    mkdir -p "$ROOT/$PROFILE_DIR_REL" || return 1
    PROFILE_FILE="$ROOT/$PROFILE_DIR_REL/profile_$(date +%Y%m%d_%H%M%S)_$$.txt"
    PROFILE_REL="$PROFILE_DIR_REL/$(basename "$PROFILE_FILE")"
    tmp="$PROFILE_FILE.partial"
    {
        printf '%s\n' PROFILE_VERSION=1 DEVICE_CLASS="$device_class" ANDROID_HOST="$android_host" CONTAINER_ENVIRONMENT="$container_environment" CPU_ARCH="$cpu_arch"
        printf '%s\n' STORAGE_READ="$storage_read" STORAGE_WRITE="$storage_write" EXEC_PRIVATE="$exec_private" SHARED_STORAGE_IO="$shared_storage_io" EXEC_SHARED="$exec_shared"
        printf '%s\n' NETWORK_DNS="$network_dns" PYTHON3="$python3" GIT="$git" JAVA="$java" JAVAC="$javac" GRADLE="$gradle"
        printf '%s\n' PIPELINE_STATUS=RUNNING PIPELINE_RUN_ID="$RUN_ID" SOURCE_COMMIT="$SOURCE_COMMIT"
        printf '%s\n' "EDE_ARTIFACT=$(read_field "$MANIFEST_FILE" ede_artifact)" "CDE_ARTIFACT=$(read_field "$MANIFEST_FILE" cde_artifact)" "GATE3_ARTIFACT=$(read_field "$MANIFEST_FILE" gate3_artifact)" "GENERATED_AT=$(date '+%Y-%m-%d %H:%M:%S')"
    } > "$tmp" || return 1
    mv "$tmp" "$PROFILE_FILE" || return 1
    PROFILE_SHA256=$(sha256sum "$PROFILE_FILE" | cut -d ' ' -f1)
    [ -n "$PROFILE_SHA256" ] || return 1
}

update_authority() {
    local old="$AUTHORITY_FILE" tmp="$AUTHORITY_FILE.tmp.$$" block
    mkdir -p "$(dirname "$AUTHORITY_FILE")" || return 1
    : > "$tmp" || return 1
    if [ -f "$old" ]; then
        awk -v target="$RUN_ID" 'BEGIN{RS="";ORS="\n\n"}{hit=0;for(i=1;i<=NF;i++)if($i=="pipeline_run_id="target)hit=1;if(hit){for(i=1;i<=NF;i++){if($i=="authoritative=TRUE")$i="authoritative=FALSE";if($i=="profile_status=VALID")$i="profile_status=HISTORICAL"}}for(i=1;i<=NF;i++)print $i}' "$old" > "$tmp" || return 1
    fi
    {
        printf '%s\n' authority_schema_version=profile-authority.v1 pipeline_run_id="$RUN_ID" profile_path="$PROFILE_REL" profile_sha256="$PROFILE_SHA256" profile_version=1 source_commit="$SOURCE_COMMIT" "generated_at=$(date '+%Y-%m-%d %H:%M:%S')" authoritative=TRUE profile_status=VALID
        printf '\n'
    } >> "$tmp" || return 1
    mv "$tmp" "$old" || return 1
    [ "$(awk -v target="$RUN_ID" 'BEGIN{RS="";n=0}{r=0;a=0;for(i=1;i<=NF;i++){if($i=="pipeline_run_id="target)r=1;if($i=="authoritative=TRUE")a=1}if(r&&a)n++}END{print n}' "$old")" = 1 ]
}

main() {
    [ "$#" -eq 1 ] || { printf '%s\n' PROFILE_AUTHORITY_STATUS=BLOCKED PROFILE_AUTHORITY_FAILURE_REASON=EXPLICIT_RUN_DIR_REQUIRED PROFILE_AUTHORITATIVE=FALSE; return $RC_BLOCKED; }
    ROOT=$(realpath -e "$(dirname "${BASH_SOURCE[0]}")/.." 2>/dev/null || true)
    RUN_DIR=$(realpath -e "$1" 2>/dev/null || true); RUN_ID=$(basename "$RUN_DIR")
    [ -n "$ROOT" ] && [ -d "$RUN_DIR" ] || { printf '%s\n' PROFILE_AUTHORITY_STATUS=BLOCKED PROFILE_AUTHORITY_FAILURE_REASON=PATH_UNRESOLVED PROFILE_AUTHORITATIVE=FALSE; return $RC_BLOCKED; }
    case "$RUN_DIR" in "$ROOT/artifacts/pipeline/"*) ;; *) printf '%s\n' PROFILE_AUTHORITY_STATUS=BLOCKED PROFILE_AUTHORITY_FAILURE_REASON=RUN_DIR_OUTSIDE_MASTER_PIPELINE_ROOT PROFILE_AUTHORITATIVE=FALSE; return $RC_BLOCKED;; esac
    STATE_FILE="$RUN_DIR/pipeline_state.txt"; MANIFEST_FILE="$RUN_DIR/manifest.txt"; AUTHORITY_FILE="$ROOT/$PROFILE_DIR_REL/profile_authority.txt"
    validate_run || { printf '%s\n' PROFILE_AUTHORITY_STATUS=BLOCKED PROFILE_AUTHORITY_FAILURE_REASON=CURRENT_RUN_VALIDATION_FAILED PROFILE_AUTHORITATIVE=FALSE; return $RC_BLOCKED; }
    write_profile || { printf '%s\n' PROFILE_AUTHORITY_STATUS=BLOCKED PROFILE_AUTHORITY_FAILURE_REASON=PROFILE_PAYLOAD_VALIDATION_OR_WRITE_FAILED PROFILE_AUTHORITATIVE=FALSE; return $RC_BLOCKED; }
    update_authority || { printf '%s\n' PROFILE_AUTHORITY_STATUS=BLOCKED PROFILE_AUTHORITY_FAILURE_REASON=AUTHORITY_INDEX_UPDATE_FAILED PROFILE_AUTHORITATIVE=FALSE; return $RC_BLOCKED; }
    printf '%s\n' PROFILE_AUTHORITY_STATUS=PASS PROFILE_AUTHORITATIVE=TRUE "PIPELINE_RUN_ID=$RUN_ID" "PROFILE_FILE=$PROFILE_FILE" "PROFILE_PATH=$PROFILE_REL" "PROFILE_SHA256=$PROFILE_SHA256" "SOURCE_COMMIT=$SOURCE_COMMIT" "AUTHORITY_FILE=$AUTHORITY_FILE" PROBING_REEXECUTED=NO
}

main "$@"
