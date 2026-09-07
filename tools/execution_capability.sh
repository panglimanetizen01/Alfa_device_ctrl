#!/usr/bin/env bash

set -u

main() {
    local SHARED_ROOT="${ALFA_EXEC_SHARED_ROOT:-/storage/emulated/0}"
    local PRIVATE_ROOT="${TMPDIR:-${PREFIX:-$HOME}/tmp}"
    local P S
    local EXEC_PRIVATE_STATUS EXEC_SHARED_STATUS SCRIPT_BASH_STATUS SCRIPT_PYTHON_STATUS PROCESS_SPAWN_STATUS
    local OVERALL_STATUS=PASS

    echo "=== ALFA EXECUTION CAPABILITY V1 ==="
    echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
    echo "execution_path=$(pwd 2>/dev/null || echo UNKNOWN)"
    echo "shared_root=$SHARED_ROOT"
    echo
    echo "[CAPABILITIES]"

    P="$PRIVATE_ROOT/alfa_exec_private.$$"
    if printf '#!/bin/sh\nprintf EXEC_PRIVATE_OK\\n\n' > "$P" 2>/dev/null \
       && chmod +x "$P" 2>/dev/null \
       && [ "$("$P" 2>/dev/null)" = 'EXEC_PRIVATE_OK' ]; then
        EXEC_PRIVATE_STATUS=PASS
    else
        EXEC_PRIVATE_STATUS=ERROR
        OVERALL_STATUS=ERROR
    fi
    rm -f "$P" 2>/dev/null || true
    echo "EXEC_PRIVATE=$EXEC_PRIVATE_STATUS | verification=private file execution | scope=current_environment"

    S="$SHARED_ROOT/.alfa_exec_shared.$$"
    if [ -d "$SHARED_ROOT" ] \
       && printf '#!/bin/sh\nprintf EXEC_SHARED_OK\\n\n' > "$S" 2>/dev/null \
       && chmod +x "$S" 2>/dev/null \
       && [ "$("$S" 2>/dev/null)" = 'EXEC_SHARED_OK' ]; then
        EXEC_SHARED_STATUS=PASS
    else
        EXEC_SHARED_STATUS=ERROR
        OVERALL_STATUS=ERROR
    fi
    rm -f "$S" 2>/dev/null || true
    echo "EXEC_SHARED=$EXEC_SHARED_STATUS | verification=shared-storage file execution | scope=current_environment"

    if printf 'printf SCRIPT_BASH_OK\\n\n' | bash 2>/dev/null | grep -qx 'SCRIPT_BASH_OK'; then
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
       && [ "$(sh -c 'printf PROCESS_SPAWN_OK' 2>/dev/null)" = 'PROCESS_SPAWN_OK' ]; then
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
