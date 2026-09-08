# Gate 17 — Runtime Execution Engine V1

## Purpose

G17 is the first execution boundary in the G6-G19 chain. It executes the exact command after G16 authorization and records objective execution evidence.

## Locked command

Only `pwd` with `POSIX_PWD` semantics may execute in V1.

## Preconditions

1. Exact current-run G16 artifact is required.
2. G16 schema/status must be PASS and authorization_status=AUTHORIZED.
3. G16 source_commit must equal current Git HEAD.
4. Referenced G15 policy and Gate 5 authorization artifacts must exist and match their recorded SHA-256 values.
5. request_id, command, semantics, command hash, execution status, authority, and execution path must be exact.
6. Missing, stale, cross-run, malformed, hash-mismatched, DENIED, UNKNOWN, or ambiguous evidence is BLOCKED.

## Execution

G17 executes exactly `pwd` in the canonical project runtime working directory. The command is not accepted from arbitrary caller input. No shell command other than the locked `pwd` operation is permitted.

The execution result must record the actual output and return code. A successful `pwd` execution requires a non-empty result and return code 0.

## Evidence

Producer: `tools/gate17_runtime_execution.sh`

Result schema: `docs/GATE_17_RESULT_SCHEMA_V1.md`

Contract test: `tools/test_gate17_contract.sh`

Live current-run verification: `tools/test_gate17_live.sh`

Expected artifact: `artifacts/pipeline/$RUN_ID/gate17/execution.txt`

## Boundary

G17 MUST NOT modify G1-G16. G17 does not perform policy authorization; it consumes G16 authorization. G17 must not execute `runtime_stage.sh` or the legacy `runtime_execution.sh`.

No timestamp/newest-artifact discovery is permitted. Publication is atomic.

## Verification

G17 is GREEN only when both layers are proven:

1. The dedicated contract test proves the execution boundary, exact provenance checks, objective execution evidence, negative fail-closed cases, and G1-G16 protection.
2. A live current-run verification creates one fresh explicit Gate 4 run and drives the real G5-G17 chain to the authoritative G17 executor without synthetic upstream fixtures or newest-artifact selection.

For Android/Termux acceptance, the same live current-run verification must additionally be executed from the canonical Termux project environment and its terminal output independently checked against the resulting G17 artifact. CI evidence is supplementary and does not replace target-device verification.

## Initial State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 17 VERIFICATION REQUIRED

## Gate 6-19 implementation contract

Gate 6 consumes Gate 5 `decision=ALLOW`; Gates 7-12 consume exact preceding artifacts; Gate 13 requests `pwd`; Gate 14 validates it; Gate 15 applies policy; Gate 16 authorizes it; Gate 17 executes it; Gate 18 normalizes the result; Gate 19 consumes without re-executing.
