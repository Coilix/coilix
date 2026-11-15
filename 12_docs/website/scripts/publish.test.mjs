import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';
import {missingFiles, publishTarget} from './publish.mjs';

test('publishes the guides without API when started by hand', () => {
  assert.deepEqual(publishTarget('', ['0.1']), {site: true, api: false, group: '', line: ''});
});

test('publishes nothing before the first core line', () => {
  assert.deepEqual(publishTarget('', []), {site: false, api: false, group: '', line: ''});
  assert.deepEqual(publishTarget('core-v0.1.0', []), {site: false, api: false, group: 'core', line: '0.1'});
});

test('adds the API of the tagged line for groups with API documentation', () => {
  assert.deepEqual(publishTarget('core-v0.2.1', ['0.2', '0.1']), {site: true, api: true, group: 'core', line: '0.2'});
  assert.deepEqual(publishTarget('web-v0.1.0', ['0.2']), {site: true, api: false, group: 'web', line: '0.1'});
});

test('skips pre-releases', () => {
  assert.deepEqual(publishTarget('core-v0.2.0-RC1', ['0.1']), {site: false, api: false, group: 'core', line: '0.2'});
});

test('rejects a tag that is not a release tag', () => {
  assert.throws(() => publishTarget('v0.1.0', ['0.1']));
});

test('lists the entry pages missing from the assembled site', () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'coilix-site-'));
  ['CNAME', 'index.html', 'pl/index.html', 'changelog.html', 'api/core/0.2/index.html'].forEach((file) => {
    fs.mkdirSync(path.dirname(path.join(directory, file)), {recursive: true});
    fs.writeFileSync(path.join(directory, file), '');
  });
  assert.deepEqual(missingFiles(directory, ['0.2', '0.1']), ['pl/changelog.html', 'api/core/0.1/index.html']);
  fs.rmSync(directory, {recursive: true});
});
