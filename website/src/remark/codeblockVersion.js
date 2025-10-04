import { visit } from 'unist-util-visit';

const codeblockVersion = (besomVersion) => () => async (ast) => {
  visit(ast, 'code', (node) => {
    node.value = node.value.replace('$version', besomVersion)
  })
}

export default codeblockVersion;