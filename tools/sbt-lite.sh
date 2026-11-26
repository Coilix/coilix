#!/usr/bin/env bash
# Runs sbt with the whole output in a log file and prints only a short summary: the result, the test counts, and on a
# failure the failed tests and the first error lines. Meant for agents and terminals where the full output is noise; the
# details stay in the log, so they can be read without running sbt again.
#
# Usage:
#   tools/sbt-lite.sh <sbt arguments...>     e.g. tools/sbt-lite.sh testFull doc
#   tools/sbt-lite.sh --summary [log file]   summarizes a saved log again (default: the last run)
#
# The log of the last run is target/sbt-lite/last.log. SBT_LITE_ERRORS sets how many error lines are printed (default 40).
# sbt runs with --server and -Dsbt.server.autostart=false, so it works next to an sbt server started by IntelliJ.
set -uo pipefail

log_dir="target/sbt-lite"
last_log="$log_dir/last.log"
error_limit="${SBT_LITE_ERRORS:-40}"
silent_failures="Couldn't resolve a member for the given link query|warnings found and -Werror specified"

strip_colors() {
  sed -E 's/\x1b\[[0-9;]*[A-Za-z]//g' "$1"
}

test_counts() {
  grep -E '^\[(info|error)\] (Passed|Failed): Total ' "$1" |
    sed -E 's/.*Total ([0-9]+), Failed ([0-9]+), Errors ([0-9]+), Passed ([0-9]+).*/\1 \2 \3 \4/' |
    awk '{ t += $1; f += $2; e += $3; p += $4; n++ }
      END { if (n > 0) { printf "tests: %d passed, %d failed, %d errors, %d total\n", p, f, e, t } else { print "tests: none run" } }'
}

failed_tests() {
  grep -E '^==> X |^\[error\][[:space:]]+(dev|com)\.[A-Za-z0-9_.]+$' "$1" | sed -E 's/^\[error\][[:space:]]+/  suite: /; s/^==> X /  test:  /' | sort -u
}

error_lines() {
  grep -E '^\[error\]' "$1" |
    grep -vE '^\[error\] (Failed: Total|Failed tests:|Error during tests:)|^\[error\][[:space:]]+(dev|com)\.[A-Za-z0-9_.]+$' |
    awk '!seen[$0]++'
}

summarize() {
  local raw="$1" status="${2:-}" clean
  clean="$(mktemp)"
  strip_colors "$raw" > "$clean"
  test_counts "$clean"
  local failed
  failed="$(failed_tests "$clean")"
  if [ -n "$failed" ]; then
    echo "failed:"
    echo "$failed"
  fi
  local errors total
  errors="$(error_lines "$clean")"
  if [ -n "$errors" ] && [ "${status:-1}" != "0" ]; then
    total="$(printf '%s\n' "$errors" | wc -l)"
    echo "errors (first $error_limit of $total):"
    printf '%s\n' "$errors" | head -n "$error_limit" | sed 's/^/  /'
  fi
  local silent
  silent="$(grep -nE "$silent_failures" "$clean" | head -n 10)"
  if [ -n "$silent" ]; then
    echo "problems that do not fail sbt (CI fails on them):"
    printf '%s\n' "$silent" | sed 's/^/  /'
  fi
  echo "log: $raw"
  rm -f "$clean"
}

if [ "${1:-}" = "--summary" ]; then
  summarize "${2:-$last_log}"
  exit 0
fi

if [ "$#" -eq 0 ]; then
  echo "usage: tools/sbt-lite.sh <sbt arguments...> | --summary [log file]" >&2
  exit 2
fi

mkdir -p "$log_dir"
started=$(date +%s)
sbt --server -Dsbt.server.autostart=false -Dsbt.supershell=false -Dsbt.color=false "$@" > "$last_log" 2>&1
status=$?
elapsed=$(( $(date +%s) - started ))

if [ "$status" -eq 0 ]; then
  echo "sbt-lite: OK  sbt $*  (${elapsed} s)"
else
  echo "sbt-lite: FAILED (exit $status)  sbt $*  (${elapsed} s)"
fi
summarize "$last_log" "$status"
exit "$status"
