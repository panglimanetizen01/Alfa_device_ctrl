# Gate 6 — Runtime Bootstrap Engine V1

## Purpose

Menjalankan bootstrap Alfa hanya setelah Runtime Decision menyatakan READY.

## Rules

1. Bootstrap hanya boleh berjalan jika decision=READY.
2. Jika decision bukan READY, bootstrap harus BLOCKED.
3. Bootstrap wajib mencatat timestamp dan execution path.
4. Bootstrap harus menghasilkan evidence artifact.
5. Bootstrap tidak boleh mengubah capability status.

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
