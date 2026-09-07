# Gate 14 Result Schema V1

## Schema ID

`gate14-runtime-command-validation.v1`

## Purpose

G14 is the command-validation boundary between the concrete request produced by G13 and the policy boundary owned by G15.

G14 validates the exact request; it does not execute it, authorize execution, or apply policy.

## Required PASS fields

```text
schema_version=gate14-runtime-command-validation.v1
gate=gate14
gate_status=PASS
validation_status=ALLOWED
validation_id=pwd-validation-<RUN_ID>
pipeline_run_id=<exact run id>
source_commit=<40-hex current HEAD>
gate4_contract_sha256=<64-hex>
profile_sha256=<64-hex>
gate13_request_sha256=<64-hex hash of exact input artifact>
gate13_artifact=<explicit path>
request_id=pwd-request-<RUN_ID>
command=pwd
command_semantics=POSIX_PWD
command_sha256=<SHA256 of POSIX pwd request>
execution_status=DEFERRED
execution_authority=G17
execution_path=DEFERRED:G17
created_at=<UTC timestamp>
validation_path=<producer project root>
validation_reason=<reason>
```

## Validation contract

G14 may produce `ALLOWED` only when all of the following are true:

- the exact G13 schema is present;
- G13 status is `PASS` and request status is `REQUESTED`;
- the run ID matches the explicit input and current source commit exactly;
- Gate 4 and profile provenance are valid 64-hex values;
- the Gate 12 kernel provenance hash is present and valid;
- `kernel_id`, `request_id`, command, command semantics, and command hash are exact;
- execution remains `DEFERRED` with authority `G17` and path `DEFERRED:G17`;
- the exact referenced Gate 12 artifact exists and its SHA-256 matches `gate12_kernel_sha256`;
- the G13 input artifact itself is hashed into the G14 evidence.

## Prohibited G14 behavior

G14 must not:

- execute `pwd` or any shell/runtime command;
- invoke `runtime_stage.sh`, `runtime_execution.sh`, `/bin/sh -c`, `eval`, or equivalent execution paths;
- produce command stdout/stderr or return-code evidence;
- convert validation into authorization;
- change capability status;
- select artifacts by directory order, timestamp, `ls -1t`, or `head -1`.

## Fail-closed rule

Missing, malformed, stale, cross-run, hash-mismatched, already-executed, or semantically invalid G13 evidence is `BLOCKED` and must result in a non-zero producer exit code. `BLOCKED` is never `PASS`.

## GREEN criteria

G14 is GREEN only when the canonical producer, positive contract case, negative provenance cases, execution-boundary checks, and G1-G13 protection checks all pass against the current canonical source.
