# Gate 4 — Environment Contract Engine V1

## Purpose

Membentuk kontrak runtime berdasarkan hasil EDE + CDE + Gate 3 + authoritative runtime profile.

## Contract

Wajib memuat:

- environment identity
- storage capabilities
- execution capabilities
- shared-storage data I/O capability
- network capability
- toolchain availability
- exact pipeline run and source-commit provenance

## Rules

1. Contract hanya boleh berisi capability yang sudah terverifikasi.
2. UNKNOWN tetap UNKNOWN dan tidak boleh dipromosikan menjadi PASS.
3. Tidak boleh mengarang capability.
4. Contract berlaku hanya untuk current execution environment.
5. Output harus machine-readable dan human-auditable.
6. Gate 4 tidak melakukan probing ulang; ia mengonsumsi artefak EDE, CDE, Gate 3, dan runtime profile authority yang telah diverifikasi.
7. Semua input dan output harus terikat pada pipeline run dan source commit yang sama; stale atau ambiguous provenance wajib ditolak.
8. `SHARED_STORAGE_IO` adalah data-I/O capability dari G3, bukan bukti bahwa shared storage dapat digunakan sebagai executable-code boundary.
9. `EXEC_SHARED` pada runtime profile hanya compatibility alias dan tidak boleh ditafsirkan G4 sebagai bukti executable shared storage.
10. Status PASS hanya boleh diterbitkan jika kontrak berhasil dibentuk dan seluruh provenance/input validation yang diwajibkan lulus.
11. Failure state harus fail-closed: BLOCKED/INVALID bukan PASS.

## Implementation Boundary

Implementation: `tools/runtime_profile_generator.sh` dan `tools/environment_contract.sh`.

The profile generator consumes the current-run EDE/CDE/G3 artifacts and preserves the G3 storage semantics as `SHARED_STORAGE_IO`. The environment contract validates the same current-run artifacts, their envelopes/hashes, authoritative profile, and exact source/run identity before emitting the contract.

Gate 4 does not claim Android APK runtime execution, PRoot guest execution, distro acceptance, or Linux userspace acceptance. Those concerns belong to later gates.

## Verification Boundary

Live verification must prove that the current pipeline can form a VALID G4 contract from current EDE/CDE/G3 evidence and that stale, missing, malformed, or semantically mismatched provenance cannot produce PASS.

## Initial State

Specification: DEFINED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 4 VERIFICATION REQUIRED
