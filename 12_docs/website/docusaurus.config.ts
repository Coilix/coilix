import fs from 'node:fs';
import path from 'node:path';
import type {Config, Plugin} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import {themes as prismThemes} from 'prism-react-renderer';

const examples = path.resolve(__dirname, '..', 'examples');

function releasedLines(): string[] {
  const file = path.resolve(__dirname, 'versions.json');
  return fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : [];
}

function exampleAliases(): Record<string, string> {
  const versioned = releasedLines().map((line) => [`@examples-${line}`, path.join(examples, line)]);
  return Object.fromEntries([['@examples', path.join(examples, 'next')], ...versioned]);
}

function examplesPlugin(): Plugin {
  return {
    name: 'coilix-examples',
    configureWebpack: () => ({resolve: {alias: exampleAliases()}}),
  };
}

const config: Config = {
  title: 'Coilix',
  tagline: 'Dependency injection for Scala 3',
  favicon: 'img/favicon.svg',
  url: 'https://docs.coilix.dev',
  baseUrl: '/',
  organizationName: 'Coilix',
  projectName: 'coilix',
  trailingSlash: false,

  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  markdown: {
    format: 'detect',
    hooks: {onBrokenMarkdownLinks: 'throw'},
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'pl'],
    localeConfigs: {
      en: {label: 'English', htmlLang: 'en-US'},
      pl: {label: 'Polski', htmlLang: 'pl-PL'},
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/Coilix/coilix/edit/master/12_docs/website/',
          versions: {current: {label: 'Next (unreleased)'}},
        },
        blog: false,
        theme: {customCss: './src/css/custom.css'},
      } satisfies Preset.Options,
    ],
  ],

  plugins: [examplesPlugin],

  themeConfig: {
    navbar: {
      title: 'Coilix',
      logo: {alt: 'Coilix', src: 'img/favicon.svg'},
      items: [
        {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
        {href: 'pathname:///api/next/index.html', position: 'left', label: 'API'},
        {to: '/changelog', position: 'left', label: 'Changelog'},
        {type: 'docsVersionDropdown', position: 'right'},
        {type: 'localeDropdown', position: 'right'},
        {href: 'https://github.com/Coilix/coilix', position: 'right', label: 'GitHub'},
      ],
    },
    footer: {
      style: 'dark',
      copyright: 'Copyright © 2026 Coilix contributors. Licensed under the Apache License 2.0.',
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'scala', 'properties', 'bash'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
