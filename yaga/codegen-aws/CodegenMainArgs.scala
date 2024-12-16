package yaga.codegen.aws

import yaga.codegen.core.extractor.CodegenSource

case class CodegenMainArgs(
  codegenSources: List[CodegenSource],
  handlerClassName: Option[String],
  packagePrefix: String,
  generateInfra: Boolean,
  outputDir: String,
  summaryFile: Option[String],
  noCleanup: Boolean
)
