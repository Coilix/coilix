import React from 'react';
import Link from '@docusaurus/Link';
import Translate, {translate} from '@docusaurus/Translate';
import Layout from '@theme/Layout';
import styles from './index.module.css';

export default function Home(): React.ReactNode {
  return (
    <Layout
      title={translate({id: 'home.title', message: 'Dependency injection for Scala 3'})}
      description={translate({
        id: 'home.description',
        message: 'Coilix is an IoC container for Scala 3 with annotations, configuration files and application startup.',
      })}>
      <main className={styles.hero}>
        <h1 className={styles.title}>Coilix</h1>
        <p className={styles.tagline}>
          <Translate id="home.tagline">
            An IoC container for Scala 3 with constructor injection, configuration files and application startup.
          </Translate>
        </p>
        <div className={styles.actions}>
          <Link className="button button--primary button--lg" to="/docs/getting-started/first-application">
            <Translate id="home.start">Get started</Translate>
          </Link>
          <Link className="button button--secondary button--lg" href="pathname:///api/next/index.html">
            <Translate id="home.api">API reference</Translate>
          </Link>
        </div>
      </main>
    </Layout>
  );
}
