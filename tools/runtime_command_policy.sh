#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/kernel/kernel_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME COMMAND POLICY V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[KERNEL]"

if [ -n "$LATEST" ] && grep -q '^kernel_status=PASS$' "$LATEST"
then
    echo "kernel_status=PASS"

    echo
    echo "[POLICY]"

    ALLOWED_CMD='pwd'
    BLOCKED_CMD='uname'

    echo "allowed_command=$ALLOWED_CMD"
    echo "allowed_status=ALLOWED"

    echo "blocked_command=$BLOCKED_CMD"
    echo "blocked_status=BLOCKED"
else
    echo "kernel_status=BLOCKED"

    echo
    echo "[POLICY]"
    echo "allowed_command=pwd"
    echo "allowed_status=BLOCKED"
    echo "blocked_command=uname"
    echo "blocked_status=BLOCKED"
fi

echo
echo "[EXECUTION CHECK]"
echo "policy_execution=NONE"

echo
echo "[END]"
