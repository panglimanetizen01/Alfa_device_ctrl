# Gate 10 Result Schema V1

Required fields for a GREEN G10 artifact:

```text
schema_version=gate10-runtime-workflow.v1
gate=gate10
gate_status=PASS
workflow_status=PASS
pipeline_run_id=<run_id>
source_commit=<40-hex-current-head>
gate4_contract_sha256=<64-hex>
profile_sha256=<64-hex>
gate9_artifact=<path>
action_id=action-<run_id>
workflow_id=workflow-<run_id>
workflow_name=runtime_self_workflow
workflow_type=RUNTIME_WORKFLOW
execution_status=DEFERRED
execution_authority=G17
created_at=<timestamp>
execution_path=<canonical-project-root>
```

G10 does not execute the guest command. `execution_status=DEFERRED` is mandatory and actual runtime execution remains G17.
