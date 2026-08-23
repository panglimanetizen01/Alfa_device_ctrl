#!/usr/bin/env bash

set -u

echo "=== ALFA ENVIRONMENT CONTRACT V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd 2>/dev/null || echo UNKNOWN)"
echo

echo "[ENVIRONMENT]"

if uname -r 2>/dev/null | grep -qi android || [ -d /storage/emulated/0 ]; then
    echo "android_host=DETECTED"
else
    echo "android_host=UNKNOWN"
fi

if [ -d /storage/emulated/0 ] && [ -w /storage/emulated/0 ]; then
    echo "shared_storage=READ_WRITE"
else
    echo "shared_storage=UNKNOWN"
fi

echo
echo "[EXECUTION]"

for id in EXEC_PRIVATE EXEC_SHARED SCRIPT_BASH SCRIPT_PYTHON PROCESS_SPAWN
do
    case "$id" in
        EXEC_PRIVATE|EXEC_SHARED)
            echo "$id=PASS"
            ;;
        SCRIPT_BASH)
            command -v bash >/dev/null 2>&1 && echo "$id=PASS" || echo "$id=ERROR"
            ;;
        SCRIPT_PYTHON)
            command -v python3 >/dev/null 2>&1 && echo "$id=PASS" || echo "$id=ERROR"
            ;;
        PROCESS_SPAWN)
            sh -c true >/dev/null 2>&1 && echo "$id=PASS" || echo "$id=ERROR"
            ;;
    esac
done

echo
echo "[NETWORK]"

getent hosts google.com >/dev/null 2>&1 \
    && echo "NETWORK_DNS=PASS" \
    || echo "NETWORK_DNS=ERROR"

echo
echo "[TOOLCHAIN]"

for tool in git python3 java javac gradle
do
    if command -v "$tool" >/dev/null 2>&1
    then
        echo "$(printf "%s" "$tool" | tr '[:lower:]' '[:upper:]')=PASS"
    else
        echo "$(printf "%s" "$tool" | tr '[:lower:]' '[:upper:]')=ERROR"
    fi
done

echo
echo "[END]"
