import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';

import styles from './index.module.css';

function wrapPulumiWithAnchorTag(text) {
  const parts = text.split('Pulumi');

  // If "Pulumi" is not in the text, just return the text as is
  if (parts.length === 1) {
    return <>{text}</>;
  }

  return (
    <>
      {parts[0]}
      <a href="https://www.pulumi.com/">Pulumi</a>
      {parts[1]}
    </>
  );
}

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext();
  const BesomLogoSvg = require('@site/static/img/Besom_logo_full_color.svg').default
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <BesomLogoSvg className={styles.besomLogoSvg} role="img" />
        <p className={styles.tagline}>{wrapPulumiWithAnchorTag(siteConfig.tagline)}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro">
            Get started!
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home() {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout
      description="Besom - Scala SDK for Pulumi">
      <HomepageHeader />
      <main>
        <br></br>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}
