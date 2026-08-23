#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/execution/execution_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME EXECUTION RESULT V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[EXECUTION]"

if [ -n "$LATEST" ]
then
    EXEC_STATUS=$(grep '^execution_status=' "$LATEST" | head -1 | cut -d= -f2-)
    CMD=$(grep '^command=' "$LATEST" | head -1 | cut -d= -f2-)
    RESULT=$(grep '^command_result=' "$LATEST" | head -1 | cut -d= -f2-)

    echo "execution_status=${EXEC_STATUS:-UNKNOWN}"
    echo "command=${CMD:-UNKNOWN}"
    echo "command_result=${RESULT:-UNKNOWN}"

    case "${EXEC_STATUS:-UNKNOWN}" in
        PASS)
            echo "result_status=PASS"
            ;;
        ERROR)
            echo "result_status=ERROR"
            ;;
        BLOCKED)
            echo "result_status=BLOCKED"
            ;;
        *)
            echo "result_status=ERROR"
            ;;
    esac
else
    echo "execution_status=UNKNOWN"
    echo "command=UNKNOWN"
    echo "command_result=UNKNOWN"
    echo "result_status=ERROR"
fi

echo
echo "[END]"
