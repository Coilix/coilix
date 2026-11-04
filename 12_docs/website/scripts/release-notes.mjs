import {releaseNotes} from './changelog.mjs';

const [group, version] = process.argv.slice(2);

if (!group || !version) {
  console.error('Usage: node release-notes.mjs <group> <version>');
  process.exit(2);
}

try {
  process.stdout.write(releaseNotes(group, version));
} catch (error) {
  console.error(error.message.split('\n').map((line) => `::error::${line}`).join('\n'));
  process.exit(1);
}
