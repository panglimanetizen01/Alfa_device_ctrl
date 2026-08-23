#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/bootstrap/bootstrap_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME SESSION V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[BOOTSTRAP]"

if [ -n "$LATEST" ] && grep -q '^bootstrap_status=PASS$' "$LATEST"
then
    echo "bootstrap_status=PASS"
    SESSION_STATUS="PASS"
else
    echo "bootstrap_status=BLOCKED"
    SESSION_STATUS="BLOCKED"
fi

echo
echo "[SESSION]"
echo "session_status=$SESSION_STATUS"

echo
echo "[END]"
