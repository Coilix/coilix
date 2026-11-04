# Changelog

Each release group of Coilix has its own directory and its own versions:

| Directory | Group | Tag |
|---|---|---|
| `core/` | `coilix-common`, `coilix-annotations`, `coilix-slf4s-plugin`, `coilix-config`, `coilix-context`, `coilix-boot` | `core-vX.Y.Z` |
| `intellij/` | IntelliJ IDEA plugin | `intellij-vX.Y.Z` |
| `<module>/`, e.g. `web/` | one feature module | `<module>-vX.Y.Z` |

The released changes are listed at [docs.coilix.dev/changelog](https://docs.coilix.dev/changelog) and in the GitHub releases.

## Adding an entry

A pull request with a change that users can see adds an entry to `unreleased.md` of every group whose modules it changes. Entries are written by hand, not generated from commits.

```markdown
### Added

- `@PostConstruct` and `@PreDestroy` lifecycle callbacks (COILIX-00004).

### Changed

- **BREAKING** `ApplicationContext.close()` now runs `@PreDestroy` callbacks.
  Code that called them by hand should stop doing so (COILIX-00004).
```

- Sections are `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed` and `Security`, in this order, each at most once and only when it has entries. This follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
- Each entry is one list item that starts with `- ` and contains the task identifier (`COILIX-NNNNN`). A longer entry continues on lines indented by two spaces.
- An incompatible change starts with **BREAKING** and says how to migrate.
- `unreleased.md` has no front matter and may be empty.

## Release files

Before a release, `unreleased.md` is renamed to `<version>.md`, gets front matter, and an empty `unreleased.md` takes its place. `npm run prepare-release -- <group> <version>` in `12_docs/website` does this on a `release/<group>-v<version>` branch and opens the pull request:

```markdown
---
version: 0.2.0
date: 2026-12-01
requires-core: "0.2"
---

### Added

- ...
```

- `version` is the same as the file name, and `date` is the release date in the form `YYYY-MM-DD`.
- `requires-core` is the core line that a feature module works with. Only feature modules have it, `core` and `intellij` do not.
- A release file is not edited after the release.

The `Documentation site` job checks every file in this directory (`npm run check` in `12_docs/website`).
