import { visit } from 'unist-util-visit';

const versionPlaceholder = '@BESOM_VERSION@';

const codeblockVersion = (besomVersion) => () => async (ast) => {
  visit(ast, 'code', (node) => {
    node.value = node.value.replaceAll(versionPlaceholder, besomVersion)
  })
  visit(ast, 'link', (node) => {
    node.url = node.url.replaceAll(versionPlaceholder, besomVersion)
  })
}

export default codeblockVersion;
