# Gate 17 — Runtime Execution Result Schema V1

Schema: `gate17-runtime-execution.v1`

G17 is the first gate permitted to execute the exact authorized command. It consumes only the exact current-run G16 authorization artifact and executes only `pwd` with `POSIX_PWD` semantics inside an explicitly supplied PRoot guest rootfs.

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
- execution_status=EXECUTED
- result_status=PASS
- command_result=/root
- command_returncode=0
- command_result_sha256
- engine_path and engine_sha256
- rootfs_path
- rootfs_os_id and rootfs_os_release_sha256
- guest_pid
- guest_proc_exe
- guest_proc_cwd
- guest_proc_root
- created_at
- execution_path=PRoot:-r:<rootfs>:-w:/root:/bin/sh:-c:pwd

Fail-closed: missing, stale, cross-run, malformed, hash-mismatched, non-AUTHORIZED, non-DEFERRED, wrong command, wrong semantics, wrong authority, missing PRoot executable, missing rootfs, invalid guest shell, failed execution, non-zero return code, missing guest process evidence, or host-directory `pwd` output blocks execution.

G17 must not select artifacts by timestamp or directory order. It must not execute any command other than the exact locked `pwd` request. The PRoot executable and guest rootfs are explicit inputs. Publication is atomic.

`command_result=/root` is accepted only together with the explicit PRoot `-r`/`-w` execution path and guest process/rootfs evidence; host `pwd` output is not valid G17 evidence.

G17 is GREEN only when its contract test proves real PRoot guest execution, exact G16 provenance, negative authorization/provenance cases, execution evidence, and protection of G1-G16. Hosted CI execution proves the PRoot boundary on Linux; Android DUT verification remains a separate live-runtime evidence requirement and must not be inferred from hosted CI.
