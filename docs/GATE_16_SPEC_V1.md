# Gate 16 — Runtime Execution Authorization Engine V1

## Purpose

Menentukan apakah command yang telah melewati policy boleh diteruskan ke execution layer.

## Rules

1. Authorization hanya boleh berjalan jika kernel_status=PASS.
2. Authorization harus membaca policy result current environment.
3. Policy ALLOWED harus menghasilkan authorization_status=AUTHORIZED.
4. Policy BLOCKED harus menghasilkan authorization_status=DENIED.
5. Authorization tidak boleh mengeksekusi command.
6. Authorization wajib menghasilkan evidence artifact.
7. Authorization wajib mencatat timestamp dan execution path.

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
