# GitHub-First Canonical Workflow

## Authority

`master` on GitHub is the canonical source of Alfa Device Ctrl. The repository default branch is `master`.

The Termux checkout is a downstream working copy. It is used to build, test, inspect, and perform Android runtime verification. It is not an independent source of truth.

## Normal flow

```text
GitHub master
    |
    | fetch
    v
Termux local master
    |
    | build / test / runtime verification
    v
Evidence
```

Source changes are committed to GitHub `master`. Local synchronization is performed only after the canonical commit exists remotely.

## Safe local synchronization

From the repository root:

```bash
bash tools/sync_from_github.sh
```

The command:

1. Requires the current branch to be `master`.
2. Requires a clean working tree.
3. Verifies the `origin` remote points to `panglimanetizen01/Alfa_device_ctrl`.
4. Fetches `origin/master`.
5. Fast-forwards local `master` only when it is strictly behind.
6. Makes no history mutation when local is aligned.
7. Stops instead of overwriting local commits when local is ahead or diverged.
8. Reports the exact final local and GitHub commit SHA.

## Hard stops

`CANONICAL_SYNC=BLOCKED` is expected when:

- the checkout is not the Alfa Device Ctrl repository;
- `origin` is missing or points elsewhere;
- the current branch is not `master`;
- uncommitted local changes exist;
- local history is ahead of GitHub;
- local and GitHub histories have diverged; or
- the post-sync SHA cannot be proven equal.

These states require diagnosis. The canonical workflow never resolves them by discarding data.

## Prohibited normal synchronization operations

The canonical sync path does not use:

- `git reset --hard`;
- `git clean`;
- force push;
- blind `git pull`; or
- automatic rebase of local work.

## Verification rule

After synchronization, the canonical state is proven only when:

```text
git rev-parse HEAD == git rev-parse origin/master
working tree == clean
branch == master
```

Only then should downstream build, test, and Android runtime verification proceed.

## Non-canonical branches

`review/gate-sync` may exist for review or historical workflow work, but it is not the canonical source branch. The local canonical checkout tracks `origin/master`.
