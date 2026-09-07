# Gate 12 — Runtime Kernel Engine V1

## Purpose

Construct a current-run, provenance-bound runtime-kernel artifact from a verified Gate 11 orchestrator artifact.

## Entrance criteria

1. Explicit `pipeline_run_id`, Gate 11 input artifact, and output path are required.
2. Gate 11 schema is `gate11-runtime-orchestrator.v1`.
3. Gate 11 `gate_status=PASS` and `orchestrator_status=PASS`.
4. Gate 11 `pipeline_run_id` equals the requested run.
5. Gate 11 `source_commit` equals current Git HEAD.
6. Gate 4/profile hashes are syntactically valid and carried forward unchanged.
7. `orchestrator_id=orchestrator-<RUN_ID>`.
8. Gate 11 `execution_status=DEFERRED`.

## Output contract

The producer emits `gate12-runtime-kernel.v1` with `gate_status=PASS`, `kernel_status=PASS`, exact run/source/provenance, `kernel_id=kernel-<RUN_ID>`, and `execution_status=DEFERRED` with `execution_authority=G17`.

G12 constructs the kernel boundary only. It does not execute a guest command, claim Linux distro acceptance, or promote deferred execution into PASS.

## Fail-closed rules

Missing, malformed, stale, cross-run, invalid identity, or non-PASS Gate 11 evidence must return nonzero and `G12_STATUS=BLOCKED`. G12 must not select artifacts by timestamps, directory order, `ls -1t`, or `head -1`. Historical default run IDs are forbidden.

## Verification

Dedicated contract tests must cover positive construction, wrong-run provenance, stale source, non-PASS upstream, invalid orchestrator identity, execution already performed, schema validation, and protection of G1-G11.

## State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LOCAL VERIFICATION REQUIRED
