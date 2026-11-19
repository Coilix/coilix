import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docs: [
    {
      type: 'category',
      label: 'Getting started',
      collapsed: false,
      items: ['getting-started/first-application'],
    },
    {
      type: 'category',
      label: 'Guides',
      collapsed: false,
      items: ['guides/bean-lifecycle', 'guides/value-types', 'guides/profiles-and-conditions', 'guides/testing'],
    },
  ],
};

export default sidebars;
