import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';

const website = path.resolve(import.meta.dirname, '..');
const translations = path.join(website, 'i18n', 'pl', 'docusaurus-plugin-content-docs');
const sections = ['Getting started', 'Guides', 'Reference', 'Migration'];
const requiredFields = ['id', 'title', 'description', 'sidebar_position'];
const inlineLanguages = ['scala', 'yaml', 'yml', 'hocon', 'conf', 'properties'];
const buildLine = /^(libraryDependencies|addCompilerPlugin|addSbtPlugin|scalaVersion)\b/;

function releasedLines() {
  const file = path.join(website, 'versions.json');
  return fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : [];
}

function pagesIn(directory) {
  if (!fs.existsSync(directory)) {
    return [];
  }
  return fs
    .readdirSync(directory, {recursive: true})
    .map((file) => file.split(path.sep).join('/'))
    .filter((file) => /\.mdx?$/.test(file))
    .sort();
}

function frontMatter(source) {
  const match = source.match(/^---\r?\n([\s\S]*?)\r?\n---/);
  if (!match) {
    return {};
  }
  const fields = match[1].split(/\r?\n/).map((line) => line.match(/^([\w-]+):\s*(.*)$/));
  return Object.fromEntries(fields.filter(Boolean).map(([, key, value]) => [key, value.trim()]));
}

function docId(page, fields) {
  const directory = path.posix.dirname(page);
  return directory === '.' ? fields.id : `${directory}/${fields.id}`;
}

function fenceProblems(page, source) {
  const fences = [...source.matchAll(/^```(\w+)[^\n]*\n([\s\S]*?)^```/gm)];
  return fences
    .filter(([, language]) => inlineLanguages.includes(language))
    .filter(([, , body]) => !body.trim().split('\n').every((line) => buildLine.test(line.trim())))
    .map(([, language]) => `${page}: ${language} code must come from a file in examples/, not from an inline block`);
}

function pageProblems(root, page) {
  const source = fs.readFileSync(path.join(root, page), 'utf8');
  const fields = frontMatter(source);
  const missing = requiredFields.filter((field) => !(field in fields)).map((field) => `${page}: missing front matter field ${field}`);
  const name = path.posix.basename(page).replace(/\.mdx?$/, '');
  const idProblem = fields.id && fields.id !== name ? [`${page}: id ${fields.id} differs from the file name ${name}`] : [];
  return [...missing, ...idProblem, ...fenceProblems(page, source)];
}

function sidebarItems(items) {
  return items.flatMap((item) => {
    if (typeof item === 'string') {
      return [{id: item}];
    }
    if (item.type === 'doc') {
      return [{id: item.id}];
    }
    if (item.type === 'category') {
      return [{category: item.label}, ...sidebarItems(item.items ?? [])];
    }
    return [];
  });
}

async function sidebarOf(line) {
  if (line === 'current') {
    return (await import(pathToFileURL(path.join(website, 'sidebars.ts')).href)).default;
  }
  return JSON.parse(fs.readFileSync(path.join(website, 'versioned_sidebars', `version-${line}-sidebars.json`), 'utf8'));
}

function sectionProblems(label, sidebar) {
  const categories = Object.values(sidebar).flatMap((items) => items.map((item) => item.label));
  const unknown = categories.filter((category) => !sections.includes(category));
  const ordered = categories.filter((category) => sections.includes(category));
  const expected = sections.filter((section) => ordered.includes(section));
  const unknownProblems = unknown.map((category) => `${label}: top-level sidebar entry "${category}" is not one of the sections`);
  const orderProblem = ordered.join() === expected.join() ? [] : [`${label}: sidebar sections must follow the order ${sections.join(', ')}`];
  return [...unknownProblems, ...orderProblem];
}

async function lineProblems(line) {
  const english = line === 'current' ? path.join(website, 'docs') : path.join(website, 'versioned_docs', `version-${line}`);
  const polish = path.join(translations, line === 'current' ? 'current' : `version-${line}`);
  const label = line === 'current' ? 'next' : line;
  const englishPages = pagesIn(english);
  const polishPages = pagesIn(polish);
  const untranslated = englishPages.filter((page) => !polishPages.includes(page)).map((page) => `${label}: ${page} has no Polish translation`);
  const orphaned = polishPages.filter((page) => !englishPages.includes(page)).map((page) => `${label}: Polish ${page} has no English page`);
  const sidebar = await sidebarOf(line);
  const listed = Object.values(sidebar).flatMap(sidebarItems).flatMap((item) => (item.id ? [item.id] : []));
  const unlisted = englishPages
    .filter((page) => !listed.includes(docId(page, frontMatter(fs.readFileSync(path.join(english, page), 'utf8')))))
    .map((page) => `${label}: ${page} is not in the sidebar`);
  const mismatched = englishPages.filter((page) => polishPages.includes(page)).flatMap((page) => translationProblems(label, english, polish, page));
  return [
    ...untranslated,
    ...orphaned,
    ...unlisted,
    ...mismatched,
    ...sectionProblems(label, sidebar),
    ...englishPages.map((page) => pageProblems(english, page)).flat().map((problem) => `${label}: ${problem}`),
    ...polishPages.map((page) => pageProblems(polish, page)).flat().map((problem) => `${label} (pl): ${problem}`),
  ];
}

function translationProblems(label, english, polish, page) {
  const source = frontMatter(fs.readFileSync(path.join(english, page), 'utf8'));
  const translation = frontMatter(fs.readFileSync(path.join(polish, page), 'utf8'));
  return ['id', 'sidebar_position']
    .filter((field) => source[field] !== translation[field])
    .map((field) => `${label}: ${page} has a different ${field} in the Polish translation`);
}

const problems = (await Promise.all(['current', ...releasedLines()].map(lineProblems))).flat();

if (problems.length > 0) {
  console.error(problems.join('\n'));
  process.exit(1);
}
console.log('Documentation pages are complete in both languages.');
