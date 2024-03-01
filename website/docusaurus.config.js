// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const fs = require('fs');

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');
const codeblockVersion = require('./src/remark/codeblockVersion');

const organizationName = 'virtuslab';
const projectName = 'besom';

const besomVersion = fs.readFileSync('../version.txt').toString().trim()

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Besom - Pulumi Scala',
  tagline: 'Scala SDK for Pulumi',
  favicon: 'img/favicon.ico',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: organizationName, // Usually your GitHub org/user name.
  projectName: projectName, // Usually your repo name.

  // https://plausible.io/docs/docusaurus-integration
  scripts: [{
    src: 'https://plausible.io/js/script.js',
    defer: true,
    'data-domain': 'virtuslab.github.io/besom'
  }],

  // Set the production url of your site here
  url: `https://${organizationName}.github.io`,
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: `/${projectName}/`,

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  customFields: {
    besomVersion: besomVersion
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          remarkPlugins: [codeblockVersion(besomVersion)],
          editUrl: 'https://github.com/VirtusLab/besom/tree/main/website'
        },
        blog: {
          showReadingTime: true,
          blogSidebarCount: 0,
          blogTitle: 'Besom, Scala SDK for Pulumi blog',
          blogDescription: 'The latest news and updates about Besom, Scala SDK for Pulumi',
          feedOptions: {
            type: 'all',
            title: 'Besom, Scala SDK for Pulumi blog',
            copyright: `Copyright © ${new Date().getFullYear()} VirtusLab Sp. z o.o.`,
          },
          editUrl: 'https://github.com/VirtusLab/besom/tree/main/website'
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      image: 'img/Besom_logo_black.png',
      navbar: {
        logo: {
          alt: 'Besom - Pulumi Scala',
          src: 'img/Besom_logo_full_color.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Documentation',
          },
          {
            to: 'blog',
            // type: 'docSidebar',
            // sidebarId: 'blogSidebar',
            position: 'left',
            label: 'Blog'
          },
          {
            href: 'https://github.com/virtuslab/besom',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        copyright: `Copyright © ${new Date().getFullYear()} VirtusLab Sp. z o.o.  Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: [
          'java',
          'scala'
        ],
      },
    }),
};

module.exports = config;
