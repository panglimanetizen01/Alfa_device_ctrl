#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/action/action_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME WORKFLOW V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[ACTION]"

if [ -n "$LATEST" ] && grep -q '^action_status=PASS$' "$LATEST"
then
    echo "action_status=PASS"

    echo
    echo "[WORKFLOW]"
    echo "workflow_name=runtime_self_workflow"
    echo "workflow_status=PASS"
else
    echo "action_status=BLOCKED"

    echo
    echo "[WORKFLOW]"
    echo "workflow_name=runtime_self_workflow"
    echo "workflow_status=BLOCKED"
fi

echo
echo "[END]"
