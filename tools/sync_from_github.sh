#!/usr/bin/env bash
set -euo pipefail

main() {
    local root branch origin_url head_sha remote_sha final_sha

    root="$(git rev-parse --show-toplevel 2>/dev/null)" || {
        printf '%s\n' 'CANONICAL_SYNC=BLOCKED reason=not-a-git-repository'
        return 2
    }
    cd "$root"

    if [[ "$(basename "$root")" != "Alfa_device_ctrl" ]]; then
        printf 'CANONICAL_SYNC=BLOCKED reason=unexpected-repository-root root=%s\n' "$root"
        return 2
    fi

    origin_url="$(git remote get-url origin 2>/dev/null)" || {
        printf '%s\n' 'CANONICAL_SYNC=BLOCKED reason=origin-missing'
        return 2
    }
    case "$origin_url" in
        https://github.com/panglimanetizen01/Alfa_device_ctrl.git|git@github.com:panglimanetizen01/Alfa_device_ctrl.git)
            ;;
        *)
            printf 'CANONICAL_SYNC=BLOCKED reason=unexpected-origin origin=%s\n' "$origin_url"
            return 2
            ;;
    esac

    branch="$(git branch --show-current)"
    if [[ "$branch" != "master" ]]; then
        printf 'CANONICAL_SYNC=BLOCKED reason=local-branch-must-be-master branch=%s\n' "${branch:-DETACHED}"
        return 2
    fi

    if [[ -n "$(git status --porcelain=v1)" ]]; then
        printf '%s\n' 'CANONICAL_SYNC=BLOCKED reason=working-tree-not-clean'
        git status --short
        return 2
    fi

    printf 'CANONICAL_REMOTE=%s\n' "$origin_url"
    printf '%s\n' 'FETCH=origin/master'
    git fetch origin master --prune

    head_sha="$(git rev-parse HEAD)"
    remote_sha="$(git rev-parse origin/master)"
    printf 'LOCAL_BEFORE=%s\n' "$head_sha"
    printf 'REMOTE_MASTER=%s\n' "$remote_sha"

    if [[ "$head_sha" == "$remote_sha" ]]; then
        printf '%s\n' 'HISTORY=ALIGNED'
    elif git merge-base --is-ancestor "$head_sha" "$remote_sha"; then
        printf '%s\n' 'HISTORY=BEHIND action=FAST_FORWARD_ONLY'
        git merge --ff-only origin/master
    elif git merge-base --is-ancestor "$remote_sha" "$head_sha"; then
        printf '%s\n' 'CANONICAL_SYNC=BLOCKED reason=local-ahead-of-github'
        printf 'LOCAL=%s\nREMOTE=%s\n' "$head_sha" "$remote_sha"
        return 3
    else
        printf '%s\n' 'CANONICAL_SYNC=BLOCKED reason=history-diverged-from-github'
        printf 'LOCAL=%s\nREMOTE=%s\n' "$head_sha" "$remote_sha"
        return 4
    fi

    final_sha="$(git rev-parse HEAD)"
    remote_sha="$(git rev-parse origin/master)"
    if [[ "$final_sha" != "$remote_sha" ]]; then
        printf 'CANONICAL_SYNC=BLOCKED reason=post-sync-sha-mismatch local=%s remote=%s\n' "$final_sha" "$remote_sha"
        return 5
    fi

    if [[ -n "$(git status --porcelain=v1)" ]]; then
        printf '%s\n' 'CANONICAL_SYNC=BLOCKED reason=working-tree-dirty-after-sync'
        git status --short
        return 6
    fi

    printf 'LOCAL_AFTER=%s\n' "$final_sha"
    printf 'REMOTE_MASTER=%s\n' "$remote_sha"
    printf '%s\n' 'CANONICAL_SYNC=ALIGNED'
    printf '%s\n' 'SOURCE_OF_TRUTH=GITHUB_MASTER'
    printf '%s\n' 'LOCAL_ROLE=DOWNSTREAM_COPY'
}

main "$@"
