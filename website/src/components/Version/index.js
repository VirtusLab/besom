import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

const Version = () => {
  const { siteConfig } = useDocusaurusContext();
  const { besomVersion } = siteConfig.customFields;

  return (<>{besomVersion}</>);
}

export default Version;