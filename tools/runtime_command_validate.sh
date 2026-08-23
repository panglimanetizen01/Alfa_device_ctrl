#!/usr/bin/env bash

set -u

ROOT=/storage/emulated/0/Alfa_device_ctrl
cd "$ROOT"

LATEST=$(ls -1t artifacts/kernel/kernel_*.txt 2>/dev/null | head -1)

echo "=== ALFA RUNTIME COMMAND VALIDATION V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd)"

echo
echo "[KERNEL]"

if [ -n "$LATEST" ] && grep -q '^kernel_status=PASS$' "$LATEST"
then
    echo "kernel_status=PASS"

    echo
    echo "[VALIDATION]"

    CMD='pwd'
    echo "command=$CMD"

    case "$CMD" in
        pwd)
            echo "validation_status=ALLOWED"
            ;;
        *)
            echo "validation_status=BLOCKED"
            ;;
    esac
else
    echo "kernel_status=BLOCKED"

    echo
    echo "[VALIDATION]"
    echo "command=pwd"
    echo "validation_status=BLOCKED"
fi

echo
echo "[END]"
