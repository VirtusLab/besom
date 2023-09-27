/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */

// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    {
      type: 'doc',
      id: 'intro',
      label: 'Introduction',
    },
    {
      type: 'doc',
      id: 'getting_started',
      label: 'Getting started',
    },
    {
      type: 'doc',
      id: 'tutorial',
      label: 'Tutorial',
    },
    {
      type: 'category',
      label: 'Architecture',
      collapsible: true,
      collapsed: false,
      items: [
        'architecture',
        'context',
        'exports',
        'constructors',
        'laziness',
        'apply_methods',
        'logging',
        'lifting',
        'interpolator',
        'components',
        'missing'
      ],
    },
    {
      type: 'doc',
      id: 'intro', // TODO url to scaladoc of Core
      label: 'API Reference',
    },
    {
      type: 'doc',
      id: 'intro',
      label: 'Packages',
    },
    {
      type: 'doc',
      id: 'intro',
      label: 'Examples',
    },
    // {
    //   type: 'doc',
    //   id: 'intro',
    //   label: 'Contributing',
    // },
    // {
    //   type: 'doc',
    //   id: 'intro',
    //   label: 'License',
    // },
  ],
};

module.exports = sidebars;
