package yaga.codegen.aws.extractor

import tastyquery.Contexts.*
import tastyquery.Symbols.*
import tastyquery.Types.*
import io.github.classgraph.ClassGraph
import scala.collection.JavaConverters.asScalaIteratorConverter

class LambdaApiExtractor():
  // TODO
  // val lambdaHandlerBaseClassFullName = classOf[yaga.extensions.aws.lambda.LambdaHandler[?, ?, ?]].getName
  private val lambdaHandlerBaseClassFullName = "yaga.extensions.aws.lambda.LambdaHandler"

  def extractLambdaApi(handlerClassName: String)(using Context): ExtractedLambdaApi =

    val cls = ctx.findTopLevelClass(handlerClassName)

    val rootTypes = cls.parents.collectFirst:
      case at: AppliedType if at.tycon.showBasic == lambdaHandlerBaseClassFullName /* TODO don't rely on showBasic? */  =>
        at.args.collect { case tpe: Type => tpe }
    .getOrElse(throw Exception(s"Class $handlerClassName does not extend $lambdaHandlerBaseClassFullName"))

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

  def extractLambdaApi(artifactJarPath: java.nio.file.Path)(using Context): ExtractedLambdaApi =
    // TODO remove duplication with LambdaHandlerUtils.scala

    val jarClassLoader = new java.net.URLClassLoader(Array(artifactJarPath.toUri.toURL))

    // TODO filter out classes from transitive dependencies?
    val lambdaHandlerSubclasses = new ClassGraph().overrideClassLoaders(jarClassLoader).enableClassInfo.scan().getSubclasses(lambdaHandlerBaseClassFullName).iterator.asScala.toList
    lambdaHandlerSubclasses match
      case Nil =>
        throw Exception(s"No lambda handler found in $artifactJarPath")
      case handler :: Nil =>
        extractLambdaApi(handlerClassName = handler.getName)
      case handlers =>
        val handlerNames = handlers.map(_.getName).mkString(", ")
        throw Exception(s"Multiple lambda handlers found in $artifactJarPath: ${handlerNames}")
