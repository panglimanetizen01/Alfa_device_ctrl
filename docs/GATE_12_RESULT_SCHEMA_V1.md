# Gate 12 Result Schema V1

Required fields:

- `schema_version=gate12-runtime-kernel.v1`
- `gate=gate12`
- `gate_status=PASS`
- `kernel_status=PASS`
- `pipeline_run_id`
- `source_commit`
- `gate4_contract_sha256`
- `profile_sha256`
- `gate11_artifact`
- `orchestrator_id=orchestrator-<RUN_ID>`
- `kernel_id=kernel-<RUN_ID>`
- `kernel_name=runtime_self_kernel`
- `kernel_type=RUNTIME_KERNEL`
- `execution_status=DEFERRED`
- `execution_authority=G17`
- `created_at`
- `execution_path`

A G12 PASS is valid only when the exact Gate 11 artifact is PASS, current HEAD matches its source commit, and all provenance identities match. G12 does not execute a guest command.
