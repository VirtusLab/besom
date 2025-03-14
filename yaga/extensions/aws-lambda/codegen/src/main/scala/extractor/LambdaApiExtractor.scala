package yaga.codegen.aws.extractor

import tastyquery.Contexts.*
import tastyquery.Symbols.*
import tastyquery.Types.*
import io.github.classgraph.ClassGraph
import scala.jdk.CollectionConverters.*
import yaga.codegen.core.extractor.{CodegenSource, ContextSetup, ModelExtractor}

class LambdaApiExtractor():
  // TODO
  // val lambdaHandlerBaseClassFullName = classOf[yaga.extensions.aws.lambda.LambdaHandler[?, ?, ?]].getName
  private val lambdaHandlerBaseClassFullName = "yaga.extensions.aws.lambda.LambdaHandler"
  private val lambdaAsyncHandlerBaseClassFullName = "yaga.extensions.aws.lambda.LambdaAsyncHandler"

  def extractLambdaApi(handlerClassFullName: String)(using Context): ExtractedLambdaApi =
    val handlerClass = ctx.findTopLevelClass(handlerClassFullName)

    val handlerClassPackageParts = ModelExtractor.ownerPackageNamesChain(handlerClass.owner)
    val handlerClassName = handlerClass.name.toString

    val rootTypes = handlerClass.parents.collectFirst:
      case at: AppliedType if at.tycon.showBasic == lambdaHandlerBaseClassFullName || at.tycon.showBasic == lambdaAsyncHandlerBaseClassFullName /* TODO don't rely on showBasic? */  =>
        at.args.collect { case tpe: Type => tpe }
    .getOrElse(throw Exception(s"Class $handlerClassName does not directly extend $lambdaHandlerBaseClassFullName or $lambdaAsyncHandlerBaseClassFullName"))

    val List(handlerConfigType, handlerInputType, handlerOutputType) = rootTypes

    val modelSymbols = extractReferencedSymbols(rootTypes).toSeq

    ExtractedLambdaApi(
      handlerClassPackageParts = handlerClassPackageParts,
      handlerClassName = handlerClassName,
      handlerConfigType = handlerConfigType,
      handlerInputType = handlerInputType,
      handlerOutputType = handlerOutputType,
      modelSymbols = modelSymbols
    )

  private def extractReferencedSymbols(rootTypes: Seq[Type])(using Context): Set[ClassSymbol] =
    val modelExtractor = AwsModelExtractor()
    modelExtractor.collect(rootTypes)

  def extractLambdaApi(codegenSources: Seq[CodegenSource])(using Context): ExtractedLambdaApi =
    // TODO remove duplication with LambdaHandlerUtils.scala

    val jarUrls = ContextSetup.getSourcesClasspath(codegenSources).map { path =>
      path.toUri.toURL
    }.toArray

    val jarClassLoader = new java.net.URLClassLoader(jarUrls)

    // TODO filter out classes from transitive dependencies?
    val lambdaSyncHandlerSubclasses = new ClassGraph().overrideClassLoaders(jarClassLoader).enableClassInfo.scan().getSubclasses(lambdaHandlerBaseClassFullName).asScala.toList
    val lambdaAsyncHandlerSubclasses = new ClassGraph().overrideClassLoaders(jarClassLoader).enableClassInfo.scan().getSubclasses(lambdaAsyncHandlerBaseClassFullName).asScala.toList

    val lambdaHandlerSubclasses = lambdaSyncHandlerSubclasses ++ lambdaAsyncHandlerSubclasses
    lambdaHandlerSubclasses match
      case Nil =>
        throw Exception(s"No lambda handler found in codegen sources $codegenSources")
      case handler :: Nil =>
        extractLambdaApi(handlerClassFullName = handler.getName)
      case handlers =>
        val handlerNames = handlers.map(_.getName).mkString(", ")
        throw Exception(s"Multiple lambda handlers found in codegen sources $codegenSources: ${handlerNames}")
