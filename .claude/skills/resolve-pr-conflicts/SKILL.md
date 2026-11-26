---
name: resolve-pr-conflicts
description: Resolves merge conflicts of an open Coilix pull request and moves stacked branches after a squash merge. Use when the user says a PR has conflicts ("konflikty", "konflikty na 105"), when GitHub reports a PR as CONFLICTING, or after a PR that other branches were built on has been merged.
---

# Resolving conflicts of Coilix pull requests

Coilix merges every PR with "Squash and merge" (ADR 0008), so `master` never contains the commits of a branch, only one new commit. Almost every conflict comes from one of two causes, and each has its own fix. Find the cause first, then act.

## 1. Find the state

```bash
cd <repo root>
git fetch -q origin
git log --oneline -5 origin/master
gh pr list --state all --limit 8 --json number,state,baseRefName,headRefName,title
gh pr view <number> --json mergeable,mergeStateStatus,baseRefName
git log --oneline origin/master..<branch of the PR>
```

The last command lists the commits that the PR brings. Compare them with the commits of `master`:

- **Stacked branch after a squash.** The list contains commits of an earlier PR that is now merged, under their old hashes. The branch was built on that PR, which `master` now holds as one squashed commit, so its old commits collide with it. GitHub often retargets such a PR to `master` by itself, which makes it show the conflict.
- **Parallel PRs.** The list contains only the PR's own commits, but another PR that touched the same lines was merged first.

## 2. Keep the local changes of the user out of the way

The working tree often has a local change that never goes into a commit, typically `.idea/inspectionProfiles/Project_Default.xml`. Rebase refuses to run with it.

```bash
git status --short
git stash push -q -m "local idea profile" -- .idea/inspectionProfiles/Project_Default.xml
git stash show --stat        # must list only that file
```

Stash only the paths you name. `git stash push <path>` with other files in the index takes the whole index, so check `git stash show --stat` before you go on. Restore the stash with `git stash pop -q` at the end, also after a failure.

Never commit with `git commit -a`, because it picks up that local file. Stage files by name or with `git add -A -- . ':!.idea'`.

## 3a. Stacked branch: move only the PR's own commits

Take the last commit of the merged base PR as it was on the branch (its old hash, visible in `git log origin/master..<branch>`), then:

```bash
git switch -q <branch of the PR>
git rebase -q --onto origin/master <old hash of the last commit of the merged base PR>
git log --oneline -4          # only the PR's own commits on top of origin/master
gh pr edit <number> --base master
```

When several PRs form a stack (A → B → C) and A was merged, move B onto `master` as above, then move C onto the new B:

```bash
git switch -q <branch C>
git rebase -q --onto <branch B> <old hash of the last commit of B before its rebase>
```

Write the old hashes down before rebasing, because the rebase changes them.

## 3b. Parallel PRs: rebase and resolve

```bash
git switch -q <branch of the PR>
git rebase -q origin/master
git diff --name-only --diff-filter=U
```

Show each conflict before changing it:

```bash
awk '/^<<<<<<< /{p=1} p{print NR": "substr($0,1,160)} /^>>>>>>> /{p=0}' <file>
```

Rules for the files that collide most often in this repository. All of them are append-only lists, so the answer is almost always to keep both sides:

| File | Resolution |
|---|---|
| `changelog/<group>/unreleased.md` | keep the entries of both sides in the same section (`### Added`, `### Changed`) |
| `docs/adr/README.md` | keep both rows, ordered by ADR number; fix the order if `master` already has it wrong |
| `tasks/README.md` | keep both rows; for the same row, take the newer status and make sure it matches the steps that are really merged |
| `CLAUDE.md`, `docs/architecture/overview.md` | keep both changes; check that the merged text still reads as one paragraph |
| `90_demo/src/test/scala/DemoApplicationTest.scala` (bean names) | keep the names of both sides in alphabetical order |
| Scala sources | understand both changes; never drop the code of `master` to make the PR apply |

Resolve with a small script rather than by hand when the rule is "keep both sides":

```python
import re
pattern = re.compile(r"<<<<<<< [^\n]*\n(.*?)=======\r?\n(.*?)>>>>>>> [^\n]*\n", re.S)
text = open(path, "rb").read().decode()
text = pattern.sub(lambda m: m.group(1) + m.group(2), text)
open(path, "wb").write(text.encode())
```

Then:

```bash
grep -rn "<<<<<<<\|>>>>>>>" <resolved files>     # nothing left
git add <resolved files>
GIT_EDITOR=true git rebase --continue
```

After the rebase, check whether code merged on `master` in the meantime needs the change of the PR too. For example, when the PR replaces a pattern everywhere (such as `IO(log.…)` with `logF.…`), new code from `master` may still use the old one. Fix it in a new signed commit on the branch (`git commit -s`).

## 4. Verify

If the files did not change, the earlier verification still holds:

```bash
git diff --stat <old head of the branch> HEAD
```

An empty diff means the tree is the same as before the rebase. Otherwise run the checks from `CLAUDE.md`. When IntelliJ runs its own sbt, add `-Dsbt.server.autostart=false`:

```bash
sbt -Dsbt.server.autostart=false scalafmtAll testFull
sbt -Dsbt.server.autostart=false doc demo/run          # when src/main or the demo changed
(cd 99_docs/website && npm run check)                  # when docs, changelog or guides changed
```

Count the modules in the output: every module must report `Passed: Total …` and none `Failed`.

## 5. Publish

```bash
git push -q --force-with-lease origin <branch>
git stash pop -q
gh pr view <number> --json mergeable,baseRefName
```

Watch CI with a background command instead of `sleep`:

```bash
gh pr checks <number> --watch --interval 30 >/dev/null 2>&1; gh pr checks <number>
```

If a job fails, read only the failing lines before changing anything:

```bash
gh run view <run id> --job <job id> --log-failed | sed 's/\x1b\[[0-9;]*m//g' | grep -E "==> X|Failed|\[error\]"
```

A failure in a test that the PR does not touch is often a race that only the slower JDK 17 runner shows. Fix the race in the test, for example by waiting with `waitUntil`, and do not just rerun the job.

## Never

- Merge a PR. The maintainer merges.
- Push to `master`, or push with `--force` instead of `--force-with-lease`.
- Resolve a conflict by dropping the other side's content because it is in the way.
- Commit the local `.idea` file, or create a commit without `-s` (DCO).
- Add AI attribution to commits, PR descriptions or comments (`CLAUDE.md`, section "Commity").

## Report to the user

Say briefly, in Polish:

- what caused the conflict (stacked branch after a squash, or another PR in the same lines),
- which files were resolved and how,
- whether the tests and CI pass,
- which PR they can merge now and what you will move after it.
