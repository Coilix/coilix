import fs from 'node:fs';
import path from 'node:path';

const website = path.resolve(import.meta.dirname, '..');
const allowed = JSON.parse(fs.readFileSync(path.join(import.meta.dirname, 'allowed-licenses.json'), 'utf8'));

function licenseOf(name, entry) {
  if (entry.license) {
    return entry.license;
  }
  const manifest = path.join(website, name, 'package.json');
  if (!fs.existsSync(manifest)) {
    return undefined;
  }
  const declared = JSON.parse(fs.readFileSync(manifest, 'utf8'));
  return declared.license ?? declared.licenses?.map((license) => license.type ?? license).join(' OR ') ?? allowed.packages[packageName(name)];
}

function packageName(name) {
  return name.slice(name.lastIndexOf('node_modules/') + 'node_modules/'.length);
}

function isAllowed(expression) {
  return expression
    .replace(/[()]/g, '')
    .split(/\s+OR\s+/)
    .some((option) => option.split(/\s+AND\s+/).every((license) => allowed.licenses.includes(license.trim())));
}

const lock = JSON.parse(fs.readFileSync(path.join(website, 'package-lock.json'), 'utf8'));
const problems = Object.entries(lock.packages)
  .filter(([name]) => name !== '')
  .map(([name, entry]) => [packageName(name), licenseOf(name, entry)])
  .filter(([, license]) => !license || !isAllowed(license))
  .map(([name, license]) => `${name}: ${license ?? 'no license'}`);

if (problems.length > 0) {
  console.error(`npm packages with a license outside scripts/allowed-licenses.json:\n${[...new Set(problems)].sort().join('\n')}`);
  process.exit(1);
}
console.log(`All ${Object.keys(lock.packages).length - 1} npm packages have an allowed license.`);
