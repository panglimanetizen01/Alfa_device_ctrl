# Gate 8 — Runtime Task Engine V1

## Purpose

Menjalankan task hanya jika Runtime Session valid.

## Rules

1. Task hanya boleh berjalan jika session_status=PASS.
2. Task wajib menghasilkan evidence artifact.
3. Task wajib mencatat timestamp dan execution path.
4. Task tidak boleh mengubah capability status.
5. Status task:
   - PASS
   - ERROR
   - BLOCKED

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED
