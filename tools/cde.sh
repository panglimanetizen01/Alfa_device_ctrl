#!/usr/bin/env bash

TS=$(date +%Y%m%d_%H%M%S)
OUT="artifacts/cde/cde_${TS}.txt"

exec > "$OUT"

echo "=== CDE V1 ==="
echo

echo "[CAPABILITIES]"

if [ -r /storage/emulated/0 ]
then
    echo "STORAGE_READ=PASS"
else
    echo "STORAGE_READ=ERROR"
fi

TMP=/storage/emulated/0/.cde_write_test.$$

if touch "$TMP" >/dev/null 2>&1
then
    echo "STORAGE_WRITE=PASS"
    echo "evidence=$TMP"
    rm -f "$TMP"
else
    echo "STORAGE_WRITE=ERROR"
fi

PVT=/tmp/cde_exec_private.$$

cat > "$PVT" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$PVT"

"$PVT" >/dev/null 2>&1
echo "EXEC_PRIVATE_RC=$?"
rm -f "$PVT"

SHR=/storage/emulated/0/.cde_exec_shared.$$

cat > "$SHR" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$SHR"

"$SHR" >/dev/null 2>&1
echo "EXEC_SHARED_RC=$?"
rm -f "$SHR"

getent hosts google.com >/dev/null 2>&1
echo "NETWORK_DNS_RC=$?"

echo
echo "[END]"
