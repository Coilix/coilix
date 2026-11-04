import fs from 'node:fs';
import path from 'node:path';

const website = path.resolve(import.meta.dirname, '..');
const target = path.join(website, 'src', 'generated', 'CHANGELOG.md');

fs.mkdirSync(path.dirname(target), {recursive: true});
fs.copyFileSync(path.resolve(website, '..', '..', 'CHANGELOG.md'), target);
