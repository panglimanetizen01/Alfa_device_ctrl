# Gate 16 — Runtime Execution Authorization Result Schema V1

## Schema

`gate16-runtime-execution-authorization.v1`

## Boundary

G16 is the authorization boundary between the G15 policy decision and G17 execution. It does not execute the command.

G16 requires both:

1. exact current-run G15 `policy_status=ALLOWED`; and
2. exact current-run Gate 5 `authorization_status=AUTHORIZED`.

Gate 5 remains an immutable upstream authorization source. G16 does not modify Gate 5 and does not reinterpret a DENIED or UNKNOWN result as authorization.

## Required PASS evidence

- `schema_version=gate16-runtime-execution-authorization.v1`
- `gate=gate16`
- `gate_status=PASS`
- `authorization_status=AUTHORIZED`
- deterministic `authorization_id`
- `pipeline_run_id`
- current `source_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `gate15_policy_sha256`
- exact `gate15_artifact`
- `gate5_authorization_sha256`
- exact `gate5_authorization_artifact`
- Gate 5 authorization and decision identities
- `request_id=pwd-request-<RUN_ID>`
- exact `command=pwd`
- `command_semantics=POSIX_PWD`
- exact command SHA256
- `policy_status=ALLOWED`
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `execution_path=DEFERRED:G17`
- `created_at`
- `authorization_path`

## Fail-closed requirements

G16 MUST reject missing, malformed, stale, cross-run, hash-mismatched, non-ALLOWED policy, non-AUTHORIZED Gate 5 evidence, wrong request identity, wrong command, wrong semantics, or any execution state other than `DEFERRED`.

G16 MUST verify the actual SHA-256 of every explicitly referenced G15/G14/G13/G12 artifact before authorization is emitted.

G16 MUST NOT select artifacts by directory order, timestamps, `ls -1t`, `head -1`, or historical fallback.

## Prohibited behavior

G16 must not:

- execute `pwd`;
- invoke `runtime_stage.sh` or `runtime_execution.sh`;
- spawn a shell for command execution;
- emit command stdout, stderr, return code, or execution result;
- convert authorization into execution.

Execution authority remains G17.

## GREEN criteria

G16 is GREEN only when the dedicated contract test proves the positive authorization path, exact G15/Gate 5 provenance binding, all required negative cases, no-execution boundary, and protection of G1-G15 against the current canonical source.
