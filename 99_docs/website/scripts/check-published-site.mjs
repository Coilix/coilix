import {docsLines} from './docs-line.mjs';
import {fail} from './release.mjs';
import {missingFiles} from './publish.mjs';

const directory = process.argv[2];

if (!directory) {
  console.error('Usage: node scripts/check-published-site.mjs <site directory>');
  process.exit(2);
}

const missing = missingFiles(directory, docsLines('core'));

if (missing.length > 0) {
  fail(missing.map((file) => `the published site has no ${file}`));
}
console.log('The published site has all entry pages.');
