package yaga.codegen.aws.extractor

import tastyquery.Contexts.*
import tastyquery.Symbols.*
import tastyquery.Types.*

class LambdaApiExtractor():
  def extractLambdaApi(handlerClassName: String)(using Context): ExtractedLambdaApi =

    // TODO
    // val lambdaHandlerClassName = classOf[yaga.extensions.aws.lambda.LambdaHandler[?, ?, ?]].getName
    val lambdaHandlerClassName = "yaga.extensions.aws.lambda.LambdaHandler"

    val cls = ctx.findTopLevelClass(handlerClassName)

    val rootTypes = cls.parents.collectFirst:
      case at: AppliedType if at.tycon.showBasic == lambdaHandlerClassName /* TODO don't rely on showBasic? */  =>
        at.args.collect { case tpe: Type => tpe }
    .getOrElse(throw Exception(s"Class $handlerClassName does not extend $lambdaHandlerClassName"))

    val List(handlerConfigType, handlerInputType, handlerOutputType) = rootTypes

    val modelSymbols = extractReferencedSymbols(rootTypes).toSeq

    ExtractedLambdaApi(
      handlerConfigType = handlerConfigType,
      handlerInputType = handlerInputType,
      handlerOutputType = handlerOutputType,
      modelSymbols = modelSymbols
    )

  private def extractReferencedSymbols(rootTypes: Seq[Type])(using Context): Set[ClassSymbol] =
    val modelExtractor = AwsModelExtractor()
    modelExtractor.collect(rootTypes)
