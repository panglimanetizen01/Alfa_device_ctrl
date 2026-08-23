#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/workflow/workflow_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME ORCHESTRATOR V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[WORKFLOW]"

if [ -n "$LATEST" ] && grep -q '^workflow_status=PASS$' "$LATEST"
then
    echo "workflow_status=PASS"

    echo
    echo "[ORCHESTRATOR]"
    echo "orchestrator_name=runtime_self_orchestrator"
    echo "orchestrator_status=PASS"
else
    echo "workflow_status=BLOCKED"

    echo
    echo "[ORCHESTRATOR]"
    echo "orchestrator_name=runtime_self_orchestrator"
    echo "orchestrator_status=BLOCKED"
fi

echo
echo "[END]"
