#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

KERNEL=$(ls -1t artifacts/kernel/kernel_*.txt 2>/dev/null | head -1)
POLICY=$(ls -1t artifacts/policy/policy_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME EXECUTION AUTHORIZATION V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[KERNEL]"

if [ -n "$KERNEL" ] && grep -q '^kernel_status=PASS$' "$KERNEL"
then
    echo "kernel_status=PASS"
else
    echo "kernel_status=BLOCKED"
fi

echo
echo "[POLICY]"

if [ -n "$POLICY" ] && grep -q '^allowed_status=ALLOWED$' "$POLICY"
then
    echo "policy_status=ALLOWED"
    echo "authorization_status=AUTHORIZED"
elif [ -n "$POLICY" ] && grep -q '^blocked_status=BLOCKED$' "$POLICY"
then
    echo "policy_status=BLOCKED"
    echo "authorization_status=DENIED"
else
    echo "policy_status=UNKNOWN"
    echo "authorization_status=DENIED"
fi

echo
echo "[EXECUTION CHECK]"
echo "command_execution=NONE"

echo
echo "[END]"
