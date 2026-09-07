# Gate 11 Result Schema V1

Required fields:

- `schema_version=gate11-runtime-orchestrator.v1`
- `gate=gate11`
- `gate_status=PASS`
- `orchestrator_status=PASS`
- `pipeline_run_id`
- `source_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `gate10_artifact`
- `workflow_id=workflow-<pipeline_run_id>`
- `orchestrator_id=orchestrator-<pipeline_run_id>`
- `orchestrator_name=runtime_self_orchestrator`
- `orchestrator_type=RUNTIME_ORCHESTRATOR`
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `created_at`
- `execution_path`

Unknown, missing, malformed, stale, cross-run, or cross-contract provenance cannot produce PASS.
