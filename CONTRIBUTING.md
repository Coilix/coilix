# Contributing to Coilix

Thank you for helping. This file is a short summary. The full workflow (in Polish) is in [docs/process/development.md](docs/process/development.md) and the code rules are in [CLAUDE.md](CLAUDE.md).

## Before you start

- For anything larger than a small fix, open an issue first so we can agree on the approach.
- Work is tracked as tasks in [tasks/](tasks/README.md) with identifiers `SCALING-NNNNN` (the project was called Scaling before it became Coilix). Changes outside any task use `SCALING-00000`.

## Workflow

1. Branch from `master`: `feature/SCALING-NNNNN-short-description` or `fix/SCALING-NNNNN-short-description`.
2. Keep each commit to one logical change. Commit messages are in English and start with the task identifier:

   ```text
   SCALING-00004: Add @PostConstruct and @PreDestroy support
   ```

3. Sign off every commit (see [Developer Certificate of Origin](#developer-certificate-of-origin)): `git commit -s`.
4. Before opening a pull request, run:

   ```sh
   sbt testFull doc demo/run
   ```

   and check that the `doc` output has no `Couldn't resolve a member for the given link query` warnings.
5. Rebase on `master` and open a pull request against `master`. CI must pass. Pull requests are merged with "Rebase and merge", so the commits land on `master` as they are.

## Code rules in short

- Scala 3 with braces only (`-no-indent -old-syntax`), no `var`, `null`, `return`, `while` or mutable collections, no `println`. The build enforces this.
- A trait first, then the implementation. Framework components expose a `given ConfigPath[T]` and a `default` in the companion object so users can replace them with a `class` key.
- One type per file, named like the file.
- Every new class has munit tests. Every public element in `src/main` has Scaladoc in English.
- No code, documentation, messages or tests copied or paraphrased from Spring or other frameworks.
- New dependencies must be under a license compatible with Apache-2.0 (see [docs/legal/dependency-licenses.md](docs/legal/dependency-licenses.md)). GPL and AGPL are not allowed.

## Changelog and versioning

Coilix follows [Semantic Versioning](https://semver.org/) with one version for all modules. Every user-visible change adds an entry to the `Unreleased` section of `CHANGELOG.md` with the task identifier. Incompatible changes are marked **BREAKING** and describe the migration. Details are in [docs/architecture/versioning.md](docs/architecture/versioning.md).

## Developer Certificate of Origin

Coilix uses the [Developer Certificate of Origin 1.1](https://developercertificate.org/) instead of a contributor license agreement. By adding a `Signed-off-by` line to a commit, you certify that you wrote the change or otherwise have the right to submit it under the Apache License 2.0:

```text
Signed-off-by: Your Name <your.email@example.com>
```

`git commit -s` adds the line using your `user.name` and `user.email`.

## License

By contributing, you agree that your contributions are licensed under the [Apache License, Version 2.0](LICENSE).
