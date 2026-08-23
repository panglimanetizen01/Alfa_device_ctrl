# Gate 15 — Runtime Command Policy Engine V1

## Purpose

Menerapkan policy command sebelum command diteruskan ke execution layer.

## Rules

1. Policy hanya boleh berjalan jika kernel_status=PASS.
2. Command yang masuk policy wajib menghasilkan policy result.
3. Command yang diizinkan harus menghasilkan ALLOWED.
4. Command yang tidak diizinkan harus menghasilkan BLOCKED.
5. Policy BLOCKED tidak boleh dieksekusi.
6. Policy tidak boleh mengubah capability status.
7. Policy wajib menghasilkan evidence artifact.
8. Policy wajib mencatat timestamp dan execution path.

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED
