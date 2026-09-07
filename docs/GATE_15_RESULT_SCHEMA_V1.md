# Gate 15 — Runtime Command Policy Result Schema V1

## Schema

`gate15-runtime-command-policy.v1`

## Required evidence

- `schema_version=gate15-runtime-command-policy.v1`
- `gate=gate15`
- `gate_status=PASS`
- `policy_status=ALLOWED|BLOCKED`
- deterministic `policy_id`
- `policy_version`
- `pipeline_run_id`
- current `source_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `gate14_validation_sha256`
- exact `gate14_artifact`
- `gate13_request_sha256`
- exact `gate13_artifact` referenced by G14
- `gate12_kernel_sha256`
- exact `gate12_artifact` referenced by G14
- `validation_id`
- `request_id`
- exact `command=pwd`
- `command_semantics=POSIX_PWD`
- `command_sha256`
- `policy_reason`
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `execution_path=DEFERRED:G17`
- `created_at`
- `policy_path`

## Boundary

G15 is a policy decision boundary. It consumes only the exact current-run G14 validation artifact and produces policy evidence. It MUST NOT execute the command, invoke `runtime_stage.sh`, invoke `runtime_execution.sh`, spawn a shell, or emit command output/return-code evidence.

The hashes carried by G14 for the referenced Gate 13 request and Gate 12 kernel are revalidated against those exact files before G15 can return ALLOWED. This prevents a syntactically valid but substituted upstream artifact from being treated as authoritative.

`ALLOWED` is permitted only for the exact V1 request established by G13/G14: `pwd` with `POSIX_PWD` semantics and deferred execution authority G17. Any missing, stale, cross-run, malformed, hash-mismatched, or already-executed evidence is rejected fail-closed.

`UNKNOWN`, `WARNING`, missing, malformed, or ambiguous policy state cannot be treated as ALLOWED.
