# Gate 19 — Runtime Result Consumer Engine V1

## Purpose

Mengkonsumsi runtime result sebagai input layer berikutnya tanpa mengulang execution.

## Rules

1. Consumer hanya boleh berjalan jika result_status tersedia.
2. result_status=PASS harus menghasilkan consume_status=ACCEPTED.
3. result_status=ERROR harus menghasilkan consume_status=REJECTED.
4. result_status=BLOCKED harus menghasilkan consume_status=REJECTED.
5. Consumer wajib mempertahankan command dan command_result.
6. Consumer tidak boleh mengeksekusi ulang command.
7. Consumer wajib mencatat timestamp dan execution path.
8. Consumer wajib menghasilkan evidence artifact.
9. Consumer tidak boleh mengubah capability status.

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
