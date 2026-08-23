# Gate 13 — Runtime Command Engine V1

## Purpose

Menjalankan command runtime nyata setelah kernel aktif.

## Rules

1. Command hanya boleh berjalan jika kernel_status=PASS.
2. Command wajib menghasilkan evidence artifact.
3. Command wajib mencatat timestamp dan execution path.
4. Command wajib mencatat command yang dieksekusi.
5. Status command:
   - PASS
   - ERROR
   - BLOCKED
