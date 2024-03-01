import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from './HomepageFeatures';

import { Content } from "@theme/BlogPostPage";
import BlogPostItem from "@theme/BlogPostItem";
import PaginatorNavLink from "@theme/PaginatorNavLink";
import { BlogPostProvider } from "@docusaurus/theme-common/internal";

import styles from './Home.module.css';

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

interface HomeProps {
  readonly recentPosts: readonly { readonly content: Content }[];
}

const Home = ({ recentPosts }: HomeProps) => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout
      description="Besom - Scala SDK for Pulumi">
      <HomepageHeader />
      <main>
        <br></br>
        <HomepageFeatures />
      </main>
      <main style={{ paddingTop: 30, paddingBottom: 30 }}>
        <section id="blogposts">
          <div className="container">
            <div className="row">
              <div className="col col--1"></div>
              <div className="col col--10">
                <h1 style={{ textAlign: 'center', paddingBottom: 30 }}>Latest Blog Posts</h1>
              </div>
              <div className="col col--1"></div>
            </div>
            <div className="row">
              <div className="col col--1"></div>
              <div className="col col--10">
                {recentPosts.map(({ content: BlogPostContent }) => (
                  <BlogPostProvider
                    key={BlogPostContent.metadata.permalink}
                    content={BlogPostContent}
                  >
                    <BlogPostItem>
                      <BlogPostContent />
                    </BlogPostItem>
                  </BlogPostProvider>
                ))}
              </div>
              <div className="col col--1"></div>
            </div>
            <div className="row">
              <div className="col col--5 col--offset-6">
                <PaginatorNavLink
                  isNext
                  permalink="/blog"
                  title="Older Entries"
                />
              </div>
            </div>
          </div>
        </section>
      </main>
    </Layout >
  );
}

export default Home;