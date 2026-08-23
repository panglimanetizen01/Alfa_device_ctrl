#!/usr/bin/env bash

echo "=== ALFA RUNTIME DISCOVERY V1 ==="
echo
echo "[EDE]"
./tools/ede.sh

echo
echo "[CDE]"
./tools/cde.sh
LATEST_CDE=$(ls -1t artifacts/cde/cde_*.txt | head -1)
cat "$LATEST_CDE"

echo
echo "=== END ==="
