#!/usr/bin/env bash

set -u

main() {
    local SHARED_ROOT="${ALFA_EXEC_SHARED_ROOT:-/storage/emulated/0}"
    local PRIVATE_ROOT="${TMPDIR:-${PREFIX:-$HOME}/tmp}"
    local P S
    local EXEC_PRIVATE_STATUS SHARED_STORAGE_IO_STATUS SCRIPT_BASH_STATUS SCRIPT_PYTHON_STATUS PROCESS_SPAWN_STATUS
    local SHARED_STORAGE_IO_REASON=PASS
    local OVERALL_STATUS=PASS

    echo "=== ALFA EXECUTION CAPABILITY V2 ==="
    echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
    echo "execution_path=$(pwd 2>/dev/null || echo UNKNOWN)"
    echo "shared_root=$SHARED_ROOT"
    echo
    echo "[CAPABILITIES]"

    # Execution semantics are tested only in the private execution-capable workspace.
    P="$PRIVATE_ROOT/alfa_exec_private.$$"
    if printf '%s\n' '#!/bin/sh' 'printf "%s\\n" EXEC_PRIVATE_OK' > "$P" 2>/dev/null \
       && chmod +x "$P" 2>/dev/null \
       && [ "$("$P" 2>/dev/null)" = 'EXEC_PRIVATE_OK' ]; then
        EXEC_PRIVATE_STATUS=PASS
    else
        EXEC_PRIVATE_STATUS=ERROR
        OVERALL_STATUS=ERROR
    fi
    rm -f "$P" 2>/dev/null || true
    echo "EXEC_PRIVATE=$EXEC_PRIVATE_STATUS | verification=private file execution | scope=current_execution_workspace"

    # Shared storage is deliberately tested as data I/O, never as executable code.
    S="$SHARED_ROOT/.alfa_storage_io.$$"
    SHARED_VALUE='ALFA_SHARED_STORAGE_IO_OK'
    if [ ! -d "$SHARED_ROOT" ]; then
        SHARED_STORAGE_IO_STATUS=ERROR
        SHARED_STORAGE_IO_REASON=ROOT_NOT_DIRECTORY
    elif ! printf '%s\n' "$SHARED_VALUE" > "$S" 2>/dev/null; then
        SHARED_STORAGE_IO_STATUS=ERROR
        SHARED_STORAGE_IO_REASON=WRITE_FAILED
    elif [ "$(cat "$S" 2>/dev/null)" != "$SHARED_VALUE" ]; then
        SHARED_STORAGE_IO_STATUS=ERROR
        SHARED_STORAGE_IO_REASON=READ_OR_CONTENT_MISMATCH
    elif ! rm -f "$S" 2>/dev/null; then
        SHARED_STORAGE_IO_STATUS=ERROR
        SHARED_STORAGE_IO_REASON=DELETE_FAILED
    elif [ -e "$S" ]; then
        SHARED_STORAGE_IO_STATUS=ERROR
        SHARED_STORAGE_IO_REASON=DELETE_NOT_OBSERVED
    else
        SHARED_STORAGE_IO_STATUS=PASS
    fi
    if [ "$SHARED_STORAGE_IO_STATUS" = ERROR ]; then
        OVERALL_STATUS=ERROR
        rm -f "$S" 2>/dev/null || true
    fi
    echo "SHARED_STORAGE_IO=$SHARED_STORAGE_IO_STATUS | verification=create-write-read-delete | scope=shared_storage_data_only"
    echo "SHARED_STORAGE_IO_REASON=$SHARED_STORAGE_IO_REASON"

    if printf '%s\n' 'printf "%s\\n" SCRIPT_BASH_OK' | bash 2>/dev/null | grep -qx 'SCRIPT_BASH_OK'; then
        SCRIPT_BASH_STATUS=PASS
    else
        SCRIPT_BASH_STATUS=ERROR
        OVERALL_STATUS=ERROR
    fi
    echo "SCRIPT_BASH=$SCRIPT_BASH_STATUS | verification=bash script execution | scope=current_environment"

    if command -v python3 >/dev/null 2>&1 \
       && printf 'print("SCRIPT_PYTHON_OK")\n' | python3 2>/dev/null | grep -qx 'SCRIPT_PYTHON_OK'; then
        SCRIPT_PYTHON_STATUS=PASS
    else
        SCRIPT_PYTHON_STATUS=ERROR
        OVERALL_STATUS=ERROR
    fi
    echo "SCRIPT_PYTHON=$SCRIPT_PYTHON_STATUS | verification=python3 script execution | scope=current_environment"

    if command -v sh >/dev/null 2>&1 \
       && [ "$(sh -c 'printf "%s" PROCESS_SPAWN_OK' 2>/dev/null)" = 'PROCESS_SPAWN_OK' ]; then
        PROCESS_SPAWN_STATUS=PASS
    else
        PROCESS_SPAWN_STATUS=ERROR
        OVERALL_STATUS=ERROR
    fi
    echo "PROCESS_SPAWN=$PROCESS_SPAWN_STATUS | verification=subprocess execution | scope=current_environment"

    echo
    echo "GATE3_STATUS=$OVERALL_STATUS"
    echo "[END]"

    [ "$OVERALL_STATUS" = PASS ]
}

main "$@"
