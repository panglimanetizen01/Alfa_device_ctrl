# Gate 15 — Runtime Command Policy Engine V1

## Purpose

G15 is the policy decision boundary between command validation (G14) and execution authorization/execution (G16/G17). It evaluates the exact current-run G14 validation evidence and produces an explicit policy result.

## Locked policy

V1 permits exactly the command established by G13 and validated by G14:

- `command=pwd`
- `command_semantics=POSIX_PWD`
- `request_id=pwd-request-$RUN_ID`
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `execution_path=DEFERRED:G17`

A valid V1 request produces `policy_status=ALLOWED`. Anything missing, stale, cross-run, malformed, hash-mismatched, or already executed is rejected fail-closed and cannot produce ALLOWED.

## Rules

1. G15 consumes only the exact explicit G14 artifact supplied by the caller.
2. G15 requires `gate14-runtime-command-validation.v1`, `gate=gate14`, `gate_status=PASS`, and `validation_status=ALLOWED`.
3. The G14 `source_commit` must equal the current Git HEAD.
4. `pipeline_run_id`, command identity, Gate 4/profile hashes, Gate 13 request hash, and Gate 12 kernel hash must be present and structurally valid.
5. The exact `pwd` command and `POSIX_PWD` semantics are an allowlist, not a denylist.
6. G15 MUST reject any execution state other than `DEFERRED` and any authority/path other than `G17` / `DEFERRED:G17`.
7. G15 produces a deterministic policy identity and machine-readable evidence artifact.
8. G15 MUST NOT execute the command, invoke a shell, invoke `runtime_stage.sh`, invoke `runtime_execution.sh`, or produce command stdout/return-code evidence.
9. G15 does not modify capability state and does not create authorization. Authorization remains a G16/Gate 5 concern.
10. Missing, UNKNOWN, WARNING, malformed, stale, ambiguous, or cross-run evidence is BLOCKED; none may be interpreted as ALLOWED.
11. No artifact may be selected by directory order, timestamps, `ls -1t`, or `head -1`.
12. Output publication is atomic and the output evidence is bound to the exact G14 input hash.

## Evidence

Producer: `tools/gate15_runtime_command_policy.sh`

Result schema: `docs/GATE_15_RESULT_SCHEMA_V1.md`

Contract test: `tools/test_gate15_contract.sh`

Expected artifact: `artifacts/pipeline/$RUN_ID/gate15/policy.txt`

## Verification boundary

G15 is GREEN only when the dedicated contract test proves the positive policy path, execution exclusion, provenance binding, stale/cross-run rejection, invalid command rejection, invalid semantics rejection, already-executed rejection, invalid authority rejection, hash rejection, and protection of G1-G14.

## Initial State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 15 VERIFICATION REQUIRED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies the policy; Gate 16 requires the Gate 5 authorization artifact; Gate 17 executes only after authorization; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
