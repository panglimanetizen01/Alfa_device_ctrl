#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/decision/decision_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME BOOTSTRAP V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[DECISION]"

if [ -n "$LATEST" ] && grep -q '^decision=READY$' "$LATEST"
then
    echo "decision=READY"
    BOOTSTRAP_STATUS="PASS"
else
    echo "decision=NOT_READY"
    BOOTSTRAP_STATUS="BLOCKED"
fi

echo
echo "[BOOTSTRAP]"
echo "bootstrap_status=$BOOTSTRAP_STATUS"

echo
echo "[END]"
