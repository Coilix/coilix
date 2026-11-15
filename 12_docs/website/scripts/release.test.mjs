import assert from 'node:assert/strict';
import {test} from 'node:test';
import {
  coreLineProblems,
  isPrerelease,
  lineOf,
  parseTag,
  releaseBranches,
  releaseFile,
  releaseTitle,
  unreleasedEntriesProblems,
  versionProblems,
} from './release.mjs';

test('reads the group and version from a tag', () => {
  assert.deepEqual(parseTag('core-v0.1.0'), {group: 'core', version: '0.1.0'});
  assert.deepEqual(parseTag('security-web-v1.2.3-RC1'), {group: 'security-web', version: '1.2.3-RC1'});
});

test('rejects tags without a group', () => {
  assert.throws(() => parseTag('v0.1.0'), {message: 'Tag v0.1.0 is not in the form <group>-vX.Y.Z, for example core-v0.1.0'});
  assert.throws(() => parseTag('core-v0.1'));
});

test('names lines, branches and titles', () => {
  assert.equal(lineOf('0.10.2-RC1'), '0.10');
  assert.deepEqual(releaseBranches('web', '0.3.1'), ['master', 'series/web-0.3']);
  assert.equal(releaseTitle('core', '0.1.0'), 'Coilix Core 0.1.0');
  assert.equal(releaseTitle('web', '0.1.0'), 'Coilix coilix-web 0.1.0');
  assert.equal(isPrerelease('0.1.0-M1'), true);
  assert.equal(isPrerelease('0.1.0'), false);
});

test('accepts a version newer than every tag on master', () => {
  assert.deepEqual(versionProblems('core', '0.2.0', 'master', ['0.1.0', '0.1.1', '0.2.0']), []);
});

test('rejects a version that is not newer on master', () => {
  assert.deepEqual(versionProblems('core', '0.1.2', 'master', ['0.1.0', '0.2.0']), ['core 0.1.2 must be newer than core 0.2.0']);
  assert.deepEqual(versionProblems('core', '0.2.0-RC1', 'master', ['0.2.0']), ['core 0.2.0-RC1 must be newer than core 0.2.0']);
});

test('compares a maintenance release only with its own line', () => {
  assert.deepEqual(versionProblems('core', '0.1.2', 'series/core-0.1', ['0.1.0', '0.1.1', '0.2.0']), []);
  assert.deepEqual(versionProblems('core', '0.1.1', 'series/core-0.1', ['0.1.1', '0.1.3']), ['core 0.1.1 must be newer than core 0.1.3']);
});

test('rejects a release from another branch', () => {
  assert.deepEqual(versionProblems('web', '0.2.1', 'feature/x', []), ['web 0.2.1 is released from master or series/web-0.2, not from feature/x']);
  assert.deepEqual(versionProblems('web', '0.2.1', 'series/core-0.2', []), ['web 0.2.1 is released from master or series/web-0.2, not from series/core-0.2']);
});

test('rejects a malformed version', () => {
  assert.deepEqual(versionProblems('core', '1.0', 'master', []), ['version 1.0 is not in the form X.Y.Z']);
});

test('turns unreleased entries into a release file', () => {
  const entries = '### Added\n\n- Feature (COILIX-00001).\n';
  assert.equal(releaseFile(entries, {version: '0.1.0', date: '2026-12-01'}), `---\nversion: 0.1.0\ndate: 2026-12-01\n---\n\n${entries}`);
  assert.equal(
    releaseFile(`\n${entries}\n`, {version: '0.2.0', date: '2026-12-02', requiresCore: '0.1'}),
    `---\nversion: 0.2.0\ndate: 2026-12-02\nrequires-core: "0.1"\n---\n\n${entries}`,
  );
});

test('refuses to release without entries', () => {
  assert.deepEqual(unreleasedEntriesProblems(''), ['unreleased.md has no entries, so there is nothing to release']);
  assert.deepEqual(unreleasedEntriesProblems('### Added\n\n- No task.\n'), ['unreleased.md: line 3: entry has no task identifier (COILIX-NNNNN)']);
});

test('checks the core line of a feature module', () => {
  assert.deepEqual(coreLineProblems('core', undefined, undefined), []);
  assert.deepEqual(coreLineProblems('intellij', undefined, undefined), []);
  assert.deepEqual(coreLineProblems('web', '0.2', '0.2.3'), []);
  assert.deepEqual(coreLineProblems('web', '0.1', '0.2.0'), ['requires-core is "0.1", but the last core release is 0.2.0']);
  assert.deepEqual(coreLineProblems('web', '0.1', undefined), ['there is no core-v* tag before this commit, release core first']);
});
