# Gate 6 — Runtime Bootstrap Engine V1

## Purpose

Gate 6 is the **runtime control-plane bootstrap boundary** after Gate 5 decision and authorization. It initializes a run-scoped bootstrap state and proves that the runtime control path can be created, written, read, and removed without changing Gate 3/4 capability claims.

Gate 6 does **not** claim Linux guest execution, PRoot execution, distro support, or APK runtime usability. Those claims belong to later runtime/acceptance gates.

## Entrance Criteria

Gate 6 may proceed only when all of the following are true for the explicit `pipeline_run_id`:

1. Gate 4 contract is `VALID`.
2. Gate 5 decision artifact exists and is `decision=ALLOW`.
3. Gate 5 authorization artifact exists and is `authorization_status=AUTHORIZED`.
4. Decision and authorization carry the same `pipeline_run_id`, `source_commit`, `profile_sha256`, and `gate4_contract_sha256` as Gate 4.
5. Authorization `decision_id` matches the Gate 5 decision `decision_id`.
6. No artifact is selected by directory ordering, timestamps, `ls -1t`, `head -1`, or "newest" discovery.

## Bootstrap Verification Contract

The implementation must perform all of these operations against a run-scoped bootstrap state:

1. create the bootstrap state directory;
2. create a bootstrap marker containing the exact run identity;
3. read the marker back and compare its content exactly;
4. remove the marker and verify that it is no longer present;
5. publish a run-scoped `bootstrap.ready` state atomically;
6. emit an atomic Gate 6 evidence artifact containing upstream provenance, implementation configuration identity, inputs, execution path, timestamp, probe result, and final status.

A successful bootstrap therefore proves **control-plane bootstrap readiness**, not guest Linux execution.

## Fail-Closed Rules

- `ALLOW` is the only accepted Gate 5 decision value.
- `AUTHORIZED` is the only accepted Gate 5 authorization value.
- Missing, malformed, stale, cross-run, cross-source-commit, cross-profile, or cross-contract evidence is `BLOCKED`.
- `UNKNOWN` is never converted to `PASS`.
- A failed bootstrap probe is `ERROR` and cannot produce `PASS`.
- Gate 6 must not modify capability status established by G3/G4.
- Evidence from another run or source commit is stale evidence.

## Evidence Schema

```text
schema_version=gate6-bootstrap.v1
gate=gate6
gate_status=PASS|BLOCKED|ERROR
pipeline_run_id=<explicit run>
source_commit=<Gate 4 upstream source commit>
implementation_commit=<exact Gate 6 implementation HEAD>
gate4_contract_sha256=<exact Gate 4 contract hash>
profile_sha256=<exact Gate 4 profile hash>
decision_id=<Gate 5 decision id>
request_id=<Gate 5 request id>
authorization_status=AUTHORIZED
bootstrap_status=PASS|BLOCKED|ERROR
bootstrap_state_path=<run-scoped state directory>
bootstrap_probe=PASS|BLOCKED|ERROR
created_at=<UTC timestamp>
execution_path=<canonical project root>
stage_reason=<auditable reason>
```

`source_commit` identifies the upstream configuration consumed by Gate 6. `implementation_commit` identifies the exact Gate 6 implementation configuration being verified. Both are required so that downstream evidence cannot confuse upstream provenance with the code under test.

## Verification Requirements

The Gate 6 verification set must prove at minimum:

- positive current-run bootstrap succeeds;
- missing Gate 5 evidence is blocked;
- cross-run/cross-provenance evidence is blocked;
- malformed or non-ALLOW decision is blocked;
- non-AUTHORIZED authorization is blocked;
- failed bootstrap I/O cannot produce `PASS`;
- the produced artifact is atomic and contains the exact current input identity and implementation configuration identity.

## NASA-Informed Verification Boundary

The gate follows NASA verification principles of explicit entrance/success criteria, configuration identification, traceability, objective evidence, discrepancy/root-cause handling, and controlled progression to the next phase. Verification is evidence of compliance with this Gate 6 contract; it is not validation of the final Linux userspace product.

## State

Specification: DEFINED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 6 VERIFICATION REQUIRED
