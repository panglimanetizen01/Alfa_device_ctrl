# Gate 11 — Runtime Orchestrator Engine V1

## Purpose

Gate 11 constructs and admits one runtime orchestrator from one verified Gate 10 workflow. It does not execute the guest command.

## Rules

1. Orchestrator is admitted only when the exact Gate 10 artifact has `gate_status=PASS` and `workflow_status=PASS`.
2. Gate 10 provenance must match current Gate 4 contract, pipeline run, source HEAD, and profile hash.
3. Orchestrator identity is deterministic and run-scoped: `orchestrator_id=orchestrator-<pipeline_run_id>`.
4. Evidence records timestamp and canonical execution path.
5. Gate 11 must not mutate capability status or invent execution results.
6. Gate 11 must not execute the guest command; `execution_status=DEFERRED` and `execution_authority=G17` are mandatory.
7. Missing, malformed, stale, cross-run, cross-contract, or UNKNOWN provenance is BLOCKED.
8. No historical run defaults, newest-artifact discovery, `ls -1t`, or `head -1` selection.
9. Publication is atomic: write a partial artifact, then rename it to the final path.

## Status

Specification: DEFINED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 11 VERIFICATION REQUIRED

## Implementation boundary

- `tools/gate11_runtime_orchestrator.sh` is the authoritative G11 producer/validator.
- `tools/runtime_orchestrator.sh` is only an explicit-run entry point and cannot fall back to `runtime_stage.sh`.
- `tools/test_gate11_contract.sh` provides positive construction and negative provenance/execution tests.
- `.github/workflows/g11-runtime-orchestrator-contract.yml` verifies the contract and protects G1-G10 source boundaries.

## Verification boundary

G11 GREEN requires objective evidence that a valid Gate 10 workflow produces a valid G11 orchestrator artifact and that wrong-run, stale-source, non-PASS workflow, invalid workflow identity, and attempted execution inputs are rejected. A G11 PASS is construction/admission evidence only; actual guest execution remains G17.

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
