import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {after, test} from 'node:test';
import {changelogProblems, compareVersions, releaseNotes, renderChangelog} from './changelog.mjs';

const entry = '### Added\n\n- New feature (COILIX-00001).\n';
const coreRelease = (version, date = '2026-12-01') => `---\nversion: ${version}\ndate: ${date}\n---\n\n${entry}`;
const webRelease = (version) => `---\nversion: ${version}\ndate: 2026-12-02\nrequires-core: "0.1"\n---\n\n${entry}`;

const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'coilix-changelog-'));

after(() => fs.rmSync(temporary, {recursive: true, force: true}));

function changelog(files) {
  const root = fs.mkdtempSync(path.join(temporary, 'case-'));
  Object.entries(files).forEach(([file, content]) => {
    fs.mkdirSync(path.dirname(path.join(root, file)), {recursive: true});
    fs.writeFileSync(path.join(root, file), content);
  });
  return root;
}

function problemsOf(files) {
  return changelogProblems(changelog(files));
}

test('accepts valid groups', () => {
  assert.deepEqual(
    problemsOf({
      'README.md': '# Changelog\n',
      'core/unreleased.md': '',
      'core/0.1.0.md': coreRelease('0.1.0'),
      'web/unreleased.md': `${entry}\n### Fixed\n\n- A fix\n  over two lines (COILIX-00002).\n`,
      'web/0.1.0-RC1.md': webRelease('0.1.0-RC1'),
    }),
    [],
  );
});

test('requires unreleased.md in every group', () => {
  assert.deepEqual(problemsOf({'core/0.1.0.md': coreRelease('0.1.0')}), ['core: missing unreleased.md']);
});

test('rejects files that are not releases', () => {
  assert.deepEqual(problemsOf({'core/unreleased.md': '', 'core/notes.txt': '', 'CHANGELOG.md': ''}), [
    'CHANGELOG.md: only group directories and README.md belong in changelog/',
    'core/notes.txt: file name must be unreleased.md or <version>.md, for example 0.1.0.md',
  ]);
});

test('rejects group names with capitals', () => {
  assert.deepEqual(problemsOf({'Web/unreleased.md': ''}), ['Web: group name must be lowercase letters, digits and hyphens']);
});

test('checks the front matter of a release', () => {
  assert.deepEqual(problemsOf({'core/unreleased.md': '', 'core/0.1.0.md': coreRelease('0.2.0', '2026-02-30')}), [
    'core/0.1.0.md: front matter version "0.2.0" differs from the file name 0.1.0',
    'core/0.1.0.md: front matter date "2026-02-30" is not a date in the form YYYY-MM-DD',
  ]);
});

test('requires front matter in a release', () => {
  assert.deepEqual(problemsOf({'core/unreleased.md': '', 'core/0.1.0.md': entry}), ['core/0.1.0.md: release file must start with front matter (version, date)']);
});

test('requires requires-core only for feature modules', () => {
  assert.deepEqual(
    problemsOf({
      'core/unreleased.md': '',
      'core/0.1.0.md': webRelease('0.1.0'),
      'web/unreleased.md': '',
      'web/0.1.0.md': coreRelease('0.1.0'),
    }),
    ['core/0.1.0.md: front matter field "requires-core" is not allowed', 'web/0.1.0.md: front matter requires-core "" must be a core line such as "0.2"'],
  );
});

test('rejects a release without entries', () => {
  assert.deepEqual(problemsOf({'core/unreleased.md': '', 'core/0.1.0.md': '---\nversion: 0.1.0\ndate: 2026-12-01\n---\n'}), ['core/0.1.0.md: release file has no entries']);
});

test('rejects front matter in unreleased.md', () => {
  assert.deepEqual(problemsOf({'core/unreleased.md': coreRelease('0.1.0')}), ['core/unreleased.md: unreleased.md must not have front matter']);
});

