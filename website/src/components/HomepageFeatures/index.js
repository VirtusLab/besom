import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Define infrastructure using Scala 3',
    Svg: require('@site/static/img/scala-logo.svg').default,
    link: 'https://www.scala-lang.org/',
    description: (
      <>
        No need to learn a new language.<br />Let Scala's type system protect your deployments too.
      </>
    ),
  },
  {
    title: 'Leverage the Pulumi ecosystem',
    Svg: require('@site/static/img/pulumi_logo_black_font.svg').default,
    link: 'https://www.pulumi.com/',
    description: (
      <>
        Use the <a href="https://www.pulumi.com/registry/">Pulumi packages</a> to deploy your apps to any cloud.<br />
        Integrate with any SaaS or platform of your choice.
      </>
    ),
  },
];

function Feature({ Svg, link, title, description }) {
  return (
    <div className={clsx('col col--6')}>
      <div className="text--center">
        <a href={link}>
          <Svg className={styles.featureSvg} role="img" height="100" />
        </a>
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
