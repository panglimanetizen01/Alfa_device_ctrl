# Gate 16 — Runtime Execution Authorization Engine V1

## Purpose

G16 is the authorization boundary between the G15 policy decision and G17 execution. It determines whether the exact command may be handed to the execution layer without executing it.

## Authorization contract

G16 requires two independent upstream conditions:

1. exact current-run G15 evidence with `policy_status=ALLOWED`; and
2. exact current-run Gate 5 evidence with `authorization_status=AUTHORIZED`.

Gate 5 is an immutable upstream authorization source. G16 does not modify or reinterpret Gate 5.

## Locked V1 request

Only the request established by G13/G14/G15 is eligible:

- `request_id=pwd-request-$RUN_ID`
- `command=pwd`
- `command_semantics=POSIX_PWD`
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `execution_path=DEFERRED:G17`

## Rules

1. All input artifacts are explicit paths; no newest-artifact discovery is permitted.
2. G15 must be `gate15-runtime-command-policy.v1`, `gate=gate15`, `gate_status=PASS`, and `policy_status=ALLOWED`.
3. G15 source commit must equal the current Git HEAD.
4. G15 run, Gate 4 hash, profile hash, request identity, command identity, and execution-deferred state must be exact.
5. Every G15-referenced G14/G13/G12 artifact must exist and its actual SHA-256 must equal the recorded hash.
6. Gate 5 authorization must be `gate5-authorization.v1`, `authorization_status=AUTHORIZED`, and carry matching run/source/Gate 4/profile/request identity.
7. Missing, stale, cross-run, malformed, hash-mismatched, DENIED, UNKNOWN, or ambiguous evidence is BLOCKED.
8. G16 produces a deterministic authorization artifact and records the exact upstream hashes.
9. G16 MUST NOT execute `pwd`, invoke `runtime_stage.sh`, invoke `runtime_execution.sh`, spawn a shell for command execution, or emit command output/return-code evidence.
10. G16 MUST NOT select artifacts by directory order, timestamp, `ls -1t`, `head -1`, or historical fallback.
11. Authorization publication is atomic.
12. Execution remains exclusively owned by G17.

## Evidence

Producer: `tools/gate16_runtime_execution_authorization.sh`

Result schema: `docs/GATE_16_RESULT_SCHEMA_V1.md`

Contract test: `tools/test_gate16_contract.sh`

Expected artifact: `artifacts/pipeline/$RUN_ID/gate16/authorization.txt`

## Verification boundary

G16 is GREEN only when the dedicated contract test proves the positive authorization path, exact G15/Gate 5 provenance binding, negative policy/authentication/provenance cases, no-execution boundary, and protection of G1-G15.

## Initial State

Specification: IMPLEMENTED
Implementation: IMPLEMENTED
Verification: IMPLEMENTED — LIVE GATE 16 VERIFICATION REQUIRED

## Gate 6-19 implementation contract

The runtime chain is current-run and path-explicit. Gate 6 consumes the Gate 5 `decision=ALLOW` artifact. Gates 7-12 consume the exact preceding artifact. Gate 13 requests only `pwd`; Gate 14 validates it; Gate 15 applies policy; Gate 16 authorizes only after G15 and Gate 5 agree; Gate 17 executes only after G16; Gate 18 normalizes the result; Gate 19 consumes without re-executing.

Every artifact carries `pipeline_run_id`, `source_commit`, `gate4_contract_sha256`, and `profile_sha256`. Missing, stale-incompatible, malformed, or cross-run evidence is BLOCKED. No stage selects artifacts using directory order, timestamps, `ls -1t`, or `head -1`.
