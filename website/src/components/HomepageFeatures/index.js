import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Write infrastructure in Scala 3',
    Svg: require('@site/static/img/scala-logo.svg').default,
    description: (
      <>
        No need to learn a new language. Use Scala 3 to write your infrastructure.
      </>
    ),
  },
  {
    title: 'Leverage the Pulumi ecosystem',
    Svg: require('@site/static/img/pulumi_logo_black_font.svg').default,
    description: (
      <>
        Seemlessly convert your existing Puluimi infrastructure to Scala 3.
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--6')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
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
