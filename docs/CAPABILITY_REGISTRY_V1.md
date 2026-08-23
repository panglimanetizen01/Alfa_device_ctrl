# Alfa Device Ctrl — Capability Registry V1

| ID | Description | Verification |
|---|---|---|
| STORAGE_READ | Read Android shared storage | Read test |
| STORAGE_WRITE | Write Android shared storage | Write/delete test |
| EXEC_PRIVATE | Execute file from private storage | Execution test |
| EXEC_SHARED | Execute file from Android shared storage | Execution test |
| NETWORK_DNS | Resolve public hostname | DNS lookup |
| GIT | Git executable available | command discovery |
| PYTHON3 | Python3 executable available | command discovery |
| JAVA | Java executable available | command discovery |
| JAVAC | Java compiler available | command discovery |
| GRADLE | Gradle executable available | command discovery |

## Registry Rules

1. Every capability has a unique ID.
2. Every capability has a defined verification method.
3. PASS requires evidence from the defined verification.
4. UNKNOWN must remain UNKNOWN.
5. Capability status is scoped to the current execution environment.
6. Registry membership does not imply capability availability.

## Initial State

Registry: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED
