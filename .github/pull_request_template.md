## What changes

<!-- Task identifier and a short description, e.g. COILIX-00004: Add @PostConstruct support. -->

## How to verify

<!-- Tests to look at, or steps in the demo application. -->

## Checklist

- [ ] The pull request covers one feature or one fix, and its title describes the whole change.
- [ ] Commits start with the task identifier and are signed off (`git commit -s`).
- [ ] Commits and this description do not mention AI tools (no `Co-Authored-By` for AI tools, no "Generated with" lines).
- [ ] `sbt testFull doc demo/run` passes and `doc` has no broken link warnings.
- [ ] Every new class has munit tests and every new public element has Scaladoc.
- [ ] Guides are updated in English and Polish (if the change is visible to users), following `docs/process/documentation.md`.
- [ ] `changelog/<group>/unreleased.md` has an entry for each changed group (if the change is visible to users).
- [ ] The change is **BREAKING**: yes / no. If yes, the changelog entry describes the migration.
- [ ] New dependencies are listed in `docs/legal/dependency-licenses.md`.
