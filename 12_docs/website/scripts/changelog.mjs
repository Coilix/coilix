import fs from 'node:fs';
import path from 'node:path';

export const changelogRoot = path.resolve(import.meta.dirname, '..', '..', '..', 'changelog');

export const sections = ['Added', 'Changed', 'Deprecated', 'Removed', 'Fixed', 'Security'];

const groupsWithoutCore = ['core', 'intellij'];
const groupLabels = {core: 'Core', intellij: 'IntelliJ plugin'};
const groupPattern = /^[a-z][a-z0-9-]*$/;
const versionPattern = /^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$/;
const corePattern = /^\d+\.\d+$/;
const datePattern = /^\d{4}-\d{2}-\d{2}$/;
const taskPattern = /COILIX-\d{5}/;
const unreleased = 'unreleased.md';

function splitFrontMatter(source) {
  const match = source.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?/);
  if (!match) {
    return {fields: undefined, body: source, bodyLine: 1};
  }
  const entries = match[1].split(/\r?\n/).map((line) => line.match(/^([\w-]+):\s*(.*)$/) ?? [line]);
  const fields = Object.fromEntries(entries.map(([line, key, value]) => (key ? [key, unquote(value.trim())] : [line, undefined])));
  return {fields, body: source.slice(match[0].length), bodyLine: match[0].split('\n').length};
}

function unquote(value) {
  return value.replace(/^"(.*)"$/, '$1').replace(/^'(.*)'$/, '$1');
}

function startSection(state, name, number) {
  const index = sections.indexOf(name);
  const known = index >= 0 ? [] : [`line ${number}: unknown section "${name}", allowed: ${sections.join(', ')}`];
  const ordered = index < 0 || index > state.lastIndex ? [] : [`line ${number}: section "${name}" is repeated or out of order, the order is ${sections.join(', ')}`];
  return {
    ...state,
    lastIndex: Math.max(index, state.lastIndex),
    sections: [...state.sections, {name, line: number, entries: []}],
    problems: [...state.problems, ...known, ...ordered],
  };
}

function updateLastSection(state, update) {
  const last = state.sections[state.sections.length - 1];
  return {...state, sections: [...state.sections.slice(0, -1), update(last)]};
}

function startEntry(state, text, number) {
  if (state.sections.length === 0) {
    return {...state, problems: [...state.problems, `line ${number}: entry outside a section`]};
  }
  return updateLastSection(state, (section) => ({...section, entries: [...section.entries, {line: number, text}]}));
}

function continueEntry(state, text, number) {
  const last = state.sections[state.sections.length - 1];
  if (!last || last.entries.length === 0) {
    return {...state, problems: [...state.problems, `line ${number}: indented line outside an entry`]};
  }
  return updateLastSection(state, (section) => {
    const entry = section.entries[section.entries.length - 1];
    return {...section, entries: [...section.entries.slice(0, -1), {...entry, text: `${entry.text} ${text}`}]};
  });
}

