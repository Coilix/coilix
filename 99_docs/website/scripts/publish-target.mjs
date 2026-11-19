import {docsLines} from './docs-line.mjs';
import {fail} from './release.mjs';
import {publishTarget} from './publish.mjs';

const tag = process.argv[2] ?? '';

try {
  const target = publishTarget(tag, docsLines('core'));
  if (!target.site) {
    console.error(`::notice::Nothing to publish for ${tag || 'master'}: core has no released documentation line yet, or the tag is a pre-release`);
  }
  console.log(Object.entries(target).map(([key, value]) => `${key}=${value}`).join('\n'));
} catch (error) {
  fail([error.message]);
}
