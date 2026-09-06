#!/usr/bin/env bash

set -u

GATE4_RC_VALID=0
GATE4_RC_BLOCKED=20
GATE4_RC_INVALID=21

PROJECT_ROOT_EXPECTED='/storage/emulated/0/Alfa_device_ctrl'
PROJECT_ID_EXPECTED='alfa_device_ctrl'
PIPELINE_ROOT_REL='artifacts/pipeline'
PROFILE_ROOT_REL='artifacts/runtime_profiles'
AUTHORITY_REL='artifacts/runtime_profiles/profile_authority.txt'

PROJECT_ROOT=''
RUN_DIR=''
RUN_ID=''
STATE_FILE=''
MANIFEST_FILE=''
PROFILE_FILE=''
PROFILE_AUTHORITY_FILE=''
OUTPUT_DIR=''
OUTPUT_FILE=''
OUTPUT_ENVELOPE=''

SOURCE_COMMIT='UNKNOWN'
PROJECT_ID='UNKNOWN'
PROJECT_ROOT_VALUE='UNKNOWN'
PIPELINE_STATUS='UNKNOWN'
PIPELINE_COMPLETION='UNKNOWN'
PIPELINE_CONTRACT_RESULT='UNKNOWN'

EDE_ARTIFACT_REL='UNKNOWN'
CDE_ARTIFACT_REL='UNKNOWN'
GATE3_ARTIFACT_REL='UNKNOWN'
EDE_ENVELOPE_REL='UNKNOWN'
CDE_ENVELOPE_REL='UNKNOWN'
GATE3_ENVELOPE_REL='UNKNOWN'
EDE_HASH='UNKNOWN'
CDE_HASH='UNKNOWN'
GATE3_HASH='UNKNOWN'

PROFILE_AUTHORITY_PATH='UNKNOWN'
PROFILE_AUTHORITY_HASH='UNKNOWN'
PROFILE_AUTHORITY_SOURCE_COMMIT='UNKNOWN'
PROFILE_AUTHORITY_RUN_ID='UNKNOWN'
PROFILE_AUTHORITY_STATUS='UNKNOWN'

failure_output_allowed() {
    [ -n "$RUN_DIR" ] || return 1
    case "$RUN_DIR" in
        "$PROJECT_ROOT_EXPECTED/$PIPELINE_ROOT_REL/"*) return 0 ;;
        *) return 1 ;;
    esac
}

read_field() {
    local file="$1"
    local key="$2"
    sed -n "s/^${key}=//p" "$file" 2>/dev/null | sed -n '1p'
}

field_count() {
    local file="$1"
    local key="$2"
    grep -c "^${key}=" "$file" 2>/dev/null || true
}

read_payload_field() {
    local file="$1"
    local key="$2"
    read_field "$file" "$key"
}

is_non_unknown() {
    local value="$1"
    [ -n "$value" ] && [ "$value" != 'UNKNOWN' ]
}

allowed_status() {
    local value="$1"
    local status="${value%% |*}"

    case "$status" in
        PASS|ERROR) return 0 ;;
        *) return 1 ;;
    esac
}

allowed_ede_value() {
    local key="$1"
    local value="$2"
    case "$key" in
        android_host|container_environment)
            [ "$value" = 'DETECTED' ] || [ "$value" = 'NOT_DETECTED' ] || [ "$value" = 'UNKNOWN' ]
            ;;
        shared_storage)
            [ "$value" = 'PRESENT' ] || [ "$value" = 'ABSENT' ] || [ "$value" = 'UNKNOWN' ]
            ;;
        writable_shared_storage)
            [ "$value" = 'YES' ] || [ "$value" = 'NO' ] || [ "$value" = 'UNKNOWN' ]
            ;;
        storage_status)
            [ "$value" = 'PASS' ] || [ "$value" = 'ERROR' ] || [ "$value" = 'UNKNOWN' ]
            ;;
        toolchain_status)
            [ "$value" = 'PASS' ] || [ "$value" = 'WARNING' ] || [ "$value" = 'ERROR' ] || [ "$value" = 'UNKNOWN' ]
            ;;
        architecture)
            is_non_unknown "$value"
            ;;
        *) return 1 ;;
    esac
}

