# EDE Result Schema V1

## Purpose

Defines the official output structure produced by Environment Discovery Engine.

EDE output must be machine-readable and human-auditable.

## Result Sections

### HEADER

Fields:

- timestamp
- ede_version
- execution_path

### IDENTITY

Fields:

- username
- uid
- gid
- groups

Status:
PASS | WARNING | ERROR | BLOCKED

### KERNEL

Fields:

- kernel_name
- kernel_version
- architecture

Status:
PASS | WARNING | ERROR | BLOCKED

### ENVIRONMENT

Fields:

- home
- pwd
- shell

Status:
PASS | WARNING | ERROR | BLOCKED

### STORAGE

Fields:

- android_shared_storage
- writable_shared_storage
- private_storage

Status:
PASS | WARNING | ERROR | BLOCKED

### TOOLCHAIN

Fields:

- git
- python3
- java
- javac
- gradle

Status:
PASS | WARNING | ERROR | BLOCKED

### CLASSIFICATION

Fields:

- android_host
- container_environment
- toolchain_status
- storage_status

Status:
PASS | WARNING | ERROR | BLOCKED

## Evidence Rule

Every reported value must be backed by collected evidence.

Unknown values must be reported as UNKNOWN.

Unknown values must never be converted into assumptions.

## Output Formats

Phase 1:

- text

Future:

- json
- yaml

## Initial State

Schema: DEFINED
Implementation: NOT STARTED
