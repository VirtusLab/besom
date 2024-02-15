const visit = require('unist-util-visit');

const codeblockVersion = () => async (ast) => {
  visit(ast, 'code', (node) => {
    node.value = node.value.replace('$version', '0.2.1') // TODO read from env: process.env.BESOM_VERSION
  })
}

module.exports = codeblockVersion;