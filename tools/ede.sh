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

ANDROID_HOST="UNKNOWN"

if uname -r 2>/dev/null | grep -qi android
then
    ANDROID_HOST="DETECTED"
elif [ -d /storage/emulated/0 ]
then
    ANDROID_HOST="DETECTED"
fi

CONTAINER_ENVIRONMENT="UNKNOWN"

if env | grep -qi userland
then
    CONTAINER_ENVIRONMENT="DETECTED"
fi

TOOLCHAIN_PRESENT=0
for tool in git python3 java javac gradle
do
    command -v "$tool" >/dev/null 2>&1 && TOOLCHAIN_PRESENT=$((TOOLCHAIN_PRESENT+1))
done

if [ "$TOOLCHAIN_PRESENT" -eq 5 ]
then
    TOOLCHAIN_STATUS="PASS"
elif [ "$TOOLCHAIN_PRESENT" -ge 2 ]
then
    TOOLCHAIN_STATUS="WARNING"
else
    TOOLCHAIN_STATUS="ERROR"
fi

if [ -d /storage/emulated/0 ]
then
    if touch /storage/emulated/0/.ede_write_test.$$ >/dev/null 2>&1
    then
        rm -f /storage/emulated/0/.ede_write_test.$$ >/dev/null 2>&1
        STORAGE_STATUS="PASS"
    else
        STORAGE_STATUS="WARNING"
    fi
else
    STORAGE_STATUS="ERROR"
fi

echo "android_host=$ANDROID_HOST"
echo "container_environment=$CONTAINER_ENVIRONMENT"
echo "toolchain_status=$TOOLCHAIN_STATUS"
echo "storage_status=$STORAGE_STATUS"
