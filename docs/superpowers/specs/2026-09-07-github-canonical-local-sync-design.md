# GitHub Canonical / Local Sync Design

## Goal
Make GitHub `master` the sole canonical source for Alfa Device Ctrl and make the Termux working tree a downstream copy used for build, runtime verification, and diagnostics.

## Decisions
- Repository default branch is `master` and remains the canonical branch.
- Local development work must not be treated as canonical source.
- Local synchronization is allowed only when the working tree is clean and the local branch is `master`.
- Synchronization uses `git fetch origin master` followed by `git merge --ff-only origin/master`.
- A local branch that is ahead of or diverged from `origin/master` is a hard stop; no reset, force update, rebase, or implicit overwrite is performed.
- Uncommitted local changes are a hard stop; they are not discarded automatically.
- CI, build, test, and runtime evidence remain downstream verification artifacts and do not become source-of-truth changes.
- `review/gate-sync` remains non-canonical; only `master` is the source branch for the local working tree.

## Canonical Flow
`GitHub master -> fetch -> clean local master -> fast-forward -> build/test/runtime verification`

## Safety Properties
1. No destructive Git operation is required for normal synchronization.
2. Local-only commits cannot silently replace canonical history.
3. Divergence is surfaced explicitly for diagnosis.
4. The exact remote commit SHA is reported before and after synchronization.
5. The repository state is independently verifiable with `git status`, `git rev-parse`, and `git merge-base`.

## Acceptance Criteria
- AC-1: GitHub reports `master` as the default branch.
- AC-2: A documented sync procedure refuses dirty, ahead, or diverged local state.
- AC-3: A clean local branch behind `origin/master` can only advance by fast-forward.
- AC-4: A clean local branch equal to `origin/master` reports aligned without creating a commit.
- AC-5: No normal sync path uses `git reset --hard`, `git clean`, force push, or blind pull.
- AC-6: CI continues to build and verify the canonical commit checked out by GitHub Actions.
