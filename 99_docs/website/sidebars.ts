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
      items: ['guides/bean-lifecycle', 'guides/bean-scopes', 'guides/choosing-beans', 'guides/value-types', 'guides/profiles-and-conditions', 'guides/testing', 'guides/generic-types', 'guides/tasks-and-scheduling', 'guides/replicas', 'guides/application-events', 'guides/fatal-errors', 'guides/cats-effect', 'guides/pekko', 'guides/background-processors'],
    },
  ],
};

export default sidebars;
