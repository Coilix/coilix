#!/usr/bin/env bash
# Decides whether a CI job runs its build steps, and writes run=true or run=false to $GITHUB_OUTPUT.
#
# Every event other than a pull request runs everything. A pull request skips the build steps only when every changed file
# is on the list below, which holds only files that no sbt task, test or site script reads. Any other file, an empty diff or
# a checkout without the merge commit runs everything. Before adding a path to the list, check that nothing in project/,
# build.sbt, the tests or 99_docs/website/scripts reads it.
#
# Usage: needs-build.sh <build|site>
set -euo pipefail

group="${1:?usage: needs-build.sh <build|site>}"
notes='docs/|tasks/|\.idea/|\.run/|(README|ROADMAP|CLAUDE|CONTRIBUTING|CODE_OF_CONDUCT|SECURITY)\.md$'

case "$group" in
  build) ignored="^(changelog/|$notes)" ;;
  site) ignored="^($notes)" ;;
  *)
    echo "Unknown group $group" >&2
    exit 2
    ;;
esac

decide() {
  if [ "${GITHUB_EVENT_NAME:-}" != "pull_request" ]; then
    echo "true"
  elif ! git rev-parse --quiet --verify HEAD^2 > /dev/null; then
    echo "true"
  else
    changed="$(git diff --name-only HEAD^1 HEAD)"
    if [ -z "$changed" ] || grep -qvE "$ignored" <<< "$changed"; then
      echo "true"
    else
      echo "false"
    fi
  fi
}

run="$(decide)"
echo "run=$run" >> "$GITHUB_OUTPUT"
if [ "$run" = "false" ]; then
  echo "::notice::Only files that the $group steps do not read changed, so they are skipped"
fi
