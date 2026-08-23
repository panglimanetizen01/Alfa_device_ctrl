#!/usr/bin/env bash

set -u

echo "=== ALFA RUNTIME DECISION V1 ==="
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo UNKNOWN)"
echo "execution_path=$(pwd 2>/dev/null || echo UNKNOWN)"
echo

echo "[REQUIRED]"

FAIL=0

for id in STORAGE_READ STORAGE_WRITE EXEC_PRIVATE EXEC_SHARED NETWORK_DNS
do
    if [ "$id" = "STORAGE_READ" ] || [ "$id" = "STORAGE_WRITE" ] ||
       [ "$id" = "EXEC_PRIVATE" ] || [ "$id" = "EXEC_SHARED" ] ||
       [ "$id" = "NETWORK_DNS" ]
    then
        :
    fi
done

[ -r /storage/emulated/0 ] || FAIL=1
[ -w /storage/emulated/0 ] || FAIL=1

P="/tmp/.alfa_gate5.$$"
printf '#!/bin/sh\nexit 0\n' > "$P"
chmod +x "$P"
"$P" >/dev/null 2>&1 || FAIL=1
rm -f "$P"

getent hosts google.com >/dev/null 2>&1 || FAIL=1

echo "storage_read=$([ -r /storage/emulated/0 ] && echo PASS || echo ERROR)"
echo "storage_write=$([ -w /storage/emulated/0 ] && echo PASS || echo ERROR)"
echo "exec_private=$([ "$FAIL" -eq 0 ] && echo PASS || echo ERROR)"
echo "network_dns=$(getent hosts google.com >/dev/null 2>&1 && echo PASS || echo ERROR)"

echo
echo "[DECISION]"

if [ "$FAIL" -eq 0 ]
then
    echo "decision=READY"
else
    echo "decision=BLOCKED"
fi

echo
echo "[END]"
