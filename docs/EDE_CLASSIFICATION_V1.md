# EDE Classification Rules V1

## Android Host

ANDROID_HOST = DETECTED

Required evidence:

- kernel contains "android"
OR
- /storage/emulated/0 exists

Otherwise:

ANDROID_HOST = UNKNOWN

## Container Environment

CONTAINER_ENVIRONMENT = DETECTED

Evidence examples:

- UserLAnd markers
- proot markers
- container markers

If no evidence exists:

CONTAINER_ENVIRONMENT = UNKNOWN

## Toolchain Status

PASS:
all required tools present

WARNING:
some tools missing

ERROR:
core tools missing

## Storage Status

PASS:
shared storage exists and writable

WARNING:
shared storage exists but not writable

ERROR:
shared storage missing
