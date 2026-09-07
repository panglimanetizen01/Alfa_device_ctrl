#!/usr/bin/env bash

set -u

RC_VALID=0
RC_BLOCKED=20
RC_INVALID=21
ROOT=''
RUN_DIR=''
RUN_ID=''
STATE_FILE=''
MANIFEST_FILE=''
PROFILE_FILE=''
AUTHORITY_FILE=''
SOURCE_COMMIT='UNKNOWN'
EDE_REL=''; CDE_REL=''; G3_REL=''
EDE_ENV=''; CDE_ENV=''; G3_ENV=''
EDE_HASH=''; CDE_HASH=''; G3_HASH=''
OUT=''; ENVELOPE=''

read_field(){ sed -n "s/^$2=//p" "$1" 2>/dev/null | sed -n '1p'; }
count_field(){ grep -c "^$2=" "$1" 2>/dev/null || true; }
sha_ok(){ local f="$1" e="$2" expected="$3" actual; [ -s "$f" ]&&[ -s "$e" ]||return 1; actual=$(sha256sum "$f"|cut -d' ' -f1); [ "$actual" = "$(read_field "$e" sha256)" ]&&[ "$actual" = "$expected" ]; }

validate_stage(){
    local stage="$1" rel="$2" env="$3" hash="$4" producer="$5" f="$ROOT/$rel" e="$ROOT/$env"
    [ -n "$rel" ]&&[ -n "$env" ]&&[ -n "$hash" ] || return 1
    case "$rel" in artifacts/pipeline/$RUN_ID/*) ;; *) return 1;; esac
    case "$env" in artifacts/pipeline/$RUN_ID/*) ;; *) return 1;; esac
    [ -f "$f" ]&&[ -f "$e" ] || return 1
    [ "$(read_field "$e" schema_version)" = artifact-envelope.v1 ] || return 1
    [ "$(read_field "$e" stage_id)" = "$stage" ] || return 1
    [ "$(read_field "$e" pipeline_run_id)" = "$RUN_ID" ] || return 1
    [ "$(read_field "$e" source_commit)" = "$SOURCE_COMMIT" ] || return 1
    [ "$(read_field "$e" completion_status)" = COMPLETE ] || return 1
    [ "$(read_field "$e" artifact_path)" = "$rel" ] || return 1
    [ "$(read_field "$e" producer)" = "$producer" ] || return 1
    sha_ok "$f" "$e" "$hash"
}

validate_state(){
    [ "$(read_field "$STATE_FILE" schema_version)" = pipeline-state.v1 ] || return 1
    [ "$(read_field "$STATE_FILE" pipeline_run_id)" = "$RUN_ID" ] || return 1
    [ "$(read_field "$STATE_FILE" project_id)" = alfa_device_ctrl ] || return 1
    [ "$(read_field "$STATE_FILE" project_root)" = "$ROOT" ] || return 1
    SOURCE_COMMIT=$(read_field "$STATE_FILE" source_commit)
    [ -n "$SOURCE_COMMIT" ]&&[ "$SOURCE_COMMIT" != UNKNOWN ] || return 1
    [ "$(read_field "$STATE_FILE" pipeline_status)" = RUNNING ] || return 1
    [ "$(read_field "$STATE_FILE" completion_status)" = RUNNING ] || return 1
    [ "$(read_field "$STATE_FILE" contract_result)" = BLOCKED ] || return 1
    [ "$(read_field "$STATE_FILE" ede_rc)" = 0 ]&&[ "$(read_field "$STATE_FILE" cde_rc)" = 0 ]&&[ "$(read_field "$STATE_FILE" gate3_rc)" = 0 ] || return 1
    [ "$(read_field "$MANIFEST_FILE" manifest_schema_version)" = manifest.v1 ] || return 1
    [ "$(read_field "$MANIFEST_FILE" pipeline_run_id)" = "$RUN_ID" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" source_commit)" = "$SOURCE_COMMIT" ] || return 1
    [ "$(read_field "$MANIFEST_FILE" project_id)" = alfa_device_ctrl ] || return 1
    [ "$(read_field "$MANIFEST_FILE" project_root)" = "$ROOT" ] || return 1
    EDE_REL=$(read_field "$MANIFEST_FILE" ede_artifact); CDE_REL=$(read_field "$MANIFEST_FILE" cde_artifact); G3_REL=$(read_field "$MANIFEST_FILE" gate3_artifact)
    EDE_ENV=$(read_field "$MANIFEST_FILE" ede_envelope); CDE_ENV=$(read_field "$MANIFEST_FILE" cde_envelope); G3_ENV=$(read_field "$MANIFEST_FILE" gate3_envelope)
    EDE_HASH=$(read_field "$MANIFEST_FILE" ede_sha256); CDE_HASH=$(read_field "$MANIFEST_FILE" cde_sha256); G3_HASH=$(read_field "$MANIFEST_FILE" gate3_sha256)
    [ "$(read_field "$MANIFEST_FILE" ede_stage_id)" = ede ]&&[ "$(read_field "$MANIFEST_FILE" cde_stage_id)" = cde ]&&[ "$(read_field "$MANIFEST_FILE" gate3_stage_id)" = gate3 ] || return 1
    validate_stage ede "$EDE_REL" "$EDE_ENV" "$EDE_HASH" tools/ede.sh || return 1
    validate_stage cde "$CDE_REL" "$CDE_ENV" "$CDE_HASH" tools/cde.sh || return 1
    validate_stage gate3 "$G3_REL" "$G3_ENV" "$G3_HASH" tools/execution_capability.sh || return 1
}

validate_gate3(){
    local f="$ROOT/$G3_REL" key v
    for key in EXEC_PRIVATE SHARED_STORAGE_IO SCRIPT_BASH SCRIPT_PYTHON PROCESS_SPAWN; do
        [ "$(count_field "$f" "$key")" = 1 ] || return 1
        v=$(read_field "$f" "$key")
        case "${v%% |*}" in PASS|ERROR) ;; *) return 1;; esac
        [ "${v%% |*}" != UNKNOWN ] || return 1
    done
}

validate_profile(){
    local count key value authority_count authority_path authority_hash authority_source authority_run authority_status
    [ -f "$AUTHORITY_FILE" ] || return 1
    authority_count=$(awk -v target="$RUN_ID" 'BEGIN{RS="";n=0}{r=0;a=0;s=0;for(i=1;i<=NF;i++){if($i=="pipeline_run_id="target)r=1;if($i=="authoritative=TRUE")a=1;if($i=="profile_status=VALID")s=1}if(r&&a&&s)n++}END{print n}' "$AUTHORITY_FILE")
    [ "$authority_count" = 1 ] || return 1
    authority_path=$(awk -v target="$RUN_ID" 'BEGIN{RS=""}{r=0;a=0;v="";for(i=1;i<=NF;i++){split($i,p,"=");if(p[1]=="pipeline_run_id"&&p[2]==target)r=1;if($i=="authoritative=TRUE")a=1;if(p[1]=="profile_path")v=p[2]}if(r&&a)print v}' "$AUTHORITY_FILE"|sed -n '1p')
    authority_hash=$(awk -v target="$RUN_ID" 'BEGIN{RS=""}{r=0;a=0;v="";for(i=1;i<=NF;i++){split($i,p,"=");if(p[1]=="pipeline_run_id"&&p[2]==target)r=1;if($i=="authoritative=TRUE")a=1;if(p[1]=="profile_sha256")v=p[2]}if(r&&a)print v}' "$AUTHORITY_FILE"|sed -n '1p')
    authority_source=$(awk -v target="$RUN_ID" 'BEGIN{RS=""}{r=0;a=0;v="";for(i=1;i<=NF;i++){split($i,p,"=");if(p[1]=="pipeline_run_id"&&p[2]==target)r=1;if($i=="authoritative=TRUE")a=1;if(p[1]=="source_commit")v=p[2]}if(r&&a)print v}' "$AUTHORITY_FILE"|sed -n '1p')
    authority_run=$(awk -v target="$RUN_ID" 'BEGIN{RS=""}{r=0;a=0;for(i=1;i<=NF;i++){if($i=="pipeline_run_id="target)r=1;if($i=="authoritative=TRUE")a=1}if(r&&a)print target}' "$AUTHORITY_FILE"|sed -n '1p')
    authority_status=VALID
    case "$authority_path" in artifacts/runtime_profiles/profile_*.txt) ;; *) return 1;; esac
    [ "$authority_run" = "$RUN_ID" ]&&[ "$authority_source" = "$SOURCE_COMMIT" ]&&[ -n "$authority_hash" ] || return 1
    PROFILE_FILE="$ROOT/$authority_path"
    [ -f "$PROFILE_FILE" ] || return 1
    [ "$(sha256sum "$PROFILE_FILE"|cut -d' ' -f1)" = "$authority_hash" ] || return 1
    [ "$(read_field "$PROFILE_FILE" PROFILE_VERSION)" = 1 ] || return 1
    [ "$(read_field "$PROFILE_FILE" PIPELINE_RUN_ID)" = "$RUN_ID" ]&&[ "$(read_field "$PROFILE_FILE" SOURCE_COMMIT)" = "$SOURCE_COMMIT" ] || return 1
    for key in DEVICE_CLASS ANDROID_HOST CONTAINER_ENVIRONMENT CPU_ARCH STORAGE_READ STORAGE_WRITE EXEC_PRIVATE SHARED_STORAGE_IO EXEC_SHARED NETWORK_DNS PYTHON3 GIT JAVA JAVAC GRADLE; do
        count=$(count_field "$PROFILE_FILE" "$key"); [ "$count" = 1 ] || return 1
        value=$(read_field "$PROFILE_FILE" "$key"); [ -n "$value" ]&&[ "$value" != UNKNOWN ] || return 1
        case "$key" in DEVICE_CLASS|ANDROID_HOST|CONTAINER_ENVIRONMENT|CPU_ARCH) ;; *) case "${value%% |*}" in PASS|ERROR) ;; *) return 1;; esac;; esac
    done
}

write_contract(){
    local tmp hash
    mkdir -p "$RUN_DIR/gate4" || return 1
    OUT="$RUN_DIR/gate4/environment_contract.txt"; ENVELOPE="$RUN_DIR/gate4/environment_contract.envelope.txt"; tmp="$OUT.partial.$$"
    {
        printf '%s\n' schema_version=environment-contract.v1 "pipeline_run_id=$RUN_ID" project_id=alfa_device_ctrl "project_root=$ROOT" "source_commit=$SOURCE_COMMIT"
        printf '%s\n' "input_state=$STATE_FILE" "input_manifest=$MANIFEST_FILE" "ede_artifact=$EDE_REL" "ede_envelope=$EDE_ENV" "ede_sha256=$EDE_HASH" "cde_artifact=$CDE_REL" "cde_envelope=$CDE_ENV" "cde_sha256=$CDE_HASH" "gate3_artifact=$G3_REL" "gate3_envelope=$G3_ENV" "gate3_sha256=$G3_HASH"
        printf '%s\n' "runtime_profile=$PROFILE_FILE" "profile_authority=$AUTHORITY_FILE" "profile_sha256=$(sha256sum "$PROFILE_FILE"|cut -d' ' -f1)" "device_class=$(read_field "$PROFILE_FILE" DEVICE_CLASS)" "android_host=$(read_field "$PROFILE_FILE" ANDROID_HOST)" "container_environment=$(read_field "$PROFILE_FILE" CONTAINER_ENVIRONMENT)" "cpu_arch=$(read_field "$PROFILE_FILE" CPU_ARCH)"
        printf '%s\n' "storage_read=$(read_field "$PROFILE_FILE" STORAGE_READ)" "storage_write=$(read_field "$PROFILE_FILE" STORAGE_WRITE)" "exec_private=$(read_field "$PROFILE_FILE" EXEC_PRIVATE)" "shared_storage_io=$(read_field "$PROFILE_FILE" SHARED_STORAGE_IO)" "exec_shared=$(read_field "$PROFILE_FILE" EXEC_SHARED)" "network_dns=$(read_field "$PROFILE_FILE" NETWORK_DNS)"
        printf '%s\n' "python3=$(read_field "$PROFILE_FILE" PYTHON3)" "git=$(read_field "$PROFILE_FILE" GIT)" "java=$(read_field "$PROFILE_FILE" JAVA)" "javac=$(read_field "$PROFILE_FILE" JAVAC)" "gradle=$(read_field "$PROFILE_FILE" GRADLE)" pipeline_status=RUNNING completion_status=COMPLETE contract_result=VALID failure_status=COMPLETE failure_reason=NONE "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
    } > "$tmp" || return 1
    mv "$tmp" "$OUT" || return 1
    hash=$(sha256sum "$OUT"|cut -d' ' -f1)
    {
        printf '%s\n' schema_version=environment-contract-envelope.v1 "pipeline_run_id=$RUN_ID" project_id=alfa_device_ctrl "project_root=$ROOT" stage_id=gate4 producer=tools/environment_contract.sh "source_commit=$SOURCE_COMMIT" "artifact_path=artifacts/pipeline/$RUN_ID/gate4/environment_contract.txt" "sha256=$hash" completion_status=COMPLETE "created_at=$(date '+%Y-%m-%d %H:%M:%S')" "input_artifacts=$EDE_REL,$CDE_REL,$G3_REL,$AUTHORITY_FILE"
    } > "$ENVELOPE" || return 1
    printf '%s\n' "ENVIRONMENT_CONTRACT_SHA256=$hash"
}

main(){
    [ "$#" -eq 2 ] || { printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=EXACT_RUN_DIR_AND_PROFILE_FILE_REQUIRED; return $RC_BLOCKED; }
    ROOT=$(realpath -e "$(dirname "${BASH_SOURCE[0]}")/.." 2>/dev/null || true)
    RUN_DIR=$(realpath -e "$1" 2>/dev/null || true); RUN_ID=$(basename "$RUN_DIR"); PROFILE_FILE="$2"
    [ -n "$ROOT" ]&&[ -d "$RUN_DIR" ] || { printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=PATH_UNRESOLVED; return $RC_BLOCKED; }
    case "$RUN_DIR" in "$ROOT/artifacts/pipeline/"*) ;; *) printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=RUN_DIR_OUTSIDE_MASTER_PIPELINE_ROOT; return $RC_BLOCKED;; esac
    case "$PROFILE_FILE" in "$ROOT/artifacts/runtime_profiles/profile_"*.txt) ;; *) printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=PROFILE_FILE_OUTSIDE_RUNTIME_PROFILE_ROOT; return $RC_BLOCKED;; esac
    STATE_FILE="$RUN_DIR/pipeline_state.txt"; MANIFEST_FILE="$RUN_DIR/manifest.txt"; AUTHORITY_FILE="$ROOT/artifacts/runtime_profiles/profile_authority.txt"
    validate_state || { printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=CURRENT_RUN_STATE_MANIFEST_VALIDATION_FAILED; return $RC_BLOCKED; }
    validate_gate3 || { printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=GATE3_PAYLOAD_SCHEMA_MISMATCH; return $RC_BLOCKED; }
    validate_profile || { printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=PROFILE_AUTHORITY_VALIDATION_FAILED; return $RC_BLOCKED; }
    write_contract || { printf '%s\n' GATE4_STATUS=BLOCKED GATE4_RESULT=BLOCKED GATE4_FAILURE_REASON=ENVIRONMENT_CONTRACT_WRITE_FAILED; return $RC_BLOCKED; }
    printf '%s\n' GATE4_STATUS=PASS GATE4_RESULT=VALID "GATE4_RUN_ID=$RUN_ID" "ENVIRONMENT_CONTRACT=$OUT" "ENVIRONMENT_CONTRACT_ENVELOPE=$ENVELOPE" PROBING_REEXECUTED=NO
    return $RC_VALID
}

main "$@"
