#!/usr/bin/env bash
set -u
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
SCRIPT="$ROOT/tools/runtime_bootstrap.sh"
COMMON="$ROOT/tools/runtime_chain_common.sh"
fail(){ printf 'GATE6_CONTRACT_TEST=FAIL\nREASON=%s\n' "$1"; exit 1; }
[ -x "$SCRIPT" ] || fail 'runtime_bootstrap.sh is not executable'
[ -f "$COMMON" ] || fail 'runtime_chain_common.sh missing'
bash -n "$SCRIPT" || fail 'runtime_bootstrap.sh syntax invalid'
bash -n "$COMMON" || fail 'runtime_chain_common.sh syntax invalid'
grep -q 'RUNTIME_ID=${2:-}' "$SCRIPT" || fail 'runtime_id argument missing'
grep -q 'chain_runtime_supported' "$SCRIPT" || fail 'runtime registry binding missing'
grep -q 'runtime_registry_sha256=' "$SCRIPT" || fail 'runtime registry provenance missing'
grep -q 'runtime_id=$RUNTIME_ID' "$SCRIPT" || fail 'runtime identity is not emitted'
printf 'GATE6_CONTRACT_TEST=PASS\nRUNTIME_BINDING=PASS\nREGISTRY_PROVENANCE=PASS\n'
