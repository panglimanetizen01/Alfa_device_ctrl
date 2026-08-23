#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/orchestrator/orchestrator_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME KERNEL V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[ORCHESTRATOR]"

if [ -n "$LATEST" ] && grep -q '^orchestrator_status=PASS$' "$LATEST"
then
    echo "orchestrator_status=PASS"

    echo
    echo "[KERNEL]"
    echo "kernel_name=runtime_self_kernel"
    echo "kernel_status=PASS"
else
    echo "orchestrator_status=BLOCKED"

    echo
    echo "[KERNEL]"
    echo "kernel_name=runtime_self_kernel"
    echo "kernel_status=BLOCKED"
fi

echo
echo "[END]"
