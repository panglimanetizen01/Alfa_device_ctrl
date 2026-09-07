# Gate 7 Result Schema V1

## Required PASS evidence

A Gate 7 GREEN result MUST contain:

```text
schema_version=gate7-runtime-session.v1
gate=gate7
gate_status=PASS
session_status=PASS
pipeline_run_id=<explicit run>
source_commit=<current 40-hex HEAD>
implementation_commit=<40-hex Android implementation identity>
gate4_contract_sha256=<64-hex>
profile_sha256=<64-hex>
session_id=<non-empty token>
request_id=<non-empty token>
runtime_id=ubuntu
process_pid=<positive integer>
pty_status=PASS
prompt_observed=PASS
engine_path=<non-empty>
rootfs_path=<non-empty>
runtime_evidence=<non-empty>
source_evidence=<Android operation-evidence.v1 path>
```

## Reject conditions

The verifier MUST reject:

- missing Gate 6 bootstrap;
- Gate 6 bootstrap not `PASS`;
- stale Gate 6 `source_commit` relative to current HEAD;
- missing/malformed provenance hashes or commits;
- missing Android session evidence;
- cross-run `pipeline_run_id`, `request_id`, or session identity;
- runtime other than the selected `ubuntu` runtime;
- missing or invalid PID;
- `PTY_CREATED` without `READY`;
- prompt absence or non-PASS PTY evidence;
- Gate 6 bootstrap alone without Android session evidence;
- `UNKNOWN`, malformed, or ambiguous values.

G7 does not accept prompt text as guest command evidence. Guest command acceptance remains downstream work.
