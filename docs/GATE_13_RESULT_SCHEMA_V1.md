# Gate 13 Result Schema V1

## Schema ID

`gate13-runtime-command-request.v1`

## Required fields

```text
schema_version=gate13-runtime-command-request.v1
gate=gate13
gate_status=PASS|BLOCKED
request_status=REQUESTED
pipeline_run_id=<exact run id>
source_commit=<40-hex current HEAD>
gate4_contract_sha256=<64-hex>
profile_sha256=<64-hex>
gate12_kernel_sha256=<64-hex>
gate12_artifact=<explicit path>
kernel_id=kernel-<RUN_ID>
request_id=pwd-request-<RUN_ID>
command=pwd
command_semantics=POSIX_PWD
command_sha256=<64-hex>
execution_status=DEFERRED
execution_authority=G17
execution_path=DEFERRED:G17
created_at=<UTC timestamp>
construction_path=<producer project root>
stage_reason=<reason>
```

## Prohibited G13 evidence

A valid PASS artifact must not contain command execution results such as:

- `command_result`
- `command_returncode`
- `execution_result`
- guest stdout/stderr
- a shell invocation result
- a claim that `pwd` executed

Those belong to Gate 17 and later result-normalization/acceptance boundaries.

## GREEN criteria

G13 may be GREEN only when the current producer constructs the exact request from a current, valid Gate 12 kernel and all contract negative tests reject invalid provenance, invalid upstream state, authority mutation, and execution leakage.

UNKNOWN, missing, malformed, stale, cross-run, or already-executed evidence is not PASS.
