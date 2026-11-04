import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import {changelogProblems, changelogRoot, groupLabel} from './changelog.mjs';
import {
  fail,
  git,
  groupExists,
  groupVersions,
  lastCoreVersion,
  lineOf,
  needsCore,
  releaseFile,
  tryGit,
  unreleasedEntriesProblems,
  versionProblems,
} from './release.mjs';

const args = process.argv.slice(2);
const push = !args.includes('--no-push');
const [group, version] = args.filter((arg) => !arg.startsWith('--'));

if (!group || !version) {
  console.error('Usage: npm run prepare-release -- <group> <version> [--no-push]');
  process.exit(2);
}

if (!groupExists(group)) {
  fail([`changelog/${group}/unreleased.md does not exist, so ${group} is not a release group`]);
}

const unreleasedFile = path.join(changelogRoot, group, 'unreleased.md');
const releaseFileName = path.join(changelogRoot, group, `${version}.md`);
tryGit('fetch', '--tags', 'origin');

const branch = git('branch', '--show-current');
const coreVersion = lastCoreVersion();

const upstream = tryGit('rev-parse', `origin/${branch}`);
const problems = [
  ...versionProblems(group, version, branch, groupVersions(group)),
  ...unreleasedEntriesProblems(fs.readFileSync(unreleasedFile, 'utf8')),
  ...(git('status', '--porcelain').length > 0 ? ['the working tree has uncommitted changes'] : []),
  ...(upstream === git('rev-parse', 'HEAD') ? [] : [`${branch} is not the same as origin/${branch}, run git pull first`]),
  ...(needsCore(group) && !coreVersion ? ['there is no core-v* tag before this commit, release core first'] : []),
  ...(fs.existsSync(releaseFileName) ? [`changelog/${group}/${version}.md already exists`] : []),
];

if (problems.length > 0) {
  fail(problems);
}

const releaseBranch = `release/${group}-v${version}`;
const title = `COILIX-00000: Release ${groupLabel(group)} ${version}`;
const date = new Date().toISOString().slice(0, 10);
const requiresCore = needsCore(group) ? lineOf(coreVersion) : undefined;

git('switch', '--create', releaseBranch);
git('mv', unreleasedFile, releaseFileName);
fs.writeFileSync(releaseFileName, releaseFile(fs.readFileSync(releaseFileName, 'utf8'), {version, date, requiresCore}));
fs.writeFileSync(unreleasedFile, '');

const changelog = changelogProblems();
if (changelog.length > 0) {
  fail(changelog.map((problem) => `changelog/${problem}`));
}

git('add', releaseFileName, unreleasedFile);
git('commit', '--signoff', '--message', title);
console.log(`Committed ${title} on ${releaseBranch}.`);

if (push) {
  git('push', '--set-upstream', 'origin', releaseBranch);
  const body = [
    `Closes the changelog of ${groupLabel(group)} ${version}: \`changelog/${group}/unreleased.md\` becomes \`changelog/${group}/${version}.md\`.`,
    '',
    `After merging, tag the merge commit: \`git tag -s ${group}-v${version} -m "Coilix ${groupLabel(group)} ${version}"\` and \`git push origin ${group}-v${version}\`.`,
  ].join('\n');
  execFileSync('gh', ['pr', 'create', '--base', branch, '--head', releaseBranch, '--title', title, '--body', body], {stdio: 'inherit'});
} else {
  console.log(`Not pushed. Review the commit, then run: git push --set-upstream origin ${releaseBranch}`);
}
