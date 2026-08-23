#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

KERNEL=$(ls -1t artifacts/kernel/kernel_*.txt 2>/dev/null | head -1)
AUTH=$(ls -1t artifacts/authorization/authorization_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME EXECUTION V1 ==="
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
echo "[AUTHORIZATION]"

if [ -n "$AUTH" ] && grep -q '^authorization_status=AUTHORIZED$' "$AUTH"
then
    echo "authorization_status=AUTHORIZED"

    echo
    echo "[EXECUTION]"

    CMD='pwd'
    RESULT=$(pwd 2>/dev/null)

    if [ -n "$RESULT" ]
    then
        echo "command=$CMD"
        echo "command_result=$RESULT"
        echo "execution_status=PASS"
    else
        echo "command=$CMD"
        echo "command_result=ERROR"
        echo "execution_status=ERROR"
    fi
else
    echo "authorization_status=DENIED"

    echo
    echo "[EXECUTION]"
    echo "command=pwd"
    echo "command_result=NOT_EXECUTED"
    echo "execution_status=BLOCKED"
fi

echo
echo "[END]"
