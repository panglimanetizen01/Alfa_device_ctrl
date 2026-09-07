# Gate 13 — Concrete Runtime Command Request V1

## Purpose

Construct one concrete `pwd` runtime request from the verified Gate 12 kernel artifact. Gate 13 is a request-construction boundary, not an execution boundary.

## Fixed boundary

1. Gate 13 consumes only the explicit Gate 12 artifact supplied by the caller.
2. The Gate 12 artifact must be `gate12-runtime-kernel.v1`, `gate_status=PASS`, and `kernel_status=PASS`.
3. The Gate 12 `pipeline_run_id` must equal the requested run ID.
4. The Gate 12 `source_commit` must equal the current Git HEAD.
5. Gate 4 and profile hashes must be valid 64-hex values and are carried forward unchanged.
6. `kernel_id` must equal `kernel-<RUN_ID>` and `kernel_type` must be `RUNTIME_KERNEL`.
7. Gate 12 must remain `execution_status=DEFERRED` with `execution_authority=G17`.
8. Gate 13 constructs exactly one command: `pwd`.
9. The command semantic is `POSIX_PWD`; Gate 13 does not require a particular guest executable path such as `/bin/pwd`.
10. Gate 13 never executes the command, opens a shell for the command, invokes `runtime_stage.sh`, invokes the execution engine, or fabricates command output/return code.
11. The output is request evidence with `request_status=REQUESTED` and `execution_status=DEFERRED`.
12. Execution authority remains G17. Real guest execution belongs exclusively to Gate 17.
13. Missing, malformed, stale, cross-run, non-PASS, already-executed, or authority-mutated Gate 12 evidence is BLOCKED and returns nonzero.
14. No artifact is selected by timestamp, directory order, `ls -1t`, or `head -1`.

## Output contract

The producer emits `gate13-runtime-command-request.v1` with:

- `gate=gate13`
- `gate_status=PASS`
- `request_status=REQUESTED`
- exact `pipeline_run_id` and current `source_commit`
- exact Gate 4/profile provenance
- SHA-256 of the consumed Gate 12 artifact
- `kernel_id=kernel-<RUN_ID>`
- `request_id=pwd-request-<RUN_ID>`
- `command=pwd`
- `command_semantics=POSIX_PWD`
- deterministic SHA-256 of the command representation
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `execution_path=DEFERRED:G17`
- construction timestamp/path

No command stdout, stderr, return code, or execution result is part of a valid G13 artifact.

## Verification contract

Verification follows NASA-style objective-evidence principles: the procedure identifies prerequisites, input, expected result, evaluation criteria, provenance/configuration, and negative/off-nominal cases. A GREEN result requires the positive request construction and all mandatory negative cases to pass.

Mandatory negative cases:

- wrong pipeline run ID
- stale source commit
- Gate 12 not PASS
- Gate 12 already executed
- invalid kernel identity
- invalid Gate 4 hash
- invalid profile hash
- wrong Gate 12 schema
- wrong execution authority
- evidence containing execution result fields
- execution implementation leakage (`runtime_stage.sh`, shell execution, or command execution primitives)

## Implementation

Primary producer: `tools/gate13_runtime_command_request.sh`

Contract test: `tools/test_gate13_contract.sh`

Result schema: `docs/GATE_13_RESULT_SCHEMA_V1.md`

## State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — CANONICAL CONTRACT TEST REQUIRED