allowed_profile_value() {
    local key="$1"
    local value="$2"
    case "$key" in
        DEVICE_CLASS)
            [ "$value" = 'ANDROID_USERLAND' ] || is_non_unknown "$value"
            ;;
        ANDROID_HOST|CONTAINER_ENVIRONMENT)
            [ "$value" = 'DETECTED' ] || [ "$value" = 'NOT_DETECTED' ]
            ;;
        CPU_ARCH)
            is_non_unknown "$value"
            ;;
        STORAGE_READ|STORAGE_WRITE|EXEC_PRIVATE|EXEC_SHARED|NETWORK_DNS|PYTHON3|GIT|JAVA|JAVAC|GRADLE)
            allowed_status "$value"
            ;;
        *) return 1 ;;
    esac
}

path_is_safe_relative() {
    local value="$1"
    case "$value" in
        ''|/*|*'..'*|*'*'*|*'?'*|*'['*|*']'*|*' '*|*$'\n'*) return 1 ;;
        *) return 0 ;;
    esac
}

canonical_path_is_under() {
    local child="$1"
    local parent="$2"
    case "$child" in
        "$parent"/*) return 0 ;;
        *) return 1 ;;
    esac
}

manifest_authority_value() {
    local key="$1"
    sed -n "s/^${key}=//p" "$MANIFEST_FILE" 2>/dev/null | sed -n '1p'
}

authority_block_count() {
    awk -v target="$RUN_ID" '
        BEGIN { RS=""; count=0 }
        {
            run=""; auth=""; status=""
            for (i=1; i<=NF; i++) {
                if ($i ~ /^pipeline_run_id=/) run=substr($i,17)
                if ($i == "authoritative=TRUE") auth="TRUE"
                if ($i ~ /^profile_status=/) status=substr($i,16)
            }
            if (run == target && auth == "TRUE" && status == "VALID") count++
        }
        END { print count }
    ' "$PROFILE_AUTHORITY_FILE" 2>/dev/null
}

authority_value() {
    local key="$1"
    awk -v target="$RUN_ID" -v wanted="$key" '
        BEGIN { RS="" }
        {
            run=""; auth=""; status=""; found=""
            for (i=1; i<=NF; i++) {
                if ($i ~ /^pipeline_run_id=/) run=substr($i,17)
                if ($i == "authoritative=TRUE") auth="TRUE"
                if ($i ~ /^profile_status=/) status=substr($i,16)
            }
            if (run == target && auth == "TRUE" && status == "VALID") {
                for (i=1; i<=NF; i++) {
                    prefix=wanted "="
                    if (index($i, prefix) == 1 && found == "") found=substr($i, length(prefix)+1)
                }
                if (found != "") print found
            }
        }
    ' "$PROFILE_AUTHORITY_FILE" 2>/dev/null
}

validate_optional_identity_fields() {
    local file="$1"
    local key expected count value

    for key in pipeline_run_id source_commit project_id project_root stage_id completion_status; do
        count=$(field_count "$file" "$key")
        [ "$count" -le 1 ] || return 1
        [ "$count" -eq 1 ] || continue
        value=$(read_field "$file" "$key")
        case "$key" in
            pipeline_run_id) expected="$RUN_ID" ;;
            source_commit) expected="$SOURCE_COMMIT" ;;
            project_id) expected="$PROJECT_ID_EXPECTED" ;;
            project_root) expected="$PROJECT_ROOT_EXPECTED" ;;
            stage_id) expected="$CURRENT_STAGE_ID" ;;
            completion_status) expected='COMPLETE' ;;
        esac
        [ "$value" = "$expected" ] || return 1
    done

    return 0
}

validate_ede_payload() {
    local file="$1"
    local key value count

    for key in android_host container_environment architecture android_shared_storage writable_shared_storage storage_status toolchain_status; do
        count=$(field_count "$file" "$key")
        [ "$count" = '1' ] || return 1
        value=$(read_payload_field "$file" "$key")
        case "$key" in
            android_shared_storage)
                case "$value" in
                    PRESENT|ABSENT) ;;
                    *) return 1 ;;
                esac
                ;;
            *)
                allowed_ede_value "$key" "$value" || return 1
                ;;
        esac
        [ "$value" != 'UNKNOWN' ] || return 1
    done

    CURRENT_STAGE_ID='ede'
    validate_optional_identity_fields "$file" || return 1
    return 0
}

validate_cde_payload() {
    local file="$1"
    local key value count

    for key in STORAGE_READ STORAGE_WRITE NETWORK_DNS PYTHON3 GIT JAVA JAVAC GRADLE; do
        count=$(field_count "$file" "$key")
        [ "$count" = '1' ] || return 1
        value=$(read_payload_field "$file" "$key")
        allowed_status "$value" || return 1
    done

    CURRENT_STAGE_ID='cde'
    validate_optional_identity_fields "$file" || return 1
    return 0
}

validate_gate3_payload() {
    local file="$1"
    local key value count

    for key in EXEC_PRIVATE EXEC_SHARED SCRIPT_BASH SCRIPT_PYTHON PROCESS_SPAWN; do
        count=$(field_count "$file" "$key")
        [ "$count" = '1' ] || return 1
        value=$(read_payload_field "$file" "$key")
        allowed_status "$value" || return 1
    done

    CURRENT_STAGE_ID='gate3'
    validate_optional_identity_fields "$file" || return 1
    return 0
}

validate_stage() {
    local stage="$1"
    local artifact_rel="$2"
    local envelope_rel="$3"
    local manifest_hash="$4"
    local expected_producer="$5"
    local artifact_abs envelope_abs canonical_artifact canonical_envelope
    local envelope_schema envelope_stage envelope_run envelope_commit envelope_status envelope_path envelope_producer

    path_is_safe_relative "$artifact_rel" || return 1
    path_is_safe_relative "$envelope_rel" || return 1
    path_is_current_run_artifact "$artifact_rel" || return 1
    path_is_current_run_artifact "$envelope_rel" || return 1

    artifact_abs="$PROJECT_ROOT/$artifact_rel"
    envelope_abs="$PROJECT_ROOT/$envelope_rel"
    [ -f "$artifact_abs" ] || return 1
    [ -f "$envelope_abs" ] || return 1

    canonical_artifact=$(realpath -e "$artifact_abs" 2>/dev/null || true)
    canonical_envelope=$(realpath -e "$envelope_abs" 2>/dev/null || true)
    canonical_path_is_under "$canonical_artifact" "$RUN_DIR" || return 1
    canonical_path_is_under "$canonical_envelope" "$RUN_DIR" || return 1

    envelope_schema=$(read_field "$envelope_abs" schema_version)
    envelope_stage=$(read_field "$envelope_abs" stage_id)
    envelope_run=$(read_field "$envelope_abs" pipeline_run_id)
    envelope_commit=$(read_field "$envelope_abs" source_commit)
    envelope_status=$(read_field "$envelope_abs" completion_status)
    envelope_path=$(read_field "$envelope_abs" artifact_path)
    envelope_producer=$(read_field "$envelope_abs" producer)

    [ "$envelope_schema" = 'artifact-envelope.v1' ] || return 1
    [ "$envelope_stage" = "$stage" ] || return 1
    [ "$envelope_run" = "$RUN_ID" ] || return 1
    [ "$envelope_commit" = "$SOURCE_COMMIT" ] || return 1
    [ "$envelope_status" = 'COMPLETE' ] || return 1
    [ "$envelope_path" = "$artifact_rel" ] || return 1
    [ "$envelope_producer" = "$expected_producer" ] || return 1
    hash_matches "$artifact_abs" "$envelope_abs" "$manifest_hash" || return 1

    case "$stage" in
        ede) validate_ede_payload "$artifact_abs" ;;
        cde) validate_cde_payload "$artifact_abs" ;;
        gate3) validate_gate3_payload "$artifact_abs" ;;
        *) return 1 ;;
    esac
}

path_is_current_run_artifact() {
    local value="$1"
    case "$value" in
        "$PIPELINE_ROOT_REL/$RUN_ID/"*) return 0 ;;
        *) return 1 ;;
    esac
}

hash_matches() {
    local payload="$1"
    local envelope="$2"
    local manifest_hash="$3"
    local envelope_hash computed

    [ -s "$payload" ] || return 1
    [ -s "$envelope" ] || return 1
    envelope_hash=$(read_field "$envelope" sha256)
    computed=$(sha256sum "$payload" 2>/dev/null | cut -d ' ' -f1)
    [ -n "$computed" ] || return 1
    [ "$envelope_hash" = "$computed" ] || return 1
    [ "$manifest_hash" = "$computed" ] || return 1
}

validate_state_and_manifest() {
    local state_schema state_run state_project state_root state_status state_completion state_result
    local manifest_schema manifest_run manifest_commit manifest_project manifest_root
    local canonical_run expected_run

    [ -d "$RUN_DIR" ] || return 1
    [ -f "$STATE_FILE" ] || return 1
    [ -f "$MANIFEST_FILE" ] || return 1

    canonical_run=$(realpath -e "$RUN_DIR" 2>/dev/null || true)
    expected_run="$PROJECT_ROOT_EXPECTED/$PIPELINE_ROOT_REL/$RUN_ID"
    [ "$canonical_run" = "$expected_run" ] || return 1

    state_schema=$(read_field "$STATE_FILE" schema_version)
    state_run=$(read_field "$STATE_FILE" pipeline_run_id)
    state_project=$(read_field "$STATE_FILE" project_id)
    state_root=$(read_field "$STATE_FILE" project_root)
    SOURCE_COMMIT=$(read_field "$STATE_FILE" source_commit)
    state_status=$(read_field "$STATE_FILE" pipeline_status)
    state_completion=$(read_field "$STATE_FILE" completion_status)
    state_result=$(read_field "$STATE_FILE" contract_result)

    PROJECT_ID="$state_project"
    PROJECT_ROOT_VALUE="$state_root"
    PIPELINE_STATUS="$state_status"
    PIPELINE_COMPLETION="$state_completion"
    PIPELINE_CONTRACT_RESULT="$state_result"

    [ "$state_schema" = 'pipeline-state.v1' ] || return 1
    [ "$state_run" = "$RUN_ID" ] || return 1
    [ "$state_project" = "$PROJECT_ID_EXPECTED" ] || return 1
    [ "$state_root" = "$PROJECT_ROOT_EXPECTED" ] || return 1
    is_non_unknown "$SOURCE_COMMIT" || return 1
    [ "$state_status" = 'SUCCESS' ] || return 1
    [ "$state_completion" = 'COMPLETE' ] || return 1
    [ "$state_result" = 'VALID' ] || return 1
    [ "$(read_field "$STATE_FILE" ede_rc)" = '0' ] || return 1
    [ "$(read_field "$STATE_FILE" cde_rc)" = '0' ] || return 1
    [ "$(read_field "$STATE_FILE" gate3_rc)" = '0' ] || return 1

    manifest_schema=$(read_field "$MANIFEST_FILE" manifest_schema_version)
    manifest_run=$(read_field "$MANIFEST_FILE" pipeline_run_id)
    manifest_commit=$(read_field "$MANIFEST_FILE" source_commit)
    manifest_project=$(read_field "$MANIFEST_FILE" project_id)
    manifest_root=$(read_field "$MANIFEST_FILE" project_root)

    [ "$manifest_schema" = 'manifest.v1' ] || return 1
    [ "$manifest_run" = "$RUN_ID" ] || return 1
    [ "$manifest_commit" = "$SOURCE_COMMIT" ] || return 1
    [ "$manifest_project" = "$PROJECT_ID_EXPECTED" ] || return 1
    [ "$manifest_root" = "$PROJECT_ROOT_EXPECTED" ] || return 1

    EDE_ARTIFACT_REL=$(manifest_authority_value ede_artifact)
    CDE_ARTIFACT_REL=$(manifest_authority_value cde_artifact)
    GATE3_ARTIFACT_REL=$(manifest_authority_value gate3_artifact)
    EDE_ENVELOPE_REL=$(manifest_authority_value ede_envelope)
    CDE_ENVELOPE_REL=$(manifest_authority_value cde_envelope)
    GATE3_ENVELOPE_REL=$(manifest_authority_value gate3_envelope)
    EDE_HASH=$(manifest_authority_value ede_sha256)
    CDE_HASH=$(manifest_authority_value cde_sha256)
    GATE3_HASH=$(manifest_authority_value gate3_sha256)

    [ -n "$EDE_ARTIFACT_REL" ] && [ -n "$CDE_ARTIFACT_REL" ] && [ -n "$GATE3_ARTIFACT_REL" ] || return 1
    [ -n "$EDE_ENVELOPE_REL" ] && [ -n "$CDE_ENVELOPE_REL" ] && [ -n "$GATE3_ENVELOPE_REL" ] || return 1
    [ -n "$EDE_HASH" ] && [ -n "$CDE_HASH" ] && [ -n "$GATE3_HASH" ] || return 1

    [ "$(manifest_authority_value ede_stage_id)" = 'ede' ] || return 1
    [ "$(manifest_authority_value cde_stage_id)" = 'cde' ] || return 1
    [ "$(manifest_authority_value gate3_stage_id)" = 'gate3' ] || return 1

    validate_stage ede "$EDE_ARTIFACT_REL" "$EDE_ENVELOPE_REL" "$EDE_HASH" 'tools/ede.sh' || return 1
    validate_stage cde "$CDE_ARTIFACT_REL" "$CDE_ENVELOPE_REL" "$CDE_HASH" 'tools/cde.sh' || return 1
    validate_stage gate3 "$GATE3_ARTIFACT_REL" "$GATE3_ENVELOPE_REL" "$GATE3_HASH" 'tools/execution_capability.sh' || return 1

    return 0
}

validate_profile_authority() {
    local canonical_profile canonical_profile_root authority_count
    local authority_path authority_hash authority_version authority_source authority_run authority_status
    local profile_run profile_source profile_version profile_pipeline_status
    local key value count

    [ -f "$PROFILE_AUTHORITY_FILE" ] || return 1
    authority_count=$(authority_block_count)
    [ "$authority_count" = '1' ] || return 1

    authority_path=$(authority_value profile_path)
    authority_hash=$(authority_value profile_sha256)
    authority_version=$(authority_value profile_version)
    authority_source=$(authority_value source_commit)
    authority_run=$(authority_value pipeline_run_id)
    authority_status=$(authority_value profile_status)

    path_is_safe_relative "$authority_path" || return 1
    case "$authority_path" in
        "$PROFILE_ROOT_REL/profile_"*.txt) ;;
        *) return 1 ;;
    esac
    [ "$authority_run" = "$RUN_ID" ] || return 1
    [ "$authority_version" = '1' ] || return 1
    [ "$authority_status" = 'VALID' ] || return 1
    [ "$authority_source" = "$SOURCE_COMMIT" ] || return 1
    [ -n "$authority_hash" ] || return 1

    [ "$PROFILE_FILE" = "$PROJECT_ROOT/$authority_path" ] || return 1
    [ -f "$PROFILE_FILE" ] || return 1
    canonical_profile=$(realpath -e "$PROFILE_FILE" 2>/dev/null || true)
    canonical_profile_root=$(realpath -e "$PROJECT_ROOT_EXPECTED/$PROFILE_ROOT_REL" 2>/dev/null || true)
    [ -n "$canonical_profile" ] && [ -n "$canonical_profile_root" ] || return 1
    canonical_path_is_under "$canonical_profile" "$canonical_profile_root" || return 1
    [ "$(sha256sum "$PROFILE_FILE" 2>/dev/null | cut -d ' ' -f1)" = "$authority_hash" ] || return 1

    profile_run=$(read_field "$PROFILE_FILE" PIPELINE_RUN_ID)
    profile_source=$(read_field "$PROFILE_FILE" SOURCE_COMMIT)
    profile_version=$(read_field "$PROFILE_FILE" PROFILE_VERSION)
    profile_pipeline_status=$(read_field "$PROFILE_FILE" PIPELINE_STATUS)
    [ "$profile_run" = "$RUN_ID" ] || return 1
    [ "$profile_source" = "$SOURCE_COMMIT" ] || return 1
    [ "$profile_version" = '1' ] || return 1
    [ "$profile_pipeline_status" = 'SUCCESS' ] || return 1

    for key in DEVICE_CLASS ANDROID_HOST CONTAINER_ENVIRONMENT CPU_ARCH STORAGE_READ STORAGE_WRITE EXEC_PRIVATE EXEC_SHARED NETWORK_DNS PYTHON3 GIT JAVA JAVAC GRADLE; do
        count=$(field_count "$PROFILE_FILE" "$key")
        [ "$count" = '1' ] || return 1
        value=$(read_field "$PROFILE_FILE" "$key")
        [ "$value" != 'UNKNOWN' ] && [ -n "$value" ] || return 1
        allowed_profile_value "$key" "$value" || return 1
    done

    PROFILE_AUTHORITY_PATH="$authority_path"
    PROFILE_AUTHORITY_HASH="$authority_hash"
    PROFILE_AUTHORITY_SOURCE_COMMIT="$authority_source"
    PROFILE_AUTHORITY_RUN_ID="$authority_run"
    PROFILE_AUTHORITY_STATUS="$authority_status"
    return 0
}

profile_field() {
    local key="$1"
    local value
    value=$(read_field "$PROFILE_FILE" "$key")
    [ -n "$value" ] && printf '%s' "$value" || printf '%s' 'UNKNOWN'
}

write_contract_payload() {
    local result="$1"
    local status="$2"
    local reason="$3"
    local generated_at="$4"
    local tmp_file
    local device_class android_host container_environment cpu_arch
    local storage_read storage_write exec_private exec_shared network_dns
    local python3_status git_status java_status javac_status gradle_status

    [ -n "$RUN_DIR" ] || return 1
    mkdir -p "$RUN_DIR/gate4" 2>/dev/null || return 1
    OUTPUT_DIR="$RUN_DIR/gate4"
    OUTPUT_FILE="$OUTPUT_DIR/environment_contract.txt"
    OUTPUT_ENVELOPE="$OUTPUT_DIR/environment_contract.envelope.txt"
    tmp_file="$OUTPUT_FILE.partial.$$"

    device_class=$(profile_field DEVICE_CLASS)
    android_host=$(profile_field ANDROID_HOST)
    container_environment=$(profile_field CONTAINER_ENVIRONMENT)
    cpu_arch=$(profile_field CPU_ARCH)
    storage_read=$(profile_field STORAGE_READ)
    storage_write=$(profile_field STORAGE_WRITE)
    exec_private=$(profile_field EXEC_PRIVATE)
    exec_shared=$(profile_field EXEC_SHARED)
    network_dns=$(profile_field NETWORK_DNS)
    python3_status=$(profile_field PYTHON3)
    git_status=$(profile_field GIT)
    java_status=$(profile_field JAVA)
    javac_status=$(profile_field JAVAC)
    gradle_status=$(profile_field GRADLE)

    {
        printf '%s\n' 'schema_version=environment-contract.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' "project_id=$PROJECT_ID"
        printf '%s\n' "project_root=$PROJECT_ROOT_VALUE"
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "input_state=$STATE_FILE"
        printf '%s\n' "input_manifest=$MANIFEST_FILE"
        printf '%s\n' "ede_artifact=$EDE_ARTIFACT_REL"
        printf '%s\n' "ede_envelope=$EDE_ENVELOPE_REL"
        printf '%s\n' "ede_sha256=$EDE_HASH"
        printf '%s\n' "cde_artifact=$CDE_ARTIFACT_REL"
        printf '%s\n' "cde_envelope=$CDE_ENVELOPE_REL"
        printf '%s\n' "cde_sha256=$CDE_HASH"
        printf '%s\n' "gate3_artifact=$GATE3_ARTIFACT_REL"
        printf '%s\n' "gate3_envelope=$GATE3_ENVELOPE_REL"
        printf '%s\n' "gate3_sha256=$GATE3_HASH"
        printf '%s\n' "runtime_profile=$PROFILE_FILE"
        printf '%s\n' "profile_authority=$PROFILE_AUTHORITY_FILE"
        printf '%s\n' "profile_authority_path=$PROFILE_AUTHORITY_PATH"
        printf '%s\n' "profile_sha256=$PROFILE_AUTHORITY_HASH"
        printf '%s\n' "profile_pipeline_run_id=$PROFILE_AUTHORITY_RUN_ID"
        printf '%s\n' "profile_source_commit=$PROFILE_AUTHORITY_SOURCE_COMMIT"
        printf '%s\n' "device_class=$device_class"
        printf '%s\n' "android_host=$android_host"
        printf '%s\n' "container_environment=$container_environment"
        printf '%s\n' "cpu_arch=$cpu_arch"
        printf '%s\n' "storage_read=$storage_read"
        printf '%s\n' "storage_write=$storage_write"
        printf '%s\n' "exec_private=$exec_private"
        printf '%s\n' "exec_shared=$exec_shared"
        printf '%s\n' "network_dns=$network_dns"
        printf '%s\n' "python3=$python3_status"
        printf '%s\n' "git=$git_status"
        printf '%s\n' "java=$java_status"
        printf '%s\n' "javac=$javac_status"
        printf '%s\n' "gradle=$gradle_status"
        printf '%s\n' "pipeline_status=$PIPELINE_STATUS"
        printf '%s\n' "completion_status=$status"
        printf '%s\n' "contract_result=$result"
        printf '%s\n' "failure_status=$status"
        printf '%s\n' "failure_reason=$reason"
        printf '%s\n' "generated_at=$generated_at"
    } > "$tmp_file" || return 1

    mv "$tmp_file" "$OUTPUT_FILE" 2>/dev/null || {
        rm -f "$tmp_file"
        return 1
    }
    return 0
}

write_contract_envelope() {
    local contract_hash generated_at tmp_file

    [ -s "$OUTPUT_FILE" ] || return 1
    contract_hash=$(sha256sum "$OUTPUT_FILE" 2>/dev/null | cut -d ' ' -f1)
    [ -n "$contract_hash" ] || return 1
    generated_at=$(date '+%Y-%m-%d %H:%M:%S')
    tmp_file="$OUTPUT_ENVELOPE.partial.$$"

    {
        printf '%s\n' 'schema_version=environment-contract-envelope.v1'
        printf '%s\n' "pipeline_run_id=$RUN_ID"
        printf '%s\n' 'project_id=alfa_device_ctrl'
        printf '%s\n' "project_root=$PROJECT_ROOT_EXPECTED"
        printf '%s\n' 'stage_id=gate4'
        printf '%s\n' 'producer=tools/environment_contract.sh'
        printf '%s\n' "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "artifact_path=$PIPELINE_ROOT_REL/$RUN_ID/gate4/environment_contract.txt"
        printf '%s\n' "sha256=$contract_hash"
        printf '%s\n' 'completion_status=COMPLETE'
        printf '%s\n' "created_at=$generated_at"
        printf '%s\n' "input_artifacts=$EDE_ARTIFACT_REL,$CDE_ARTIFACT_REL,$GATE3_ARTIFACT_REL,$PROFILE_AUTHORITY_PATH"
    } > "$tmp_file" || return 1

    mv "$tmp_file" "$OUTPUT_ENVELOPE" 2>/dev/null || {
        rm -f "$tmp_file"
        return 1
    }
    printf '%s\n' "ENVIRONMENT_CONTRACT_SHA256=$contract_hash"
    return 0
}

write_failure_output() {
    local result="$1"
    local status="$2"
    local reason="$3"
    local generated_at

    failure_output_allowed || return 1
    generated_at=$(date '+%Y-%m-%d %H:%M:%S')
    write_contract_payload "$result" "$status" "$reason" "$generated_at" >/dev/null 2>&1 || return 1
    return 0
}

fail_blocked() {
    local reason="$1"
    write_failure_output BLOCKED BLOCKED "$reason" >/dev/null 2>&1 || true
    printf '%s\n' 'GATE4_STATUS=BLOCKED'
    printf '%s\n' 'GATE4_RESULT=BLOCKED'
    printf '%s\n' "GATE4_FAILURE_REASON=$reason"
    if [ -n "$OUTPUT_FILE" ] && [ -s "$OUTPUT_FILE" ]; then
        printf '%s\n' "ENVIRONMENT_CONTRACT=$OUTPUT_FILE"
    fi
    return "$GATE4_RC_BLOCKED"
}

fail_invalid() {
    local reason="$1"
    write_failure_output INVALID INVALID "$reason" >/dev/null 2>&1 || true
    printf '%s\n' 'GATE4_STATUS=INVALID'
    printf '%s\n' 'GATE4_RESULT=INVALID'
    printf '%s\n' "GATE4_FAILURE_REASON=$reason"
    if [ -n "$OUTPUT_FILE" ] && [ -s "$OUTPUT_FILE" ]; then
        printf '%s\n' "ENVIRONMENT_CONTRACT=$OUTPUT_FILE"
    fi
    return "$GATE4_RC_INVALID"
}

main() {
    local script_dir canonical_root
    local authority_count

    if [ "$#" -ne 2 ]; then
        fail_blocked EXACT_RUN_DIR_AND_PROFILE_FILE_REQUIRED
        return $?
    fi

    script_dir=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)
    canonical_root=$(CDPATH= cd -- "$script_dir/.." 2>/dev/null && pwd)
    [ "$canonical_root" = "$PROJECT_ROOT_EXPECTED" ] || { fail_blocked MASTER_PATH_MISMATCH; return $?; }
    PROJECT_ROOT="$canonical_root"

    RUN_DIR="$1"
    PROFILE_FILE="$2"
    RUN_ID=$(basename "$RUN_DIR")
    STATE_FILE="$RUN_DIR/pipeline_state.txt"
    MANIFEST_FILE="$RUN_DIR/manifest.txt"
    PROFILE_AUTHORITY_FILE="$PROJECT_ROOT/$AUTHORITY_REL"

    case "$RUN_DIR" in
        "$PROJECT_ROOT_EXPECTED/$PIPELINE_ROOT_REL/"*) ;;
        *) fail_blocked RUN_DIR_OUTSIDE_MASTER_PIPELINE_ROOT; return $? ;;
    esac
    [ "$RUN_ID" != '.' ] && [ "$RUN_ID" != '..' ] || { fail_blocked RUN_ID_INVALID; return $?; }

    case "$PROFILE_FILE" in
        "$PROJECT_ROOT_EXPECTED/$PROFILE_ROOT_REL/profile_"*.txt) ;;
        *) fail_blocked PROFILE_FILE_OUTSIDE_RUNTIME_PROFILE_ROOT; return $? ;;
    esac

    if ! validate_state_and_manifest; then
        fail_blocked CURRENT_RUN_STATE_MANIFEST_OR_STAGE_VALIDATION_FAILED
        return $?
    fi

    if ! validate_profile_authority; then
        fail_blocked PROFILE_AUTHORITY_VALIDATION_FAILED
        return $?
    fi

    if ! write_contract_payload VALID COMPLETE NONE "$(date '+%Y-%m-%d %H:%M:%S')"; then
        fail_blocked ENVIRONMENT_CONTRACT_WRITE_FAILED
        return $?
    fi

    if ! write_contract_envelope >/dev/null; then
        fail_blocked ENVIRONMENT_CONTRACT_ENVELOPE_WRITE_FAILED
        return $?
    fi

    printf '%s\n' 'GATE4_STATUS=PASS'
    printf '%s\n' 'GATE4_RESULT=VALID'
    printf '%s\n' "GATE4_RUN_ID=$RUN_ID"
    printf '%s\n' "ENVIRONMENT_CONTRACT=$OUTPUT_FILE"
    printf '%s\n' "ENVIRONMENT_CONTRACT_ENVELOPE=$OUTPUT_ENVELOPE"
    printf '%s\n' 'PROBING_REEXECUTED=NO'
    return "$GATE4_RC_VALID"
}

main "$@"
