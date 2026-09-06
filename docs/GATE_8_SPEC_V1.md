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

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
