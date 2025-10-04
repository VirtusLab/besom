import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from './HomepageFeatures';

import { Content } from "@theme/BlogPostPage";
import PaginatorNavLink from "@theme/PaginatorNavLink";

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

type PostContent = {
  metadata: {
    title: string
    permalink: string
    date: string
    description?: string
    formattedDate?: string
    readingTime?: number
    authors?: Array<{
      name?: string
      title?: string
      url?: string
      imageURL?: string
    }>
    tags?: Array<{ label: string; permalink: string }>
    truncated?: boolean
  }
}

function CustomBlogPostItem({ content }: { content: PostContent }) {
  const { metadata } = content
  const {
    title,
    permalink,
    date,
    description,
    formattedDate,
    readingTime,
    authors = [],
    tags = [],
  } = metadata

  const dateStr =
    formattedDate ??
    new Date(date).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
  const readStr = readingTime && readingTime > 0 ? ` · ${Math.ceil(readingTime)} min read` : ''

  return (
    <article className={styles.card} itemProp="blogPost" itemScope itemType="http://schema.org/BlogPosting">
      <header className={styles.header}>
        <h2 className={styles.title}>
          <Link itemProp="url" to={permalink}>
            <span itemProp="headline">{title}</span>
          </Link>
        </h2>

        <div className={styles.meta}>
          <time dateTime={date} itemProp="datePublished">
            {dateStr}
          </time>
          <span className={styles.dot}>·</span>
          <span>{readStr ? readStr.slice(3) : ''}</span>
        </div>

        {authors.length > 0 && (
          <div className={styles.authors}>
            {authors.map((a, i) => (
              <div key={(a.url || a.name || '') + i} className={styles.author}>
                {a.imageURL && <img src={a.imageURL} alt={a.name ?? ''} className={styles.avatar} loading="lazy" />}
                <div className={styles.authorText}>
                  {a.url ? (
                    <a href={a.url} className={styles.authorName}>{a.name}</a>
                  ) : (
                    <span className={styles.authorName}>{a.name}</span>
                  )}
                  {a.title && <div className={styles.authorTitle}>{a.title}</div>}
                </div>
              </div>
            ))}
          </div>
        )}
      </header>

      {description && (
        <p className={styles.excerpt} itemProp="description">
          {description}
        </p>
      )}

      {tags.length > 0 && (
        <div className={styles.tags}>
          {tags.map((t) => (
            <Link key={t.permalink} to={t.permalink} className="badge badge--secondary margin-right--sm">
              {t.label}
            </Link>
          ))}
        </div>
      )}

      <footer className={styles.footer}>
        <Link to={permalink} className="button button--primary button--sm">
          Read more
        </Link>
      </footer>
    </article>
  )
};

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
                {recentPosts.map(({ content }) => (
                  <CustomBlogPostItem
                    key={content.metadata.permalink}
                    content={content}
                  >
                  </CustomBlogPostItem>
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