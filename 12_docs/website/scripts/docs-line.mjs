import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import {isPrerelease, lineOf} from './release.mjs';

export const website = path.resolve(import.meta.dirname, '..');

const examples = path.resolve(website, '..', 'examples');
const docusaurus = path.join(website, 'node_modules', '@docusaurus', 'core', 'bin', 'docusaurus.mjs');

export const docsSections = {
  core: {
    pluginId: 'default',
    versions: 'versions.json',
    versionedDocs: 'versioned_docs',
    versionedSidebars: 'versioned_sidebars',
    polish: path.join('i18n', 'pl', 'docusaurus-plugin-content-docs'),
  },
};

export function hasDocsSection(group) {
  return Object.hasOwn(docsSections, group);
}

export function docsLines(group) {
  const file = path.join(website, docsSections[group].versions);
  return fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : [];
}

export function freezeLinks(group, line, source) {
  return source.replaceAll('@examples/', `@examples-${line}/`).replaceAll(`/api/${group}/next/`, `/api/${group}/${line}/`);
}

function pagesIn(directory) {
  return fs
    .readdirSync(directory, {recursive: true})
    .filter((file) => /\.mdx?$/.test(file))
    .map((file) => path.join(directory, file));
}

function versionCommand(section) {
  return section.pluginId === 'default' ? 'docs:version' : `docs:version:${section.pluginId}`;
}

export function docusaurusInstalled() {
  return fs.existsSync(docusaurus);
}

export function needsDocsLine(group, version) {
  return hasDocsSection(group) && !isPrerelease(version) && !docsLines(group).includes(lineOf(version));
}

export function createDocsLine(group, line) {
  const section = docsSections[group];
  execFileSync(process.execPath, [docusaurus, versionCommand(section), line], {cwd: website, stdio: 'inherit'});
  const englishPages = path.join(website, section.versionedDocs, `version-${line}`);
  const polishPages = path.join(website, section.polish, `version-${line}`);
  const frozenExamples = path.join(examples, line);
  fs.cpSync(path.join(examples, 'next'), frozenExamples, {recursive: true});
  [englishPages, polishPages].filter((directory) => fs.existsSync(directory)).flatMap(pagesIn).forEach((page) => fs.writeFileSync(page, freezeLinks(group, line, fs.readFileSync(page, 'utf8'))));
  return [
    englishPages,
    polishPages,
    path.join(website, section.polish, `version-${line}.json`),
    path.join(website, section.versionedSidebars, `version-${line}-sidebars.json`),
    path.join(website, section.versions),
    frozenExamples,
  ].filter((file) => fs.existsSync(file));
}