test('checks sections and entries', () => {
  const body = ['Some text', '### Fixed', '', '- Fix (COILIX-00001).', '### Added', '- Feature without task.', '### Fixed', '- Again (COILIX-00002).', '### Other', '### Removed'].join('\n');
  assert.deepEqual(problemsOf({'core/unreleased.md': body}), [
    'core/unreleased.md: line 1: expected "### <section>", "- <entry>" or an indented continuation',
    'core/unreleased.md: line 5: section "Added" is repeated or out of order, the order is Added, Changed, Deprecated, Removed, Fixed, Security',
    'core/unreleased.md: line 7: section "Fixed" is repeated or out of order, the order is Added, Changed, Deprecated, Removed, Fixed, Security',
    'core/unreleased.md: line 9: unknown section "Other", allowed: Added, Changed, Deprecated, Removed, Fixed, Security',
    'core/unreleased.md: line 10: section "Removed" is repeated or out of order, the order is Added, Changed, Deprecated, Removed, Fixed, Security',
    'core/unreleased.md: line 9: section "Other" has no entries',
    'core/unreleased.md: line 10: section "Removed" has no entries',
    'core/unreleased.md: line 6: entry has no task identifier (COILIX-NNNNN)',
  ]);
});

test('reports line numbers after the front matter', () => {
  const release = '---\nversion: 0.1.0\ndate: 2026-12-01\n---\n\n### Added\n\n- No task.\n';
  assert.deepEqual(problemsOf({'core/unreleased.md': '', 'core/0.1.0.md': release}), ['core/0.1.0.md: line 8: entry has no task identifier (COILIX-NNNNN)']);
});

test('rejects entries outside a section and loose indented lines', () => {
  assert.deepEqual(problemsOf({'core/unreleased.md': '- Entry (COILIX-00001).\n  continued\n'}), [
    'core/unreleased.md: line 1: entry outside a section',
    'core/unreleased.md: line 2: indented line outside an entry',
  ]);
});

test('orders versions by SemVer', () => {
  const versions = ['0.10.0', '0.2.0', '0.2.0-RC1', '0.2.0-M2', '0.2.1', '1.0.0'];
  assert.deepEqual([...versions].sort(compareVersions), ['0.2.0-M2', '0.2.0-RC1', '0.2.0', '0.2.1', '0.10.0', '1.0.0']);
});

test('returns the body of a release as notes', () => {
  const root = changelog({'core/unreleased.md': '', 'core/0.1.0.md': coreRelease('0.1.0')});
  assert.equal(releaseNotes('core', '0.1.0', root), entry);
});

test('refuses notes for a missing or invalid release', () => {
  const root = changelog({'core/unreleased.md': '', 'core/0.2.0.md': coreRelease('0.3.0')});
  assert.throws(() => releaseNotes('core', '0.1.0', root), {message: 'changelog/core/0.1.0.md does not exist'});
  assert.throws(() => releaseNotes('core', '0.2.0', root), {message: 'changelog/core/0.2.0.md: front matter version "0.3.0" differs from the file name 0.2.0'});
});

test('renders released versions without unreleased changes', () => {
  const root = changelog({
    'intellij/unreleased.md': '',
    'intellij/0.1.0.md': coreRelease('0.1.0'),
    'web/unreleased.md': entry,
    'web/0.1.0.md': webRelease('0.1.0'),
    'core/unreleased.md': entry,
    'core/0.1.0.md': coreRelease('0.1.0'),
    'core/0.10.0.md': coreRelease('0.10.0', '2027-01-10'),
    'data/unreleased.md': entry,
  });
  const release = (heading) => [heading, '', '#### Added', '', '- New feature (COILIX-00001).', ''];
  assert.equal(
    renderChangelog(root),
    [
      '## Core {#core}',
      '',
      ...release('### 0.10.0 (2027-01-10) {#core-0.10.0}'),
      ...release('### 0.1.0 (2026-12-01) {#core-0.1.0}'),
      '## coilix-web {#web}',
      '',
      '### 0.1.0 (2026-12-02) {#web-0.1.0}',
      '',
      'Works with Core 0.1.x.',
      '',
      '#### Added',
      '',
      '- New feature (COILIX-00001).',
      '',
      '## IntelliJ plugin {#intellij}',
      '',
      '### 0.1.0 (2026-12-01) {#intellij-0.1.0}',
      '',
      '#### Added',
      '',
      '- New feature (COILIX-00001).',
      '',
    ].join('\n'),
  );
});

test('says when nothing is released', () => {
  assert.equal(renderChangelog(changelog({'core/unreleased.md': entry})), 'No version has been released yet.\n');
});

test('the changelog of this repository is valid', () => {
  assert.deepEqual(changelogProblems(), []);
});
