#!/usr/bin/env bash

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
    rm -f "$TMP" >/dev/null 2>&1
    echo "STORAGE_WRITE=PASS"
else
    echo "STORAGE_WRITE=ERROR"
fi

PVT=/tmp/cde_exec_private.$$

cat > "$PVT" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$PVT"

if "$PVT" >/dev/null 2>&1
then
    echo "EXEC_PRIVATE=PASS"
else
    echo "EXEC_PRIVATE=ERROR"
fi

rm -f "$PVT"

SHR=/storage/emulated/0/.cde_exec_shared.$$

cat > "$SHR" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$SHR"

if "$SHR" >/dev/null 2>&1
then
    echo "EXEC_SHARED=PASS"
else
    echo "EXEC_SHARED=ERROR"
fi

rm -f "$SHR"

if getent hosts google.com >/dev/null 2>&1
then
    echo "NETWORK_DNS=PASS"
else
    echo "NETWORK_DNS=ERROR"
fi
