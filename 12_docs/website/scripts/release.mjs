import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import {changelogRoot, compareVersions, groupLabel, parseBody, releaseProblems} from './changelog.mjs';

const tagPattern = /^([a-z][a-z0-9-]*)-v(\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?)$/;
const versionPattern = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/;
const groupsWithoutCore = ['core', 'intellij'];

export function parseTag(tag) {
  const match = tag.match(tagPattern);
  if (!match) {
    throw new Error(`Tag ${tag} is not in the form <group>-vX.Y.Z, for example core-v0.1.0`);
  }
  return {group: match[1], version: match[2]};
}

export function lineOf(version) {
  return version.split('.').slice(0, 2).join('.');
}

export function releaseBranches(group, version) {
  return ['master', `series/${group}-${lineOf(version)}`];
}

export function needsCore(group) {
  return !groupsWithoutCore.includes(group);
}

export function releaseTitle(group, version) {
  return `Coilix ${groupLabel(group)} ${version}`;
}

export function isPrerelease(version) {
  return version.includes('-');
}

export function versionProblems(group, version, branch, existing) {
  const others = existing.filter((other) => other !== version);
  const sameLine = others.filter((other) => lineOf(other) === lineOf(version));
  const compared = branch === 'master' ? others : sameLine;
  const newest = [...compared].sort(compareVersions).pop();
  const format = versionPattern.test(version) ? [] : [`version ${version} is not in the form X.Y.Z`];
  const branchProblem = releaseBranches(group, version).includes(branch)
    ? []
    : [`${group} ${version} is released from ${releaseBranches(group, version).join(' or ')}, not from ${branch}`];
  const order = newest && compareVersions(version, newest) <= 0 ? [`${group} ${version} must be newer than ${group} ${newest}`] : [];
  return [...format, ...branchProblem, ...order];
}

export function releaseFile(unreleased, fields) {
  const header = ['---', `version: ${fields.version}`, `date: ${fields.date}`, ...(fields.requiresCore ? [`requires-core: "${fields.requiresCore}"`] : []), '---', ''];
  return `${header.join('\n')}\n${unreleased.trim()}\n`;
}

export function unreleasedEntriesProblems(source) {
  const parsed = parseBody(source);
  const empty = parsed.sections.length === 0 ? ['unreleased.md has no entries, so there is nothing to release'] : [];
  return [...empty, ...parsed.problems.map((problem) => `unreleased.md: ${problem}`)];
}

export function coreLineProblems(group, requiresCore, coreVersion) {
  if (!needsCore(group)) {
    return [];
  }
  if (!coreVersion) {
    return ['there is no core-v* tag before this commit, release core first'];
  }
  return requiresCore === lineOf(coreVersion) ? [] : [`requires-core is "${requiresCore}", but the last core release is ${coreVersion}`];
}

export function git(...args) {
  return execFileSync('git', args, {cwd: changelogRoot, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe']}).trim();
}

export function tryGit(...args) {
  try {
    return git(...args);
  } catch {
    return undefined;
  }
}

export function groupVersions(group) {
  return (tryGit('tag', '--list', `${group}-v*`) ?? '')
    .split('\n')
    .filter((tag) => tag.length > 0)
    .flatMap((tag) => (tag.match(tagPattern)?.[1] === group ? [tag.match(tagPattern)[2]] : []));
}

export function lastCoreVersion() {
  return tryGit('describe', '--tags', '--abbrev=0', '--match', 'core-v[0-9]*', 'HEAD')?.replace(/^core-v/, '');
}

export function groupExists(group) {
  return fs.existsSync(path.join(changelogRoot, group, 'unreleased.md'));
}

export function releaseFields(group, version) {
  const file = path.join(changelogRoot, group, `${version}.md`);
  return fs.existsSync(file) ? releaseProblems(group, version, fs.readFileSync(file, 'utf8')).fields : {};
}

export function fail(problems) {
  console.error(problems.map((problem) => `::error::${problem}`).join('\n'));
  process.exit(1);
}
