# Gate 9 Result Schema V1

A GREEN Gate 9 artifact MUST contain:

```text
schema_version=gate9-runtime-action.v1
gate=gate9
gate_status=PASS
action_status=PASS
task_status=PASS
pipeline_run_id=<explicit run>
source_commit=<current 40-hex HEAD>
gate4_contract_sha256=<64-hex>
profile_sha256=<64-hex>
gate8_artifact=<exact G8 artifact path>
task_id=<non-empty token>
action_id=<non-empty token>
action_name=runtime_self_action
action_type=RUNTIME_ACTION
execution_status=DEFERRED
execution_authority=G17
created_at=<UTC timestamp>
execution_path=<canonical project root>
```

Reject conditions:

- missing or malformed G8 artifact;
- G8 `gate_status` or `task_status` not `PASS`;
- G8 source commit is not the current HEAD;
- cross-run, cross-contract, or cross-profile provenance;
- missing/invalid task identity;
- unexpected task name/type;
- capability status mutation or invented capability result;
- action execution attempted in G9;
- historical run defaults or newest-artifact discovery;
- missing timestamp or execution path.

`PASS` means the runtime action was constructed and admitted. It does **not** mean the action or guest command executed successfully. Execution authority remains downstream at G17.
