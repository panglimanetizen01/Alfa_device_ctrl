#!/usr/bin/env bash
set -u

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo 'G2_STATUS=RED'
  echo 'G2_REASON=not-a-git-repository'
  exit 1
}
cd "$ROOT" || exit 1

EXPECTED_REPO='panglimanetizen01/Alfa_device_ctrl'
EXPECTED_BRANCH='master'
EVIDENCE='artifacts/gates/g2/canonical-source-build-boundary.txt'
SCHEMA='g2-canonical-source-build-boundary.v1'
mkdir -p "$(dirname "$EVIDENCE")"

fail() {
  reason="$1"
  {
    echo 'schema_version='"$SCHEMA"
    echo 'gate=G2'
    echo 'gate_status=RED'
    echo 'source_commit='"${SOURCE_COMMIT:-UNKNOWN}"
    echo 'repository='"$EXPECTED_REPO"
    echo 'reason='"$reason"
  } > "$EVIDENCE"
  echo 'G2_STATUS=RED'
  echo 'G2_REASON='"$reason"
  echo 'G2_EVIDENCE='"$ROOT/$EVIDENCE"
  return 1
}

SOURCE_COMMIT="$(git rev-parse HEAD 2>/dev/null)" || { fail 'missing-head'; exit 1; }
case "$SOURCE_COMMIT" in
  [0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]) ;;
  *) fail 'malformed-source-commit'; exit 1 ;;
esac

ORIGIN_URL="$(git remote get-url origin 2>/dev/null || true)"
case "$ORIGIN_URL" in
  https://github.com/$EXPECTED_REPO.git|https://github.com/$EXPECTED_REPO) ;;
  git@github.com:$EXPECTED_REPO.git) ;;
  *) fail 'repository-identity-mismatch'; exit 1 ;;
esac

BRANCH="$(git symbolic-ref --quiet --short HEAD 2>/dev/null || true)"
[ "$BRANCH" = "$EXPECTED_BRANCH" ] || { fail 'branch-mismatch'; exit 1; }

if ! git fsck --full --no-progress >/dev/null 2>&1; then
  fail 'git-object-integrity-failed'; exit 1
fi

if [ -n "$(git status --porcelain=v1 --untracked-files=all)" ]; then
  if git status --porcelain=v1 --untracked-files=all | grep -v '^?? artifacts/gates/g2/' | grep -q .; then
    fail 'worktree-not-clean'; exit 1
  fi
fi

if git show-ref --verify --quiet refs/remotes/origin/master; then
  ORIGIN_MASTER="$(git rev-parse refs/remotes/origin/master)"
  [ "$SOURCE_COMMIT" = "$ORIGIN_MASTER" ] || { fail 'origin-master-mismatch'; exit 1; }
else
  ORIGIN_MASTER='UNAVAILABLE'
fi

for f in settings.gradle build.gradle gradlew gradle/wrapper/gradle-wrapper.properties app/build.gradle; do
  [ -f "$f" ] || { fail "required-build-file-missing:$f"; exit 1; }
done

[ -x gradlew ] || { fail 'gradlew-not-executable'; exit 1; }
WRAPPER_URL="$(sed -n 's/^distributionUrl=//p' gradle/wrapper/gradle-wrapper.properties | head -n1)"
case "$WRAPPER_URL" in
  https://services.gradle.org/distributions/gradle-*.zip) ;;
  *) fail 'invalid-gradle-wrapper-url'; exit 1 ;;
esac

APP_ID="$(sed -n 's/^[[:space:]]*applicationId[[:space:]]*['"'"']\([^'"'"']*\)['"'"'].*/\1/p' app/build.gradle | head -n1)"
[ -n "$APP_ID" ] || { fail 'missing-application-id'; exit 1; }

SOURCE_TREE_SHA256="$(git ls-tree -r --name-only "$SOURCE_COMMIT" | while IFS= read -r path; do
  printf '%s\0' "$path"
done | while IFS= read -r -d '' path; do
  printf '%s\0' "$path"
  git show "$SOURCE_COMMIT:$path" | sha256sum | awk '{print $1}' | tr -d '\n'
  printf '\0'
done | sha256sum | awk '{print $1}')"

WRAPPER_SHA256="$(sha256sum gradle/wrapper/gradle-wrapper.properties | awk '{print $1}')"
APP_BUILD_SHA256="$(sha256sum app/build.gradle | awk '{print $1}')"

{
  echo 'schema_version='"$SCHEMA"
  echo 'gate=G2'
  echo 'gate_status=GREEN'
  echo 'source_commit='"$SOURCE_COMMIT"
  echo 'repository='"$EXPECTED_REPO"
  echo 'branch='"$BRANCH"
  echo 'origin_master='"$ORIGIN_MASTER"
  echo 'source_tree_sha256='"$SOURCE_TREE_SHA256"
  echo 'git_object_integrity=PASS'
  echo 'worktree_clean=PASS'
  echo 'build_boundary=PASS'
  echo 'gradle_wrapper_distribution='"$WRAPPER_URL"
  echo 'gradle_wrapper_sha256='"$WRAPPER_SHA256"
  echo 'app_build_gradle_sha256='"$APP_BUILD_SHA256"
  echo 'application_id='"$APP_ID"
  echo 'apk_release_before_g19=FORBIDDEN'
} > "$EVIDENCE"

echo 'G2_STATUS=GREEN'
echo 'G2_SOURCE_COMMIT='"$SOURCE_COMMIT"
echo 'G2_EVIDENCE='"$ROOT/$EVIDENCE"
cat "$EVIDENCE"
