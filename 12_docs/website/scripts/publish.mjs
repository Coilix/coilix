import fs from 'node:fs';
import path from 'node:path';
import {isPrerelease, lineOf, parseTag} from './release.mjs';

const apiGroups = ['core'];

export const domain = 'docs.coilix.dev';

export function publishTarget(tag, coreLines) {
  const released = coreLines.length > 0;
  if (!tag) {
    return {site: released, api: false, group: '', line: ''};
  }
  const {group, version} = parseTag(tag);
  const site = released && !isPrerelease(version);
  return {site, api: site && apiGroups.includes(group), group, line: lineOf(version)};
}

export function requiredFiles(coreLines) {
  return [
    'CNAME',
    'index.html',
    'pl/index.html',
    'changelog.html',
    'pl/changelog.html',
    ...coreLines.map((line) => `api/core/${line}/index.html`),
  ];
}

export function missingFiles(directory, coreLines) {
  return requiredFiles(coreLines).filter((file) => !fs.existsSync(path.join(directory, file)));
}
