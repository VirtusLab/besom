// Copied (and adapted) from codegen/**/Codegen.scala

package yaga.codegen.core.generator

case class SourceFile(
  filePath: FilePath,
  sourceCode: String
)