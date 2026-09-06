#!/usr/bin/env bash

set -u

PROFILE_RC_OK=0
PROFILE_RC_BLOCKED=20
PROFILE_RC_INVALID=21

PROJECT_ROOT_EXPECTED='/storage/emulated/0/Alfa_device_ctrl'
PROJECT_ID_EXPECTED='alfa_device_ctrl'
PROFILE_DIR_REL='artifacts/runtime_profiles'
AUTHORITY_FILE_NAME='profile_authority.txt'

PROJECT_ROOT=''
PROFILE_OUTPUT_DIR=''
PROFILE_AUTHORITY_FILE=''
AUTHORITY_LOCK_DIR=''
RUN_DIR=''
RUN_ID=''
STATE_FILE=''
MANIFEST_FILE=''
SOURCE_COMMIT=''

PROFILE_FILE=''
PROFILE_REL=''
PROFILE_SHA256=''
PROFILE_GENERATED_AT=''

trim_value() {
    printf '%s' "$1" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

read_field() {
    local file="$1"
    local key="$2"
    local value

    value=$(sed -n \
        -e "s/^${key}=[[:space:]]*//p" \
        -e "s/^${key}:[[:space:]]*//p" \
        -e "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*//p" \
        -e "s/^[[:space:]]*${key}[[:space:]]*:[[:space:]]*//p" \
        "$file" 2>/dev/null | sed -n '1p')

    trim_value "$value"
}

read_payload_field() {
    local key="$1"
    local file value alias
    shift

    for file in "$@"; do
        [ -f "$file" ] || continue
        for alias in "$key"; do
            value=$(read_field "$file" "$alias")
            if [ -n "$value" ]; then
                printf '%s' "$value"
                return 0
            fi
        done
    done

    return 1
}

profile_value() {
    local key="$1"
    shift
    local value

    value=$(read_payload_field "$key" "$@" 2>/dev/null || true)
    if [ -n "$value" ]; then
        printf '%s' "$value"
    else
        printf '%s' 'UNKNOWN'
    fi
}

path_is_safe_relative() {
    local value="$1"
    case "$value" in
        ''|/*|*'..'*|*'*'*) return 1 ;;
        *) return 0 ;;
    esac
}

path_is_current_run_artifact() {
    local value="$1"
    case "$value" in
        artifacts/pipeline/$RUN_ID/*) return 0 ;;
        *) return 1 ;;
    esac
}

hash_matches() {
    local payload="$1"
    local envelope="$2"
    local expected_manifest_hash="$3"
    local envelope_hash computed

    [ -s "$payload" ] || return 1
    [ -s "$envelope" ] || return 1

    envelope_hash=$(read_field "$envelope" sha256)
    computed=$(sha256sum "$payload" 2>/dev/null | cut -d ' ' -f1)

    [ -n "$computed" ] || return 1
    [ "$envelope_hash" = "$computed" ] || return 1
    [ "$expected_manifest_hash" = "$computed" ] || return 1
}

validate_stage() {
    local stage="$1"
    local artifact_rel="$2"
    local envelope_rel="$3"
    local manifest_hash="$4"
    local expected_producer="$5"
    local artifact_abs envelope_abs
    local envelope_stage envelope_run envelope_commit envelope_status envelope_path envelope_producer

    path_is_safe_relative "$artifact_rel" || return 1
    path_is_safe_relative "$envelope_rel" || return 1
    path_is_current_run_artifact "$artifact_rel" || return 1
    path_is_current_run_artifact "$envelope_rel" || return 1

    artifact_abs="$PROJECT_ROOT/$artifact_rel"
    envelope_abs="$PROJECT_ROOT/$envelope_rel"

    [ -f "$artifact_abs" ] || return 1
    [ -f "$envelope_abs" ] || return 1

    envelope_stage=$(read_field "$envelope_abs" stage_id)
    envelope_run=$(read_field "$envelope_abs" pipeline_run_id)
    envelope_commit=$(read_field "$envelope_abs" source_commit)
    envelope_status=$(read_field "$envelope_abs" completion_status)
    envelope_path=$(read_field "$envelope_abs" artifact_path)
    envelope_producer=$(read_field "$envelope_abs" producer)

    [ "$envelope_stage" = "$stage" ] || return 1
    [ "$envelope_run" = "$RUN_ID" ] || return 1
    [ "$envelope_commit" = "$SOURCE_COMMIT" ] || return 1
    [ "$envelope_status" = 'COMPLETE' ] || return 1
    [ "$envelope_path" = "$artifact_rel" ] || return 1
    [ "$envelope_producer" = "$expected_producer" ] || return 1
    hash_matches "$artifact_abs" "$envelope_abs" "$manifest_hash" || return 1

    return 0
}

validate_explicit_run() {
    local state_schema state_run state_project state_root state_status state_completion state_result
    local manifest_run manifest_commit manifest_schema
    local ede_rel cde_rel gate3_rel ede_env cde_env gate3_env
    local ede_hash cde_hash gate3_hash
    local ede_stage cde_stage gate3_stage
    local canonical_run expected_run

    [ -d "$RUN_DIR" ] || return 1
    [ -f "$STATE_FILE" ] || return 1
    [ -f "$MANIFEST_FILE" ] || return 1

    canonical_run=$(realpath -e "$RUN_DIR" 2>/dev/null || true)
    expected_run="$PROJECT_ROOT_EXPECTED/artifacts/pipeline/$RUN_ID"
    [ "$canonical_run" = "$expected_run" ] || return 1

    state_schema=$(read_field "$STATE_FILE" schema_version)
    state_run=$(read_field "$STATE_FILE" pipeline_run_id)
    state_project=$(read_field "$STATE_FILE" project_id)
    state_root=$(read_field "$STATE_FILE" project_root)
    SOURCE_COMMIT=$(read_field "$STATE_FILE" source_commit)
    state_status=$(read_field "$STATE_FILE" pipeline_status)
    state_completion=$(read_field "$STATE_FILE" completion_status)
    state_result=$(read_field "$STATE_FILE" contract_result)

    [ "$state_schema" = 'pipeline-state.v1' ] || return 1
    [ "$state_run" = "$RUN_ID" ] || return 1
    [ "$state_project" = "$PROJECT_ID_EXPECTED" ] || return 1
    [ "$state_root" = "$PROJECT_ROOT_EXPECTED" ] || return 1
    [ -n "$SOURCE_COMMIT" ] && [ "$SOURCE_COMMIT" != 'UNKNOWN' ] || return 1
    [ "$state_status" = 'SUCCESS' ] || return 1
    [ "$state_completion" = 'COMPLETE' ] || return 1
    [ "$state_result" = 'VALID' ] || return 1
    [ "$(read_field "$STATE_FILE" ede_rc)" = '0' ] || return 1
    [ "$(read_field "$STATE_FILE" cde_rc)" = '0' ] || return 1
    [ "$(read_field "$STATE_FILE" gate3_rc)" = '0' ] || return 1

    manifest_schema=$(read_field "$MANIFEST_FILE" manifest_schema_version)
    manifest_run=$(read_field "$MANIFEST_FILE" pipeline_run_id)
    manifest_commit=$(read_field "$MANIFEST_FILE" source_commit)
    [ "$manifest_schema" = 'manifest.v1' ] || return 1
    [ "$manifest_run" = "$RUN_ID" ] || return 1
    [ "$manifest_commit" = "$SOURCE_COMMIT" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" project_id)" = "$PROJECT_ID_EXPECTED" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" project_root)" = "$PROJECT_ROOT_EXPECTED" ] || return 1

    ede_rel=$(read_field "$MANIFEST_FILE" ede_artifact)
    cde_rel=$(read_field "$MANIFEST_FILE" cde_artifact)
    gate3_rel=$(read_field "$MANIFEST_FILE" gate3_artifact)
    ede_env=$(read_field "$MANIFEST_FILE" ede_envelope)
    cde_env=$(read_field "$MANIFEST_FILE" cde_envelope)
    gate3_env=$(read_field "$MANIFEST_FILE" gate3_envelope)
    ede_hash=$(read_field "$MANIFEST_FILE" ede_sha256)
    cde_hash=$(read_field "$MANIFEST_FILE" cde_sha256)
    gate3_hash=$(read_field "$MANIFEST_FILE" gate3_sha256)
    ede_stage=$(read_field "$MANIFEST_FILE" ede_stage_id)
    cde_stage=$(read_field "$MANIFEST_FILE" cde_stage_id)
    gate3_stage=$(read_field "$MANIFEST_FILE" gate3_stage_id)

    [ "$ede_stage" = 'ede' ] || return 1
    [ "$cde_stage" = 'cde' ] || return 1
    [ "$gate3_stage" = 'gate3' ] || return 1

    validate_stage ede "$ede_rel" "$ede_env" "$ede_hash" 'tools/ede.sh' || return 1
    validate_stage cde "$cde_rel" "$cde_env" "$cde_hash" 'tools/cde.sh' || return 1
    validate_stage gate3 "$gate3_rel" "$gate3_env" "$gate3_hash" 'tools/execution_capability.sh' || return 1

    EDE_ARTIFACT_REL="$ede_rel"
    CDE_ARTIFACT_REL="$cde_rel"
    GATE3_ARTIFACT_REL="$gate3_rel"
    EDE_ARTIFACT="$PROJECT_ROOT/$ede_rel"
    CDE_ARTIFACT="$PROJECT_ROOT/$cde_rel"
    GATE3_ARTIFACT="$PROJECT_ROOT/$gate3_rel"

    return 0
}

validate_required_profile_fields() {
    local key value

    for key in DEVICE_CLASS ANDROID_HOST CONTAINER_ENVIRONMENT CPU_ARCH STORAGE_READ STORAGE_WRITE EXEC_PRIVATE EXEC_SHARED NETWORK_DNS PYTHON3 GIT JAVA JAVAC GRADLE; do
        value=$(read_field "$PROFILE_FILE" "$key")
        [ -n "$value" ] || return 1
        [ "$value" != 'UNKNOWN' ] || return 1
    done

    return 0
}

write_profile_payload() {
    local ede_file="$EDE_ARTIFACT"
    local cde_file="$CDE_ARTIFACT"
    local gate3_file="$GATE3_ARTIFACT"
    local device_class android_host container_environment cpu_arch
    local storage_read storage_write exec_private exec_shared network_dns
    local python3_status git_status java_status javac_status gradle_status
    local tmp_file

    android_host=$(profile_value android_host "$ede_file")
    container_environment=$(profile_value container_environment "$ede_file")
    cpu_arch=$(profile_value architecture "$ede_file")
    storage_read=$(profile_value STORAGE_READ "$cde_file")
    storage_write=$(profile_value STORAGE_WRITE "$cde_file")
    exec_private=$(profile_value EXEC_PRIVATE "$gate3_file")
    exec_shared=$(profile_value EXEC_SHARED "$gate3_file")
    network_dns=$(profile_value NETWORK_DNS "$cde_file")
    python3_status=$(profile_value PYTHON3 "$cde_file")
    git_status=$(profile_value GIT "$cde_file")
    java_status=$(profile_value JAVA "$cde_file")
    javac_status=$(profile_value JAVAC "$cde_file")
    gradle_status=$(profile_value GRADLE "$cde_file")

    if [ "$android_host" = 'DETECTED' ] && [ "$container_environment" = 'DETECTED' ]; then
        device_class='ANDROID_USERLAND'
    else
        device_class=$(profile_value DEVICE_CLASS "$ede_file")
    fi

    PROFILE_GENERATED_AT=$(date '+%Y-%m-%d %H:%M:%S')
    PROFILE_FILE="$PROFILE_OUTPUT_DIR/profile_$(date +%Y%m%d_%H%M%S)_$$.txt"
    PROFILE_REL="$PROFILE_DIR_REL/$(basename "$PROFILE_FILE")"
    tmp_file="$PROFILE_FILE.partial"

    {
        printf '%s\n' 'PROFILE_VERSION=1'
        printf '%s\n' "DEVICE_CLASS=$device_class"
        printf '%s\n' "ANDROID_HOST=$android_host"
        printf '%s\n' "CONTAINER_ENVIRONMENT=$container_environment"
        printf '%s\n' "CPU_ARCH=$cpu_arch"
        printf '%s\n' "STORAGE_READ=$storage_read"
        printf '%s\n' "STORAGE_WRITE=$storage_write"
        printf '%s\n' "EXEC_PRIVATE=$exec_private"
        printf '%s\n' "EXEC_SHARED=$exec_shared"
        printf '%s\n' "NETWORK_DNS=$network_dns"
        printf '%s\n' "PYTHON3=$python3_status"
        printf '%s\n' "GIT=$git_status"
        printf '%s\n' "JAVA=$java_status"
        printf '%s\n' "JAVAC=$javac_status"
        printf '%s\n' "GRADLE=$gradle_status"
        printf '%s\n' 'PIPELINE_STATUS=SUCCESS'
        printf '%s\n' "PIPELINE_RUN_ID=$RUN_ID"
        printf '%s\n' "SOURCE_COMMIT=$SOURCE_COMMIT"
        printf '%s\n' "EDE_ARTIFACT=$EDE_ARTIFACT_REL"
        printf '%s\n' "CDE_ARTIFACT=$CDE_ARTIFACT_REL"
        printf '%s\n' "GATE3_ARTIFACT=$GATE3_ARTIFACT_REL"
        printf '%s\n' "GENERATED_AT=$PROFILE_GENERATED_AT"
    } > "$tmp_file" || return 1

    mv "$tmp_file" "$PROFILE_FILE" 2>/dev/null || {
        rm -f "$tmp_file"
        return 1
    }

    validate_required_profile_fields || return 1

    PROFILE_SHA256=$(sha256sum "$PROFILE_FILE" 2>/dev/null | cut -d ' ' -f1)
    [ -n "$PROFILE_SHA256" ] || return 1

    return 0
}

acquire_authority_lock() {
    AUTHORITY_LOCK_DIR="$PROFILE_OUTPUT_DIR/.profile_authority.lock"
    if mkdir "$AUTHORITY_LOCK_DIR" 2>/dev/null; then
        return 0
    fi
    return 1
}

release_authority_lock() {
    if [ -n "$AUTHORITY_LOCK_DIR" ]; then
        rmdir "$AUTHORITY_LOCK_DIR" 2>/dev/null || true
    fi
}

append_authority_entry() {
    local file="$1"
    {
        printf '%s\n' 'authority_schema_version=profile-authority.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "profile_path=$PROFILE_REL"
        printf '%s\n' "profile_sha256=$PROFILE_SHA256"
        printf '%s\n' 'profile_version=1'
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "generated_at=$PROFILE_GENERATED_AT"
        printf '%s\n' 'authoritative=TRUE'
        printf '%s\n' 'profile_status=VALID'
    } >> "$file"
    printf '\n' >> "$file"
}

update_authority_index() {
    local index_tmp old_index
    local retained_tmp
    local authority_count

    old_index="$PROFILE_AUTHORITY_FILE"
    index_tmp="$PROFILE_OUTPUT_DIR/.profile_authority.txt.$$.tmp"
    retained_tmp="$PROFILE_OUTPUT_DIR/.profile_authority.retained.$$.tmp"

    : > "$retained_tmp" || return 1

    if [ -f "$old_index" ]; then
        awk -v target_run="$RUN_ID" '
            BEGIN { RS=""; ORS="\n\n" }
            {
                keep=1
                has_target=0
                for (i=1; i<=NF; i++) {
                    split($i, pair, "=")
                    if (pair[1] == "pipeline_run_id" && pair[2] == target_run) {
                        has_target=1
                    }
                }
                if (has_target) {
                    for (i=1; i<=NF; i++) {
                        if ($i == "authoritative=TRUE") $i="authoritative=FALSE"
                        if ($i == "profile_status=VALID") $i="profile_status=HISTORICAL"
                    }
                }
                for (i=1; i<=NF; i++) print $i
            }
        ' "$old_index" > "$retained_tmp" || {
            rm -f "$retained_tmp"
            return 1
        }
    fi

    cat "$retained_tmp" > "$index_tmp" || {
        rm -f "$retained_tmp"
        return 1
    }
    append_authority_entry "$index_tmp" || {
        rm -f "$retained_tmp" "$index_tmp"
        return 1
    }

    authority_count=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS=""; count=0 }
        {
            has_target=0
            has_authority=0
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
            }
            if (has_target && has_authority) count++
        }
        END { print count }
    ' "$index_tmp")

    [ "$authority_count" = '1' ] || {
        rm -f "$retained_tmp" "$index_tmp"
        return 1
    }

    if ! mv "$index_tmp" "$old_index"; then
        rm -f "$retained_tmp" "$index_tmp"
        return 1
    fi

    rm -f "$retained_tmp"
    return 0
}

validate_authority_readback() {
    local authority_count profile_path profile_hash profile_version authority
    local status source_commit index_file canonical_profile expected_profile

    index_file="$PROFILE_AUTHORITY_FILE"
    [ -s "$index_file" ] || return 1

    authority_count=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS=""; count=0 }
        {
            has_target=0
            has_authority=0
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
            }
            if (has_target && has_authority) count++
        }
        END { print count }
    ' "$index_file")
    [ "$authority_count" = '1' ] || return 1

    profile_path=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS="" }
        {
            has_target=0; has_authority=0; result=""
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
                if (pair[1] == "profile_path") result=pair[2]
            }
            if (has_target && has_authority) print result
        }
    ' "$index_file" | sed -n '1p')
    profile_hash=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS="" }
        {
            has_target=0; has_authority=0; result=""
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
                if (pair[1] == "profile_sha256") result=pair[2]
            }
            if (has_target && has_authority) print result
        }
    ' "$index_file" | sed -n '1p')
    profile_version=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS="" }
        {
            has_target=0; has_authority=0; result=""
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
                if (pair[1] == "profile_version") result=pair[2]
            }
            if (has_target && has_authority) print result
        }
    ' "$index_file" | sed -n '1p')
    status=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS="" }
        {
            has_target=0; has_authority=0; result=""
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
                if (pair[1] == "profile_status") result=pair[2]
            }
            if (has_target && has_authority) print result
        }
    ' "$index_file" | sed -n '1p')
    authority=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS="" }
        {
            has_target=0; has_authority=0
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
            }
            if (has_target && has_authority) print "TRUE"
        }
    ' "$index_file" | sed -n '1p')
    source_commit=$(awk -v target_run="$RUN_ID" '
        BEGIN { RS="" }
        {
            has_target=0; has_authority=0; result=""
            for (i=1; i<=NF; i++) {
                split($i, pair, "=")
                if (pair[1] == "pipeline_run_id" && pair[2] == target_run) has_target=1
                if ($i == "authoritative=TRUE") has_authority=1
                if (pair[1] == "source_commit") result=pair[2]
            }
            if (has_target && has_authority) print result
        }
    ' "$index_file" | sed -n '1p')

    expected_profile="$PROFILE_REL"
    canonical_profile=$(realpath -e "$PROFILE_FILE" 2>/dev/null || true)
    [ "$profile_path" = "$expected_profile" ] || return 1
    [ "$profile_hash" = "$PROFILE_SHA256" ] || return 1
    [ "$profile_version" = '1' ] || return 1
    [ "$status" = 'VALID' ] || return 1
    [ "$authority" = 'TRUE' ] || return 1
    [ "$source_commit" = "$SOURCE_COMMIT" ] || return 1
    [ -n "$canonical_profile" ] || return 1
    case "$canonical_profile" in
        "$PROJECT_ROOT_EXPECTED/$PROFILE_DIR_REL"/*) ;;
        *) return 1 ;;
    esac

    return 0
}

fail_blocked() {
    local reason="$1"
    printf '%s\n' 'PROFILE_AUTHORITY_STATUS=BLOCKED'
    printf '%s\n' "PROFILE_AUTHORITY_FAILURE_REASON=$reason"
    printf '%s\n' 'PROFILE_AUTHORITATIVE=FALSE'
    return "$PROFILE_RC_BLOCKED"
}

fail_invalid() {
    local reason="$1"
    printf '%s\n' 'PROFILE_AUTHORITY_STATUS=INVALID'
    printf '%s\n' "PROFILE_AUTHORITY_FAILURE_REASON=$reason"
    printf '%s\n' 'PROFILE_AUTHORITATIVE=FALSE'
    return "$PROFILE_RC_INVALID"
}

main() {
    local script_dir canonical_script_root

    if [ "$#" -ne 1 ]; then
        fail_blocked EXPLICIT_RUN_DIR_REQUIRED
        return $?
    fi

    script_dir=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)
    canonical_script_root=$(CDPATH= cd -- "$script_dir/.." 2>/dev/null && pwd)
    [ -n "$canonical_script_root" ] || { fail_blocked SCRIPT_ROOT_UNRESOLVED; return $?; }
    [ "$canonical_script_root" = "$PROJECT_ROOT_EXPECTED" ] || { fail_blocked MASTER_PATH_MISMATCH; return $?; }

    PROJECT_ROOT="$canonical_script_root"
    PROFILE_OUTPUT_DIR="$PROJECT_ROOT/$PROFILE_DIR_REL"
    PROFILE_AUTHORITY_FILE="$PROFILE_OUTPUT_DIR/$AUTHORITY_FILE_NAME"
    RUN_DIR="$1"
    RUN_ID=$(basename "$RUN_DIR")
    STATE_FILE="$RUN_DIR/pipeline_state.txt"
    MANIFEST_FILE="$RUN_DIR/manifest.txt"

    case "$RUN_DIR" in
        "$PROJECT_ROOT_EXPECTED/artifacts/pipeline/"*) ;;
        *) fail_blocked RUN_DIR_OUTSIDE_MASTER_PIPELINE_ROOT; return $? ;;
    esac
    [ "$RUN_ID" != '.' ] && [ "$RUN_ID" != '..' ] || { fail_blocked RUN_ID_INVALID; return $?; }
    [ -d "$PROFILE_OUTPUT_DIR" ] || { fail_blocked PROFILE_OUTPUT_DIRECTORY_MISSING; return $?; }

    if ! validate_explicit_run; then
        fail_blocked CURRENT_RUN_VALIDATION_FAILED
        return $?
    fi

    if ! write_profile_payload; then
        fail_blocked PROFILE_PAYLOAD_VALIDATION_OR_WRITE_FAILED
        return $?
    fi

    if ! acquire_authority_lock; then
        fail_blocked AUTHORITY_INDEX_LOCK_BUSY
        return $?
    fi

    if ! update_authority_index; then
        release_authority_lock
        fail_blocked AUTHORITY_INDEX_UPDATE_FAILED
        return $?
    fi

    if ! validate_authority_readback; then
        release_authority_lock
        fail_invalid AUTHORITY_INDEX_READBACK_FAILED
        return $?
    fi

    release_authority_lock

    printf '%s\n' 'PROFILE_AUTHORITY_STATUS=PASS'
    printf '%s\n' 'PROFILE_AUTHORITATIVE=TRUE'
    printf '%s\n' "PIPELINE_RUN_ID=$RUN_ID"
    printf '%s\n' "PROFILE_FILE=$PROFILE_FILE"
    printf '%s\n' "PROFILE_PATH=$PROFILE_REL"
    printf '%s\n' "PROFILE_SHA256=$PROFILE_SHA256"
    printf '%s\n' "SOURCE_COMMIT=$SOURCE_COMMIT"
    printf '%s\n' "AUTHORITY_FILE=$PROFILE_AUTHORITY_FILE"
    printf '%s\n' 'PROBING_REEXECUTED=NO'
    return "$PROFILE_RC_OK"
}

main "$@"
