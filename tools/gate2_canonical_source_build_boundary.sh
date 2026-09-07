#!/usr/bin/env bash
set -u
ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo 'G2_STATUS=RED'; echo 'G2_REASON=not-a-git-repository'; exit 1; }
cd "$ROOT" || exit 1
EXPECTED_REPO='panglimanetizen01/Alfa_device_ctrl'
EXPECTED_BRANCH='master'
EVIDENCE='artifacts/gates/g2/canonical-source-build-boundary.txt'
G1_EVIDENCE='artifacts/gates/g1/foundation-contract.txt'
SCHEMA='g2-canonical-source-build-boundary.v1'
mkdir -p "$(dirname "$EVIDENCE")"
fail() { reason="$1"; { echo "schema_version=$SCHEMA"; echo 'gate=G2'; echo 'gate_status=RED'; echo "source_commit=${SOURCE_COMMIT:-UNKNOWN}"; echo "repository=$EXPECTED_REPO"; echo "reason=$reason"; } > "$EVIDENCE"; echo 'G2_STATUS=RED'; echo "G2_REASON=$reason"; echo "G2_EVIDENCE=$ROOT/$EVIDENCE"; return 1; }
SOURCE_COMMIT="$(git rev-parse HEAD 2>/dev/null)" || { fail 'missing-head'; exit 1; }
if ! printf '%s\n' "$SOURCE_COMMIT" | grep -Eq '^[0-9a-f]{40}$'; then fail 'malformed-source-commit'; exit 1; fi
if [ -n "${G2_EXPECT_SOURCE_COMMIT:-}" ] && [ "$SOURCE_COMMIT" != "$G2_EXPECT_SOURCE_COMMIT" ]; then fail 'stale-source-commit'; exit 1; fi
ORIGIN_URL="$(git remote get-url origin 2>/dev/null || true)"
case "$ORIGIN_URL" in https://github.com/$EXPECTED_REPO.git|https://github.com/$EXPECTED_REPO|git@github.com:$EXPECTED_REPO.git) ;; *) fail 'repository-identity-mismatch'; exit 1 ;; esac
BRANCH="$(git symbolic-ref --quiet --short HEAD 2>/dev/null || true)"
if [ "$BRANCH" != "$EXPECTED_BRANCH" ]; then
  if [ "${GITHUB_REF:-}" != "refs/heads/$EXPECTED_BRANCH" ] || [ "${GITHUB_SHA:-}" != "$SOURCE_COMMIT" ]; then
    fail 'branch-mismatch'; exit 1
  fi
  BRANCH='master(detached-ci)'
fi
git fsck --full --no-progress >/dev/null 2>&1 || { fail 'git-object-integrity-failed'; exit 1; }
if [ -n "$(git status --porcelain=v1 --untracked-files=all)" ]; then
  if git status --porcelain=v1 --untracked-files=all | grep -v '^?? artifacts/gates/g2/' | grep -q .; then fail 'worktree-not-clean'; exit 1; fi
fi
if git show-ref --verify --quiet refs/remotes/origin/master; then ORIGIN_MASTER="$(git rev-parse refs/remotes/origin/master)"; [ "$SOURCE_COMMIT" = "$ORIGIN_MASTER" ] || { fail 'origin-master-mismatch'; exit 1; }; else ORIGIN_MASTER='UNAVAILABLE'; fi
[ -f "$G1_EVIDENCE" ] || { fail 'g1-evidence-missing'; exit 1; }
[ "$(sed -n 's/^gate_status=//p' "$G1_EVIDENCE" | head -n1)" = GREEN ] || { fail 'g1-not-green'; exit 1; }
[ "$(sed -n 's/^source_commit=//p' "$G1_EVIDENCE" | head -n1)" = "$SOURCE_COMMIT" ] || { fail 'g1-source-commit-mismatch'; exit 1; }
[ "$(sed -n 's/^schema_version=//p' "$G1_EVIDENCE" | head -n1)" = g1-foundation-contract.v2 ] || { fail 'g1-schema-mismatch'; exit 1; }
for f in settings.gradle build.gradle gradlew gradle/wrapper/gradle-wrapper.properties app/build.gradle; do [ -f "$f" ] || { fail "required-build-file-missing:$f"; exit 1; }; done
[ -x gradlew ] || { fail 'gradlew-not-executable'; exit 1; }
WRAPPER_URL="$(sed -n 's/^distributionUrl=//p' gradle/wrapper/gradle-wrapper.properties | head -n1)"
WRAPPER_URL="${WRAPPER_URL//\\:/\:}"
case "$WRAPPER_URL" in https://services.gradle.org/distributions/gradle-*.zip) ;; *) fail 'invalid-gradle-wrapper-url'; exit 1 ;; esac
APP_ID="$(sed -n 's/^[[:space:]]*applicationId[[:space:]]*['"'"']\([^'"'"']*\)['"'"'].*/\1/p' app/build.gradle | head -n1)"
[ -n "$APP_ID" ] || { fail 'missing-application-id'; exit 1; }
SOURCE_TREE_SHA256="$(python3 - "$SOURCE_COMMIT" <<'PY'
import hashlib, subprocess, sys
commit=sys.argv[1]
paths=subprocess.check_output(["git","ls-tree","-r","--name-only",commit],text=True).splitlines()
h=hashlib.sha256()
for path in paths:
    data=subprocess.check_output(["git","show",f"{commit}:{path}"])
    h.update(path.encode()); h.update(b"\0"); h.update(hashlib.sha256(data).digest())
print(h.hexdigest())
PY
)"
WRAPPER_SHA256="$(sha256sum gradle/wrapper/gradle-wrapper.properties | awk '{print $1}')"
APP_BUILD_SHA256="$(sha256sum app/build.gradle | awk '{print $1}')"
{
  echo "schema_version=$SCHEMA"
  echo 'gate=G2'
  echo 'gate_status=GREEN'
  echo "source_commit=$SOURCE_COMMIT"
  echo "repository=$EXPECTED_REPO"
  echo "branch=$BRANCH"
  echo "origin_master=$ORIGIN_MASTER"
  echo "source_tree_sha256=$SOURCE_TREE_SHA256"
  echo 'g1_evidence=PASS'
  echo 'git_object_integrity=PASS'
  echo 'worktree_clean=PASS'
  echo 'build_boundary=PASS'
  echo "gradle_wrapper_distribution=$WRAPPER_URL"
  echo "gradle_wrapper_sha256=$WRAPPER_SHA256"
  echo "app_build_gradle_sha256=$APP_BUILD_SHA256"
  echo "application_id=$APP_ID"
  echo 'apk_release_before_g19=FORBIDDEN'
} > "$EVIDENCE"
echo 'G2_STATUS=GREEN'
echo "G2_SOURCE_COMMIT=$SOURCE_COMMIT"
echo "G2_EVIDENCE=$ROOT/$EVIDENCE"
cat "$EVIDENCE"
