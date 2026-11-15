import assert from 'node:assert/strict';
import {test} from 'node:test';
import {freezeLinks, hasDocsSection, needsDocsLine} from './docs-line.mjs';

test('points a frozen page at the examples and API of its line', () => {
  const source = [
    "import Code from '!!raw-loader!@examples/scala/FirstApplication.scala';",
    '[ApplicationContext](pathname:///api/core/next/dev/coilix/context/ApplicationContext.html)',
  ].join('\n');
  assert.equal(
    freezeLinks('core', '0.2', source),
    [
      "import Code from '!!raw-loader!@examples-0.2/scala/FirstApplication.scala';",
      '[ApplicationContext](pathname:///api/core/0.2/dev/coilix/context/ApplicationContext.html)',
    ].join('\n'),
  );
});

test('leaves links to other groups and frozen lines alone', () => {
  const source = 'pathname:///api/web/next/index.html and @examples-0.1/scala/A.scala';
  assert.equal(freezeLinks('core', '0.2', source), source);
});

test('creates a documentation line only for the first final release of a line with guides', () => {
  assert.equal(hasDocsSection('core'), true);
  assert.equal(hasDocsSection('web'), false);
  assert.equal(needsDocsLine('core', '0.1.0'), true);
  assert.equal(needsDocsLine('core', '0.1.0-RC1'), false);
  assert.equal(needsDocsLine('web', '0.1.0'), false);
});
