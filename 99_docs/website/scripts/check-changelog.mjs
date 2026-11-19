import {changelogProblems} from './changelog.mjs';

const problems = changelogProblems();

if (problems.length > 0) {
  console.error(problems.map((problem) => `changelog/${problem}`).join('\n'));
  process.exit(1);
}
console.log('Changelog files are valid.');
