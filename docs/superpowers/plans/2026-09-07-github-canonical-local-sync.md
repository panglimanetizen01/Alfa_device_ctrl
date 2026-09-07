# GitHub Canonical Local Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make GitHub `master` the sole canonical source and provide a safe Termux synchronization path that treats the local tree only as a downstream copy.

**Architecture:** GitHub `master` remains the default and canonical branch. Termux synchronization fetches `origin/master`, requires a clean local `master`, and advances only with `git merge --ff-only`; dirty, ahead, and diverged states stop without mutation.

**Tech Stack:** Git, Bash, GitHub Actions, Termux.

**Spec:** `docs/superpowers/specs/2026-09-07-github-canonical-local-sync-design.md`

## Global Constraints

- GitHub `master` is the sole canonical source.
- Normal local synchronization must not discard local changes.
- Normal local synchronization must not create local commits.
- No `git reset --hard`, `git clean`, force push, or blind pull is used by the sync path.
- All project artifacts remain under the Alfa Device Ctrl repository tree.

---

### Task 1: Canonical synchronization script

**Files:**
- Create: `tools/sync_from_github.sh`

**Interfaces:**
- Consumes: local Git repository, `origin`, local branch `master`.
- Produces: explicit synchronization result and final local/remote SHA pair.

- [ ] **Step 1: Implement preflight checks**

The script must verify it is inside the expected repository, verify `origin` exists, require the current branch to be `master`, and require a clean `git status --porcelain=v1` result. Any failure must stop before changing refs.

- [ ] **Step 2: Fetch canonical master**

Run `git fetch origin master --prune` and resolve `origin/master` to an exact SHA.

- [ ] **Step 3: Classify history**

Compare `HEAD` and `origin/master` using `git merge-base --is-ancestor`. Report `ALIGNED`, `BEHIND`, `AHEAD`, or `DIVERGED` without modifying history for `AHEAD` or `DIVERGED`.

- [ ] **Step 4: Fast-forward only when behind**

For `BEHIND`, run `git merge --ff-only origin/master`. For `ALIGNED`, make no commit or ref change.

- [ ] **Step 5: Verify final state**

Require `HEAD = origin/master` and a clean working tree. Print `CANONICAL_SYNC=ALIGNED` plus both SHAs.

- [ ] **Step 6: Shell verification**

Run `bash -n tools/sync_from_github.sh` and a non-mutating source inspection of the script to verify it contains no destructive Git commands.

### Task 2: Canonical workflow documentation

**Files:**
- Modify: `README.md` if an appropriate workflow section exists; otherwise keep documentation in `docs/superpowers/specs/2026-09-07-github-canonical-local-sync-design.md`.

**Interfaces:**
- Consumes: canonical-sync design.
- Produces: operator-facing explanation of GitHub-first development.

- [ ] **Step 1: Document the source-of-truth rule**

State that `master` is canonical and local is downstream verification only.

- [ ] **Step 2: Document the safe sync command**

Use `bash tools/sync_from_github.sh` from the repository root.

- [ ] **Step 3: Document hard-stop states**

Explain that dirty, ahead, and diverged local states are diagnosed rather than overwritten.

### Task 3: End-to-end verification

**Files:**
- Test: repository Git state and script behavior.

**Interfaces:**
- Consumes: Task 1 script and GitHub `master`.
- Produces: evidence that the canonical model is operational.

- [ ] **Step 1: Verify GitHub default branch**

Confirm repository metadata reports `default_branch=master`.

- [ ] **Step 2: Verify canonical commit**

Confirm `refs/heads/master` resolves to the current canonical SHA.

- [ ] **Step 3: Verify script syntax**

Run `bash -n tools/sync_from_github.sh`.

- [ ] **Step 4: Run local canonical sync**

From the project root, execute `bash tools/sync_from_github.sh` and capture its final SHA/state output.

- [ ] **Step 5: Verify no local divergence**

Confirm `git rev-parse HEAD` equals `git rev-parse origin/master` and `git status --porcelain=v1` is empty.

- [ ] **Step 6: Continue downstream validation**

Only after canonical alignment, run the project's existing build/test/runtime verification flow.
