#!/usr/bin/env bash

set -u

echo "=== EDE V1 ==="
echo

echo "[HEADER]"
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "ede_version=1"
echo "execution_path=$(pwd 2>/dev/null || echo UNKNOWN)"
echo

echo "[IDENTITY]"
echo "username=$(whoami 2>/dev/null || echo UNKNOWN)"
echo "uid=$(id -u 2>/dev/null || echo UNKNOWN)"
echo "gid=$(id -g 2>/dev/null || echo UNKNOWN)"
echo "groups=$(id -Gn 2>/dev/null || echo UNKNOWN)"
echo

echo "[KERNEL]"
echo "kernel_name=$(uname -s 2>/dev/null || echo UNKNOWN)"
echo "kernel_version=$(uname -r 2>/dev/null || echo UNKNOWN)"
echo "architecture=$(uname -m 2>/dev/null || echo UNKNOWN)"
echo

echo "[ENVIRONMENT]"
echo "home=${HOME:-UNKNOWN}"
echo "pwd=$(pwd 2>/dev/null || echo UNKNOWN)"
echo "shell=${SHELL:-UNKNOWN}"
echo

echo "[TOOLCHAIN]"
for tool in git python3 java javac gradle
do
    if command -v "$tool" >/dev/null 2>&1
    then
        echo "$tool=PRESENT"
    else
        echo "$tool=ABSENT"
    fi
done
echo

echo "[STORAGE]"

if [ -d /storage/emulated/0 ]
then
    echo "android_shared_storage=PRESENT"
else
    echo "android_shared_storage=ABSENT"
fi

TMPFILE=/storage/emulated/0/.ede_write_test.$$ 2>/dev/null

if touch /storage/emulated/0/.ede_write_test.$$ >/dev/null 2>&1
then
    rm -f /storage/emulated/0/.ede_write_test.$$ >/dev/null 2>&1
    echo "writable_shared_storage=YES"
else
    echo "writable_shared_storage=NO"
fi

echo "private_storage=${HOME:-UNKNOWN}"
echo

echo "[CLASSIFICATION]"
echo "android_host=DETECTED"
echo "container_environment=UNKNOWN"
echo "toolchain_status=DISCOVERED"
echo "storage_status=DISCOVERED"
