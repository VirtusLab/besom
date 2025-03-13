package yaga.codegen.aws.extractor

import tastyquery.Symbols.*
import tastyquery.Types.*

case class ExtractedLambdaApi(
  handlerClassPackageParts: Seq[String],
  handlerClassName: String,
  handlerConfigType: Type,
  handlerInputType: Type,
  handlerOutputType: Type,
  modelSymbols: Seq[Symbol]
)