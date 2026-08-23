#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/result/result_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME RESULT CONSUMER V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[RESULT]"

if [ -n "$LATEST" ]
then
    RESULT_STATUS=$(grep '^result_status=' "$LATEST" | head -1 | cut -d= -f2-)
    CMD=$(grep '^command=' "$LATEST" | head -1 | cut -d= -f2-)
    RESULT=$(grep '^command_result=' "$LATEST" | head -1 | cut -d= -f2-)

    echo "result_status=${RESULT_STATUS:-UNKNOWN}"
    echo "command=${CMD:-UNKNOWN}"
    echo "command_result=${RESULT:-UNKNOWN}"

    case "${RESULT_STATUS:-UNKNOWN}" in
        PASS)
            echo "consume_status=ACCEPTED"
            ;;
        ERROR|BLOCKED)
            echo "consume_status=REJECTED"
            ;;
        *)
            echo "consume_status=REJECTED"
            ;;
    esac
else
    echo "result_status=UNKNOWN"
    echo "command=UNKNOWN"
    echo "command_result=UNKNOWN"
    echo "consume_status=REJECTED"
fi

echo
echo "[EXECUTION CHECK]"
echo "consumer_execution=NONE"

echo
echo "[END]"
