# Gate 8 Result Schema V1

A GREEN Gate 8 artifact MUST contain:

```text
schema_version=gate8-runtime-task.v1
gate=gate8
gate_status=PASS
task_status=PASS
pipeline_run_id=<explicit run>
source_commit=<current 40-hex HEAD>
gate4_contract_sha256=<64-hex>
profile_sha256=<64-hex>
gate7_artifact=<exact G7 artifact path>
session_id=<non-empty token>
request_id=<non-empty token>
runtime_id=<selected runtime>
task_id=<non-empty token>
task_name=runtime_self_test
task_type=SESSION_TASK
execution_status=DEFERRED
execution_authority=G17
created_at=<UTC timestamp>
execution_path=<canonical project root>
```

Reject conditions:

- missing or malformed G7 artifact;
- G7 `gate_status` or `session_status` not `PASS`;
- G7 source commit is not the current HEAD;
- cross-run or cross-contract provenance;
- missing session/request/runtime identity;
- invalid or UNKNOWN provenance;
- task name/type missing or unexpected;
- `execution_status` other than `DEFERRED`;
- any attempt to execute the guest command in G8;
- historical run defaults or newest-artifact discovery.

`PASS` means the task was constructed and admitted. It does NOT mean the guest task executed successfully. Guest execution remains downstream at G17.
