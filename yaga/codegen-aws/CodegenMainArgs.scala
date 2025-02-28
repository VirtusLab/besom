package yaga.codegen.aws

import yaga.codegen.core.extractor.CodegenSource
import java.nio.file.Path

case class CodegenMainArgs(
  codegenSources: List[CodegenSource],
  handlerClassFullName: Option[String],
  packagePrefix: String,
  generateInfra: Boolean,
  lambdaArtifactAbsolutePath: Option[Path],
  lambdaRuntime: Option[String],
  outputDir: String,
  summaryFile: Option[String],
  noCleanup: Boolean
)
