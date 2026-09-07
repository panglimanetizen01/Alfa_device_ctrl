# Gate 14 — Runtime Command Validation Engine V1

## Purpose

G14 is the deterministic validation boundary for the exact runtime command request produced by G13. It proves that the request is structurally valid, current, provenance-bound, semantically the intended `pwd` request, and still deferred to the execution authority owned by G17.

G14 does not execute the command, apply policy, or authorize execution.

## Rules

1. Validation consumes only an explicit Gate 13 artifact and explicit `RUN_ID`.
2. Gate 13 must be `PASS` with `request_status=REQUESTED`.
3. The Gate 13 source commit must equal the current Git `HEAD` exactly.
4. `pipeline_run_id`, Gate 4 hash, profile hash, request identity, and command identity must remain exact.
5. The exact referenced Gate 12 artifact must exist and its SHA-256 must equal `gate12_kernel_sha256` carried by G13.
6. The command must be exactly `pwd` with semantics `POSIX_PWD` and the canonical command SHA-256.
7. Upstream execution must remain `DEFERRED`, with authority `G17` and path `DEFERRED:G17`.
8. A valid request produces `validation_status=ALLOWED`; invalid or stale evidence is `BLOCKED`.
9. G14 must never execute `pwd`, a shell, PRoot, or any runtime command.
10. G14 must never produce command stdout, stderr, return-code, or execution-result evidence.
11. G14 must not perform policy or authorization; those are downstream G15/G16 boundaries.
12. Evidence is explicit, current-run, source-bound, and atomically published.
13. Artifact selection by directory order, timestamps, `ls -1t`, or `head -1` is forbidden.
14. `BLOCKED`, `UNKNOWN`, missing, malformed, stale, cross-run, or hash-mismatched evidence is never treated as PASS.

## Provenance

Every G14 PASS artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, `profile_sha256`, and the SHA-256 of the exact G13 input artifact. The G13 artifact must itself bind the exact Gate 12 kernel hash to the same run and source.

## Implementation boundary

Canonical implementation:

- `tools/gate14_runtime_command_validation.sh`
- `tools/test_gate14_contract.sh`

The producer is the only authoritative G14 implementation. Legacy `runtime_stage.sh` propagation is not a G14 implementation and must not be used to establish G14 status.

## Verification boundary

The contract test must prove:

- positive validation of a current exact G13 request;
- exact evidence schema and `ALLOWED` result;
- no execution evidence or execution path;
- wrong run rejection;
- stale source rejection;
- non-PASS request rejection;
- invalid request-state rejection;
- wrong command and semantics rejection;
- upstream execution leakage rejection;
- wrong authority rejection;
- G1-G13 protection surface remains present.

## State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE LOCAL G14 VERIFICATION REQUIRED
