#!/usr/bin/env bash

set -u

if [ "${ALFA_BUILD_CONTEXT:-}" = 'ci' ] || [ "${GITHUB_ACTIONS:-}" = 'true' ]; then
    STORAGE_ROOT="${ALFA_CI_SHARED_ROOT:-${RUNNER_TEMP:-${TMPDIR:-/tmp}}/alfa-ci-shared}"
    PRIVATE_ROOT="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/alfa-ci-private"
    STORAGE_SCOPE='ci_builder_filesystem'
    mkdir -p "$STORAGE_ROOT" "$PRIVATE_ROOT" || exit 1
else
    STORAGE_ROOT=/storage/emulated/0
    PRIVATE_ROOT="${TMPDIR:-${PREFIX:-$HOME}/tmp}"
    STORAGE_SCOPE='android_shared_storage'
    mkdir -p "$PRIVATE_ROOT" 2>/dev/null || true
fi

TS=$(date +%Y%m%d_%H%M%S)
OUT="${ALFA_CDE_OUT:-artifacts/cde/cde_${TS}.txt}"
mkdir -p "$(dirname "$OUT")" || exit 1

exec > "$OUT"

echo "=== CDE V1 ==="
echo
echo "context=${ALFA_BUILD_CONTEXT:-${GITHUB_ACTIONS:+github_actions}}"
echo "storage_root=$STORAGE_ROOT"
echo "private_root=$PRIVATE_ROOT"
echo "storage_scope=$STORAGE_SCOPE"
echo
echo "[REGISTRY]"
echo "registry=CAPABILITY_REGISTRY_V1"
echo
echo "[CAPABILITIES]"

if [ -r "$STORAGE_ROOT" ]; then
    echo "STORAGE_READ=PASS | verification=Read test | scope=$STORAGE_SCOPE"
else
    echo "STORAGE_READ=ERROR | verification=Read test | scope=$STORAGE_SCOPE"
fi

TMP="$STORAGE_ROOT/.cde_write_test.$$"
if touch "$TMP" >/dev/null 2>&1; then
    echo "STORAGE_WRITE=PASS | verification=Write/delete test | scope=$STORAGE_SCOPE"
    echo "evidence=$TMP"
    rm -f "$TMP"
else
    echo "STORAGE_WRITE=ERROR | verification=Write/delete test | scope=$STORAGE_SCOPE"
fi

PVT="$PRIVATE_ROOT/cde_exec_private.$$"
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

SHR="$STORAGE_ROOT/.cde_exec_shared.$$"
cat > "$SHR" <<'SH'
#!/bin/sh
exit 0
SH
chmod +x "$SHR"
"$SHR" >/dev/null 2>&1
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "EXEC_SHARED=PASS | verification=Execution test | scope=$STORAGE_SCOPE"
else
    echo "EXEC_SHARED=ERROR | verification=Execution test | scope=$STORAGE_SCOPE"
fi
rm -f "$SHR"

python3 -c 'import socket; socket.getaddrinfo("google.com", 443, type=socket.SOCK_STREAM)' >/dev/null 2>&1
RC=$?
if [ "$RC" -eq 0 ]; then
    echo "NETWORK_DNS=PASS | verification=DNS lookup via socket.getaddrinfo | scope=current_environment"
else
    echo "NETWORK_DNS=ERROR | verification=DNS lookup via socket.getaddrinfo | scope=current_environment"
fi

echo
echo "[TOOLCHAIN]"
for tool in git python3 java javac gradle; do
    if command -v "$tool" >/dev/null 2>&1; then
        echo "$(printf "%s" "$tool" | tr "[:lower:]" "[:upper:]")=PASS | verification=command discovery | scope=current_environment"
    else
        echo "$(printf "%s" "$tool" | tr "[:lower:]" "[:upper:]")=ERROR | verification=command discovery | scope=current_environment"
    fi
done

echo
echo "[END]"
