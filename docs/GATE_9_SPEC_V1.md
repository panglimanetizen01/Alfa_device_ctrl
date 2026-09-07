# Gate 9 — Runtime Action Engine V1

## Purpose

Gate 9 constructs and admits one runtime action from one verified Gate 8 task. It does not execute the guest command.

## Rules

1. Action is admitted only when the exact Gate 8 artifact has `gate_status=PASS` and `task_status=PASS`.
2. Gate 8 provenance must match the current Gate 4 contract, pipeline run, source HEAD, and profile hash.
3. Action identity is deterministic and run-scoped: `action_id=action-<pipeline_run_id>`.
4. Action evidence records timestamp and canonical execution path.
5. Gate 9 must not mutate capability status or invent capability results.
6. Gate 9 must not execute the guest command; `execution_status=DEFERRED` and `execution_authority=G17` are mandatory.
7. Missing, malformed, stale, cross-run, cross-contract, or UNKNOWN provenance is BLOCKED.
8. No historical run defaults, newest-artifact discovery, `ls -1t`, or `head -1` selection.
9. Publication is atomic: write a partial artifact, then rename it to the final path.

## Status

Specification: DEFINED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 9 VERIFICATION REQUIRED

## Implementation boundary

- `tools/gate9_runtime_action.sh` is the authoritative G9 producer/validator.
- `tools/runtime_action.sh` is only a thin explicit-run entry point and cannot fall back to `runtime_stage.sh`.
- `tools/test_gate9_contract.sh` provides positive artifact construction and negative provenance/execution tests.
- `.github/workflows/g9-runtime-action-contract.yml` verifies the contract and protects G1-G8 source boundaries.

## Verification boundary

G9 GREEN requires objective evidence that a valid Gate 8 task produces a valid G9 action artifact and that wrong-run, stale-source, non-PASS task, and attempted execution inputs are rejected. A G9 PASS is construction/admission evidence only; actual guest execution remains G17.

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED.
