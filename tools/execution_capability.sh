#!/usr/bin/env bash

echo "=== ALFA EXECUTION CAPABILITY V1 ==="
echo

echo "[CAPABILITIES]"

P="/tmp/alfa_exec_private.$$"
printf '#!/bin/sh\nexit 0\n' > "$P"
chmod +x "$P"
"$P" >/dev/null 2>&1
[ "$?" -eq 0 ] && echo "EXEC_PRIVATE=PASS | verification=execution test" || echo "EXEC_PRIVATE=ERROR | verification=execution test"
rm -f "$P"

S="/storage/emulated/0/.alfa_exec_shared.$$"
printf '#!/bin/sh\nexit 0\n' > "$S"
chmod +x "$S"
"$S" >/dev/null 2>&1
[ "$?" -eq 0 ] && echo "EXEC_SHARED=PASS | verification=execution test" || echo "EXEC_SHARED=ERROR | verification=execution test"
rm -f "$S"

printf 'echo SCRIPT_BASH_OK\n' | bash >/dev/null 2>&1
[ "$?" -eq 0 ] && echo "SCRIPT_BASH=PASS | verification=bash execution" || echo "SCRIPT_BASH=ERROR | verification=bash execution"

printf 'print("SCRIPT_PYTHON_OK")\n' | python3 >/dev/null 2>&1
[ "$?" -eq 0 ] && echo "SCRIPT_PYTHON=PASS | verification=python execution" || echo "SCRIPT_PYTHON=ERROR | verification=python execution"

sh -c 'true' >/dev/null 2>&1
[ "$?" -eq 0 ] && echo "PROCESS_SPAWN=PASS | verification=process spawn" || echo "PROCESS_SPAWN=ERROR | verification=process spawn"

echo
echo "[END]"
