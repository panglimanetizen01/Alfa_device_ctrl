#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/task/task_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME ACTION V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[TASK]"

if [ -n "$LATEST" ] && grep -q '^task_status=PASS$' "$LATEST"
then
    echo "task_status=PASS"

    echo
    echo "[ACTION]"
    echo "action_name=runtime_self_action"
    echo "action_status=PASS"
else
    echo "task_status=BLOCKED"

    echo
    echo "[ACTION]"
    echo "action_name=runtime_self_action"
    echo "action_status=BLOCKED"
fi

echo
echo "[END]"