function readLine(state, line, number) {
  const heading = line.match(/^### (.+)$/);
  if (heading) {
    return startSection(state, heading[1].trim(), number);
  }
  if (line.startsWith('- ')) {
    return startEntry(state, line.slice(2), number);
  }
  if (/^ {2,}\S/.test(line)) {
    return continueEntry(state, line.trim(), number);
  }
  if (line.trim() === '') {
    return state;
  }
  return {...state, problems: [...state.problems, `line ${number}: expected "### <section>", "- <entry>" or an indented continuation`]};
}

function bodyProblems(parsed) {
  const empty = parsed.sections.filter((section) => section.entries.length === 0).map((section) => `line ${section.line}: section "${section.name}" has no entries`);
  const untracked = parsed.sections
    .flatMap((section) => section.entries)
    .filter((entry) => !taskPattern.test(entry.text))
    .map((entry) => `line ${entry.line}: entry has no task identifier (COILIX-NNNNN)`);
  return [...parsed.problems, ...empty, ...untracked];
}

export function parseBody(body, firstLine = 1) {
  const initial = {sections: [], lastIndex: -1, problems: []};
  const parsed = body.split(/\r?\n/).reduce((state, line, index) => readLine(state, line, firstLine + index), initial);
  return {sections: parsed.sections, problems: bodyProblems(parsed)};
}

function isRealDate(value) {
  const date = new Date(`${value}T00:00:00Z`);
  return datePattern.test(value) && !Number.isNaN(date.getTime()) && date.toISOString().startsWith(value);
}

function fieldProblems(group, version, fields) {
  const needsCore = !groupsWithoutCore.includes(group);
  const allowed = needsCore ? ['version', 'date', 'requires-core'] : ['version', 'date'];
  const unknown = Object.keys(fields).filter((key) => !allowed.includes(key)).map((key) => `front matter field "${key}" is not allowed`);
  const versionProblem = fields.version === version ? [] : [`front matter version "${fields.version ?? ''}" differs from the file name ${version}`];
  const dateProblem = isRealDate(fields.date ?? '') ? [] : [`front matter date "${fields.date ?? ''}" is not a date in the form YYYY-MM-DD`];
  const coreProblem = needsCore && !corePattern.test(fields['requires-core'] ?? '') ? [`front matter requires-core "${fields['requires-core'] ?? ''}" must be a core line such as "0.2"`] : [];
  return [...unknown, ...versionProblem, ...dateProblem, ...coreProblem];
}

export function releaseProblems(group, version, source) {
  const {fields, body, bodyLine} = splitFrontMatter(source);
  if (!fields) {
    return {sections: [], fields: {}, body, problems: ['release file must start with front matter (version, date)']};
  }
  const parsed = parseBody(body, bodyLine);
  const emptyProblem = parsed.sections.length === 0 ? ['release file has no entries'] : [];
  return {sections: parsed.sections, fields, body, problems: [...fieldProblems(group, version, fields), ...emptyProblem, ...parsed.problems]};
}

export function unreleasedProblems(source) {
  const {fields, body} = splitFrontMatter(source);
  const frontMatterProblem = fields ? ['unreleased.md must not have front matter'] : [];
  return [...frontMatterProblem, ...parseBody(body).problems];
}

export function compareVersions(left, right) {
  const [, ...a] = left.match(versionPattern);
  const [, ...b] = right.match(versionPattern);
  const numbers = [0, 1, 2].map((index) => Number(a[index]) - Number(b[index])).find((difference) => difference !== 0);
  if (numbers !== undefined) {
    return numbers;
  }
  if (a[3] === b[3]) {
    return 0;
  }
  if (a[3] === undefined || b[3] === undefined) {
    return a[3] === undefined ? 1 : -1;
  }
  return a[3].localeCompare(b[3], 'en', {numeric: true});
}

function read(root, ...parts) {
  return fs.readFileSync(path.join(root, ...parts), 'utf8');
}

function groupsIn(root) {
  return fs
    .readdirSync(root, {withFileTypes: true})
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort();
}

function isReleaseFile(file) {
  return file.endsWith('.md') && versionPattern.test(file.slice(0, -'.md'.length));
}

function releasesIn(root, group) {
  return fs
    .readdirSync(path.join(root, group))
    .filter(isReleaseFile)
    .map((file) => file.slice(0, -'.md'.length))
    .sort(compareVersions)
    .reverse();
}

function groupProblems(root, group) {
  const files = fs.readdirSync(path.join(root, group));
  const nameProblem = groupPattern.test(group) ? [] : [`${group}: group name must be lowercase letters, digits and hyphens`];
  const missing = files.includes(unreleased) ? [] : [`${group}: missing ${unreleased}`];
  const misnamed = files
    .filter((file) => file !== unreleased && !isReleaseFile(file))
    .map((file) => `${group}/${file}: file name must be ${unreleased} or <version>.md, for example 0.1.0.md`);
  const draft = files.includes(unreleased) ? unreleasedProblems(read(root, group, unreleased)).map((problem) => `${group}/${unreleased}: ${problem}`) : [];
  const released = releasesIn(root, group).flatMap((version) =>
    releaseProblems(group, version, read(root, group, `${version}.md`)).problems.map((problem) => `${group}/${version}.md: ${problem}`),
  );
  return [...nameProblem, ...missing, ...misnamed, ...draft, ...released];
}

export function changelogProblems(root = changelogRoot) {
  const stray = fs
    .readdirSync(root, {withFileTypes: true})
    .filter((entry) => !entry.isDirectory() && entry.name !== 'README.md')
    .map((entry) => `${entry.name}: only group directories and README.md belong in changelog/`);
  return [...stray, ...groupsIn(root).flatMap((group) => groupProblems(root, group))];
}

export function releaseNotes(group, version, root = changelogRoot) {
  const file = path.join(root, group, `${version}.md`);
  if (!fs.existsSync(file)) {
    throw new Error(`changelog/${group}/${version}.md does not exist`);
  }
  const release = releaseProblems(group, version, fs.readFileSync(file, 'utf8'));
  if (release.problems.length > 0) {
    throw new Error(release.problems.map((problem) => `changelog/${group}/${version}.md: ${problem}`).join('\n'));
  }
  return `${release.body.trim()}\n`;
}

function groupLabel(group) {
  return groupLabels[group] ?? `coilix-${group}`;
}

function groupOrder(left, right) {
  const rank = (group) => (group === 'core' ? 0 : group === 'intellij' ? 2 : 1);
  return rank(left) - rank(right) || left.localeCompare(right);
}

function renderRelease(root, group, version) {
  const release = releaseProblems(group, version, read(root, group, `${version}.md`));
  const core = release.fields['requires-core'] ? [`Works with Core ${release.fields['requires-core']}.x.`, ''] : [];
  return [`### ${version} (${release.fields.date}) {#${group}-${version}}`, '', ...core, release.body.trim().replace(/^### /gm, '#### '), ''];
}

export function renderChangelog(root = changelogRoot) {
  const groups = groupsIn(root)
    .filter((group) => releasesIn(root, group).length > 0)
    .sort(groupOrder);
  if (groups.length === 0) {
    return 'No version has been released yet.\n';
  }
  const lines = groups.flatMap((group) => [`## ${groupLabel(group)} {#${group}}`, '', ...releasesIn(root, group).flatMap((version) => renderRelease(root, group, version))]);
  return `${lines.join('\n').trim()}\n`;
}
