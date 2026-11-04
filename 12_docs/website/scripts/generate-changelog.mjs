import fs from 'node:fs';
import path from 'node:path';
import {renderChangelog} from './changelog.mjs';

const target = path.resolve(import.meta.dirname, '..', 'src', 'generated', 'changelog.md');

fs.mkdirSync(path.dirname(target), {recursive: true});
fs.writeFileSync(target, renderChangelog());
