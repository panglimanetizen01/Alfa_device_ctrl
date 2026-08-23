#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/kernel/kernel_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME COMMAND V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[KERNEL]"

if [ -n "$LATEST" ] && grep -q '^kernel_status=PASS$' "$LATEST"
then
    echo "kernel_status=PASS"

    echo
    echo "[COMMAND]"

    CMD='pwd'
    RESULT=$(pwd 2>/dev/null)

    echo "command=$CMD"
    echo "command_result=$RESULT"
    echo "command_status=PASS"
else
    echo "kernel_status=BLOCKED"

    echo
    echo "[COMMAND]"
    echo "command_status=BLOCKED"
fi

echo
echo "[END]"
