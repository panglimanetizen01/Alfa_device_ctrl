#!/usr/bin/env bash

TS=$(date +%Y%m%d_%H%M%S)
OUT="artifacts/cde/cde_${TS}.txt"

exec > "$OUT"

echo "=== CDE V1 ==="
echo

echo "[REGISTRY]"
echo "registry=CAPABILITY_REGISTRY_V1"
echo
echo "[CAPABILITIES]"

if [ -r /storage/emulated/0 ]
then
    echo "STORAGE_READ=PASS | verification=Read test | scope=current_environment"
else
    echo "STORAGE_READ=ERROR | verification=Read test | scope=current_environment"
fi

TMP=/storage/emulated/0/.cde_write_test.$$

if touch "$TMP" >/dev/null 2>&1
then
    echo "STORAGE_WRITE=PASS | verification=Write/delete test | scope=current_environment"
    echo "evidence=$TMP"
    rm -f "$TMP"
else
    echo "STORAGE_WRITE=ERROR | verification=Write/delete test | scope=current_environment"
fi

PVT=/tmp/cde_exec_private.$$

cat > "$PVT" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$PVT"

"$PVT" >/dev/null 2>&1
echo "EXEC_PRIVATE_RC=$? | verification=Execution test | scope=current_environment"
rm -f "$PVT"

SHR=/storage/emulated/0/.cde_exec_shared.$$

cat > "$SHR" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$SHR"

"$SHR" >/dev/null 2>&1
echo "EXEC_SHARED_RC=$? | verification=Execution test | scope=current_environment"
rm -f "$SHR"

getent hosts google.com >/dev/null 2>&1
echo "NETWORK_DNS_RC=$? | verification=DNS lookup | scope=current_environment"

echo
echo "[END]"
