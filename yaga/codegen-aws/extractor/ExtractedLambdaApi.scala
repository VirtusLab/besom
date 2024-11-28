package yaga.codegen.aws.extractor

import tastyquery.Symbols.*
import tastyquery.Types.*

case class ExtractedLambdaApi(
  handlerConfigType: Type,
  handlerInputType: Type,
  handlerOutputType: Type,
  modelSymbols: Seq[Symbol]
)