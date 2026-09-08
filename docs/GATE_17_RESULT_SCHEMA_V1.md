# Gate 17 — Runtime Execution Result Schema V1

Schema: `gate17-runtime-execution.v1`

G17 is the first gate permitted to execute the exact authorized command. It consumes only the exact current-run G16 authorization artifact and executes only `pwd` with `POSIX_PWD` semantics.

Required PASS evidence:

- schema_version=gate17-runtime-execution.v1
- gate=gate17
- gate_status=PASS
- deterministic execution_id
- pipeline_run_id and current source_commit
- Gate 4/profile hashes
- exact G16 authorization hash and artifact
- request_id=pwd-request-<RUN_ID>
- command=pwd
- command_semantics=POSIX_PWD
- exact command SHA-256
- authorization_status=AUTHORIZED
- execution_status=PASS
- result_status=PASS
- command_result
- command_returncode=0
- command_result_sha256
- created_at
- execution_path

Fail-closed: missing, stale, cross-run, malformed, hash-mismatched, non-AUTHORIZED, non-DEFERRED, wrong command, wrong semantics, or wrong authority evidence blocks execution.

G17 must not select artifacts by timestamp or directory order. It must not execute any command other than the exact locked `pwd` request. Publication is atomic.

G17 contract GREEN requires the synthetic boundary/negative contract test to prove the execution boundary and protection rules. G17 live GREEN requires an additional fresh current-run G4→G17 execution proof with real upstream artifacts and the authoritative G17 executor. Target Android/Termux verification remains mandatory and is not replaced by CI.
