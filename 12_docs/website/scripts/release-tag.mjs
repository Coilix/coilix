import fs from 'node:fs';
import {releaseNotes} from './changelog.mjs';
import {
  coreLineProblems,
  fail,
  groupExists,
  groupVersions,
  isPrerelease,
  lastCoreVersion,
  parseTag,
  releaseBranches,
  releaseFields,
  releaseTitle,
  tryGit,
  versionProblems,
} from './release.mjs';

const [tag, notesFile] = process.argv.slice(2);

if (!tag || !notesFile) {
  console.error('Usage: node release-tag.mjs <group>-vX.Y.Z <release notes file>');
  process.exit(2);
}

function parsed() {
  try {
    return parseTag(tag);
  } catch (error) {
    return fail([error.message]);
  }
}

function notes(group, version) {
  try {
    return {text: releaseNotes(group, version), problems: []};
  } catch (error) {
    return {text: '', problems: error.message.split('\n')};
  }
}

const {group, version} = parsed();

if (!groupExists(group)) {
  fail([`changelog/${group}/unreleased.md does not exist, so ${group} is not a release group`]);
}

const containing = (tryGit('branch', '--remotes', '--contains', 'HEAD', '--format=%(refname:short)') ?? '')
  .split('\n')
  .map((branch) => branch.replace(/^origin\//, ''))
  .filter((branch) => branch.length > 0);
const branch = releaseBranches(group, version).find((candidate) => containing.includes(candidate)) ?? containing[0] ?? 'a commit on no branch';
const release = notes(group, version);
const problems = [
  ...versionProblems(group, version, branch, groupVersions(group)),
  ...release.problems,
  ...coreLineProblems(group, releaseFields(group, version)['requires-core'], lastCoreVersion()),
];

if (problems.length > 0) {
  fail(problems);
}

fs.writeFileSync(notesFile, release.text);
console.log([`group=${group}`, `version=${version}`, `title=${releaseTitle(group, version)}`, `prerelease=${isPrerelease(version)}`].join('\n'));
