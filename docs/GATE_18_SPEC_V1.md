# Gate 18 — Runtime Execution Result Engine V1

## Purpose

Menormalisasi hasil execution menjadi runtime result yang dapat dikonsumsi layer berikutnya **tanpa mengurangi jaminan provenance dari Gate 17**.

## Rules

1. Result hanya boleh dibuat dari execution evidence current environment.
2. Input Gate 17 wajib menggunakan schema `gate17-runtime-execution.v1` dan `gate=gate17`.
3. Gate 17 evidence wajib mempertahankan current-run identity: `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, dan `profile_sha256`.
4. Gate 17 execution identity wajib cocok dengan `execution-pwd-<RUN_ID>` dan request `pwd-request-<RUN_ID>`.
5. Gate 17 command wajib `pwd` dengan `POSIX_PWD` semantics dan command SHA-256 yang tepat.
6. `authorization_status` wajib `AUTHORIZED`.
7. `execution_status=PASS` dan `result_status=PASS` wajib konsisten dengan `command_returncode=0`, `command_result` yang tidak kosong, dan `command_result_sha256` yang cocok.
8. Gate 16 authorization evidence yang direferensikan Gate 17 wajib tersedia dan hash-nya cocok.
9. Evidence yang missing, stale, malformed, hash-mismatched, cross-run, atau contradictory harus menghasilkan `gate_status=BLOCKED`.
10. `execution_status=PASS` harus menghasilkan `result_status=PASS`.
11. `execution_status=ERROR` harus menghasilkan `result_status=ERROR` hanya jika evidence execution valid dan konsisten.
12. `execution_status=BLOCKED` harus menghasilkan `result_status=BLOCKED` hanya jika evidence execution valid dan konsisten.
13. Result wajib mempertahankan execution/request identity, command, command semantics, command hash, authorization status, execution status, command result, return code, result hash, Gate 16 authorization linkage, timestamp, dan execution path aktual dari execution evidence.
14. Result wajib menghasilkan evidence artifact current-run.
15. Gate 18 tidak boleh mengeksekusi ulang command.
16. Gate 18 tidak boleh mengubah capability status atau otoritas eksekusi.

## Gate 17 input contract

Gate 18 mengonsumsi artifact langsung dari Gate 17. Untuk evidence PASS, field berikut wajib tersedia dan konsisten:

- `schema_version=gate17-runtime-execution.v1`
- `gate=gate17`
- `gate_status=PASS`
- `execution_id=execution-pwd-<RUN_ID>`
- `pipeline_run_id`
- `source_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `gate16_authorization_sha256`
- `gate16_authorization_artifact`
- `request_id=pwd-request-<RUN_ID>`
- `command=pwd`
- `command_semantics=POSIX_PWD`
- `command_sha256`
- `authorization_status=AUTHORIZED`
- `execution_status=PASS`
- `result_status=PASS`
- `command_result`
- `command_returncode=0`
- `command_result_sha256`
- `created_at`
- `execution_path`

Gate 18 must fail closed when any required field or referenced evidence is invalid.

## Traceability basis

G18 is a normalization boundary, not a new authorization or execution boundary. It inherits the authoritative execution evidence produced by G17 and may only transform that evidence into a consumable result. G17 remains the first and only gate permitted to execute the exact authorized `pwd` command.

## Initial State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: CONTRACT_TESTED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
