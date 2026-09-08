# Gate 19 — Runtime Result Consumer Engine V1

## Purpose

Mengkonsumsi runtime result sebagai terminal acceptance boundary tanpa mengulang execution. G19 adalah batas konsumsi evidence: ia tidak memberi otoritas eksekusi baru, tetapi hanya membuka phase berikutnya ketika current-run evidence lengkap, konsisten, dan PASS.

## Rules

1. Consumer hanya boleh berjalan untuk `gate18-artifact.v1` dari `RUN_ID` eksplisit.
2. `gate=gate18` dan `gate_status=PASS` wajib.
3. `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, dan `profile_sha256` wajib cocok dengan current Gate 4 contract.
4. G18 wajib mempertahankan execution identity `execution-pwd-<RUN_ID>` dan request identity `pwd-request-<RUN_ID>`.
5. Command wajib `pwd` dengan `POSIX_PWD` semantics dan SHA-256 yang tepat.
6. `authorization_status=AUTHORIZED`, `execution_status=PASS`, `result_status=PASS`, dan `command_returncode=0` wajib konsisten.
7. `command_result` dan `execution_path` wajib tersedia; `command_result_sha256` wajib cocok dengan hasil aktual.
8. Referensi Gate 16 authorization artifact wajib ada dan hash-nya cocok.
9. Missing, stale, malformed, cross-run, hash-mismatched, atau contradictory evidence menghasilkan `gate_status=BLOCKED` dan `consume_status=REJECTED`.
10. Consumer wajib mempertahankan command, command_result, execution path, execution/request identity, authorization linkage, dan provenance.
11. Consumer tidak boleh mengeksekusi ulang command.
12. Consumer wajib menghasilkan `gate19-artifact.v1` sebagai evidence artifact yang self-contained.
13. `consume_status=ACCEPTED` hanya boleh dihasilkan jika seluruh contract PASS.
14. Jika PASS, consumer wajib menghasilkan `next_phase=APK_BUILD_AND_UI`, `next_phase_status=UNLOCKED`, dan `apk_build_status=OPEN`.
15. G19 tidak mengubah capability status dan tidak memberi execution authority baru.
16. G19 adalah terminal acceptance boundary untuk runtime chain; distribution acceptance dan APK runtime installation tetap menjadi phase berikutnya.

## Input contract

Required G18 fields:

- `schema_version=gate18-artifact.v1`
- `gate=gate18`
- `gate_status=PASS`
- `pipeline_run_id`
- `source_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `input_artifact`
- `created_at`
- `execution_id`
- `request_id`
- `command`
- `command_semantics`
- `command_sha256`
- `authorization_status`
- `execution_status`
- `result_status`
- `command_result`
- `command_returncode`
- `command_result_sha256`
- `gate16_authorization_sha256`
- `gate16_authorization_artifact`
- `execution_path`

## Output contract

G19 emits `gate19-artifact.v1` containing the complete current-run provenance and:

- `consume_status=ACCEPTED` only on PASS
- `next_phase=APK_BUILD_AND_UI`
- `next_phase_status=UNLOCKED` only on PASS
- `apk_build_status=OPEN` only on PASS

This is a build-phase authorization marker, not an APK installation or runtime-success claim.

## Architecture basis

NASA/JPL F´ separates command execution status from event/evidence recording and retains execution context so downstream consumers can attribute the result to the original command. G19 applies the same boundary discipline: consume recorded execution evidence; do not silently re-execute. Android's application model likewise requires the product APK to remain within the Android application/security boundary; unlocking the build phase does not bypass Android packaging or signing requirements.

## Initial State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: CONTRACT_TESTED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing and opens the next APK/UI build phase only after terminal acceptance.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
