#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/session/session_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME TASK V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[SESSION]"

if [ -n "$LATEST" ] && grep -q '^session_status=PASS$' "$LATEST"
then
    echo "session_status=PASS"

    echo
    echo "[TASK]"
    echo "task_name=runtime_self_test"
    echo "task_status=PASS"
else
    echo "session_status=BLOCKED"

    echo
    echo "[TASK]"
    echo "task_name=runtime_self_test"
    echo "task_status=BLOCKED"
fi

echo
echo "[END]"
