# Gate 7 — Runtime Session Engine V1

## Purpose

Gate 7 proves that the Android runtime session boundary was actually established for one exact Gate 6 run. G7 is an evidence-consumption gate: it never creates a session and never converts Gate 6 bootstrap PASS into session PASS.

## Locked boundary

G7 proves only:

1. the exact Gate 6 bootstrap for the requested `pipeline_run_id` is `PASS`;
2. an Android-produced `operation-evidence.v1` attestation exists for that same run;
3. the attestation identifies the same source commit, Gate 4 contract hash, profile hash, and implementation commit as Gate 6;
4. the selected runtime is `ubuntu` and the runtime identity paths are present;
5. a real PTY session reached `READY` with a positive shell PID and observed prompt;
6. the evidence is current-run, explicit-path, and fail-closed.

G7 does **not** prove guest command acceptance, `pwd`, distro acceptance, network capability, or final Linux userspace usability. Those remain downstream responsibilities.

## Provenance contract

Every accepted G7 result is bound to:

- `pipeline_run_id`
- `source_commit`
- `implementation_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `session_id`
- `request_id`
- `runtime_id`

Missing, stale, malformed, cross-run, or mismatched evidence is `BLOCKED`.

No stage may select artifacts using directory order, timestamps, `ls -1t`, `head -1`, or another implicit newest-artifact rule.

## Android session evidence

The Android session engine emits `operation-evidence.v1`. A valid G7 READY attestation contains at minimum:

- `state=READY`
- `result=PROMPT_OBSERVED`
- `process_pid` greater than zero
- `pty_status=PASS`
- `prompt_observed=PASS`
- exact Gate 6 provenance fields
- runtime engine/rootfs/READY evidence paths

Prompt observation is evidence that the PTY/shell reached its interactive boundary; it is **not** guest command acceptance and is not reused as G13 `pwd` evidence.

## Implementation boundary

- `tools/gate7_runtime_session.sh` — fail-closed verifier/producer.
- `tools/runtime_session.sh` — explicit-path G7 entrypoint only.
- `tools/test_gate7_contract.sh` — positive and negative contract tests.
- `app/src/main/java/com/alfa/device_ctrl/InteractiveSessionContract.java` — Android-side provenance contract.
- `app/src/main/java/com/alfa/device_ctrl/OperationEvidence.java` — atomic Android session attestation.
- `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java` — PTY/session lifecycle and readiness evidence.
- `terminal-emulator/src/main/jni/termux.c` — native PTY/fork/exec boundary.

## Failure policy

`UNKNOWN`, missing evidence, prompt-only evidence without session identity, invalid PID, stale provenance, or Gate 6-only evidence cannot produce G7 GREEN.

## Verification state

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LOCAL CONTRACT VERIFICATION REQUIRED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.
