# Capability Discovery Engine (CDE) V1

## Purpose

CDE determines actual capabilities available to the runtime.

CDE must verify capabilities.

CDE must not infer capabilities.

## Capability States

PASS
WARNING
ERROR
BLOCKED

## Initial Capability Set

### STORAGE_READ

Can read Android shared storage.

### STORAGE_WRITE

Can write Android shared storage.

### EXEC_PRIVATE

Can execute files from private storage.

### EXEC_SHARED

Can execute files from shared storage.

### NETWORK_DNS

Can resolve public hostnames.

### GIT

Git executable available.

### PYTHON3

Python3 executable available.

### JAVA

Java executable available.

### JAVAC

Java compiler available.

### GRADLE

Gradle executable available.

## Evidence Rule

Every capability requires:

- test procedure
- observed result
- capability state

No capability may be marked PASS without evidence.

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED
