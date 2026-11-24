#!/usr/bin/env bash
# Runs sbt and runs it again when it failed only because a download from a repository failed, such as HTTP 429 from Maven
# Central. A run that failed for any other reason, including a failed test or compilation, fails at once, so a retry never
# hides a broken build. Every attempt runs the whole command again; nothing is skipped.
#
# Coursier leaves a marker (.<file>.error) next to every file it could not download and does not ask the repository again
# while the marker exists. Such markers also come back from the dependency cache of an earlier run, so the script removes
# them before every attempt; otherwise every retry would fail at once without a download.
#
# Usage: sbt-retry.sh <log file> <sbt arguments...>
# The output of every attempt goes to the console and is appended to the log file.
set -uo pipefail

log="${1:?usage: sbt-retry.sh <log file> <sbt arguments...>}"
shift
attempts=4
download_failure='Error downloading|could not retrieve sbt|CantDownloadModule|retryable HTTP error|download error: Caught java\.net\.|Server returned HTTP response code: 429'

forget_failed_downloads() {
  for cache in "$HOME/.cache/coursier" "${COURSIER_CACHE:-}"; do
    if [ -n "$cache" ] && [ -d "$cache" ]; then
      find "$cache" -type f -name '.*.error' -delete
    fi
  done
}

: > "$log"
for attempt in $(seq 1 "$attempts"); do
  forget_failed_downloads
  attempt_log="$(mktemp)"
  sbt "$@" 2>&1 | tee "$attempt_log"
  status=${PIPESTATUS[0]}
  cat "$attempt_log" >> "$log"
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if ! grep -Eq "$download_failure" "$attempt_log"; then
    exit "$status"
  fi
  if [ "$attempt" -lt "$attempts" ]; then
    wait_seconds=$((attempt * 60))
    echo "::warning::sbt could not download a dependency (attempt $attempt of $attempts), trying again in $wait_seconds s"
    sleep "$wait_seconds"
  fi
done
echo "::error::sbt could not download a dependency after $attempts attempts"
exit "$status"
