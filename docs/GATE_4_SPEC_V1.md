# Gate 4 — Environment Contract Engine V1

## Purpose

Membentuk kontrak runtime berdasarkan hasil EDE + CDE + Gate 3.

## Contract

Wajib memuat:

- environment identity
- storage capabilities
- execution capabilities
- network capability
- toolchain availability

## Rules

1. Contract hanya boleh berisi capability yang sudah terverifikasi.
2. UNKNOWN tetap UNKNOWN.
3. Tidak boleh mengarang capability.
4. Contract berlaku hanya untuk current execution environment.
5. Output harus machine-readable dan human-auditable.
6. Gate 4 tidak melakukan probing ulang; ia mengonsumsi artefak EDE, CDE, Gate 3, dan runtime profile authority yang telah diverifikasi.
7. Semua input dan output harus terikat pada pipeline run dan source commit yang sama; stale atau ambiguous provenance wajib ditolak.
8. Status PASS hanya boleh diterbitkan jika kontrak berhasil dibentuk dan seluruh provenance/input validation yang diwajibkan lulus.
9. Failure state harus fail-closed: BLOCKED/INVALID bukan PASS.

## Implementation Boundary

Implementation: `tools/environment_contract.sh`

The implementation validates the current pipeline state, manifest, authoritative runtime profile, upstream EDE/CDE/Gate 3 artifacts, provenance, and output paths before producing the environment contract. It emits a machine-readable contract and an envelope containing its source/run provenance.

Gate 4 does not claim Android APK runtime execution, PRoot guest execution, distro acceptance, or Linux userspace acceptance. Those concerns belong to later gates.

## Verification Boundary

Verification: the Gate 4 producer and its contract validation/self-test must prove that valid current-run inputs produce a VALID contract and that stale, missing, malformed, or unauthorized provenance cannot produce PASS.

## Initial State

Specification: DEFINED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 4 VERIFICATION REQUIRED
