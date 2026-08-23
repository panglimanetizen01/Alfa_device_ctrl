# Environment Discovery Engine (EDE) V1

## Purpose

EDE is responsible for discovering and classifying the real execution environment.

EDE must report facts.

EDE must not infer unsupported capabilities.

## Discovery Targets

### Identity

- username
- uid
- gid
- groups

### Kernel

- kernel name
- kernel version
- architecture

### Environment

- HOME
- PWD
- SHELL

### Storage

- Android shared storage
- private storage
- mounted paths
- writable paths

### Toolchain

- git
- python3
- java
- javac
- gradle

### Android Indicators

- Android storage presence
- Android filesystem markers
- Android environment markers

## Classification

EDE must classify:

- ANDROID_HOST
- CONTAINER_ENVIRONMENT
- TOOLCHAIN_STATUS
- STORAGE_STATUS

## Output Rules

Status values:

PASS
WARNING
ERROR
BLOCKED

Every status must include evidence.

## Non-Goals

EDE does not claim:

- Android API access
- Camera access
- Bluetooth access
- SMS access

unless independently verified.

## Initial State

Specification: DEFINED
Implementation: NOT STARTED
Verification: NOT STARTED
