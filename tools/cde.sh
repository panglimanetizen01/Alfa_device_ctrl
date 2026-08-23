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
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "EXEC_PRIVATE=PASS | verification=Execution test | scope=current_environment"
else
    echo "EXEC_PRIVATE=ERROR | verification=Execution test | scope=current_environment"
fi
rm -f "$PVT"

SHR=/storage/emulated/0/.cde_exec_shared.$$

cat > "$SHR" <<'SH'
#!/bin/sh
exit 0
SH

chmod +x "$SHR"

"$SHR" >/dev/null 2>&1
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "EXEC_SHARED=PASS | verification=Execution test | scope=current_environment"
else
    echo "EXEC_SHARED=ERROR | verification=Execution test | scope=current_environment"
fi
rm -f "$SHR"

getent hosts google.com >/dev/null 2>&1
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "NETWORK_DNS=PASS | verification=DNS lookup | scope=current_environment"
else
    echo "NETWORK_DNS=ERROR | verification=DNS lookup | scope=current_environment"
fi

echo
echo "[TOOLCHAIN]"

for tool in git python3 java javac gradle
do
    if command -v "$tool" >/dev/null 2>&1
    then
        echo "$tool=PASS | verification=command discovery | scope=current_environment"
    else
        echo "$tool=ERROR | verification=command discovery | scope=current_environment"
    fi
done

echo
echo "[END]"
