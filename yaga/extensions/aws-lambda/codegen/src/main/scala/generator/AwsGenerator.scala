package yaga.codegen.aws.generator

import java.nio.file.Path
import tastyquery.Contexts.*
import tastyquery.Symbols.*
import yaga.codegen.core.generator.{ScalaMetaUtils, SourceFile, FilePath, TypeRenderer}
import yaga.codegen.core.generator.scalameta.interpolator.*
import yaga.codegen.core.extractor.ModelExtractor
import yaga.codegen.aws.extractor.ExtractedLambdaApi
import scala.meta.XtensionSyntax

class AwsGenerator(packagePrefixParts: Seq[String], lambdaApi: ExtractedLambdaApi):
  val apiSymbolSet = lambdaApi.modelSymbols.toSet
  val typeRenderer = TypeRenderer(packagePrefixParts, apiSymbolSet)

  def generateModelSources()(using Context): Seq[SourceFile] =
    lambdaApi.modelSymbols.flatMap:
      case sym: ClassSymbol if sym.isCaseClass =>
        Seq(sourceForCaseClass(sym))
      case sym =>
        throw Exception(s"Code generation unsupported for symbol: ${sym}")
         // TODO handle other types

  def sourceForCaseClass(sym: ClassSymbol)(using Context): SourceFile =
    val packagesSuffixParts = ModelExtractor.ownerPackageNamesChain(sym.owner)
    val packageParts = packagePrefixParts ++ packagesSuffixParts
    val packageRef = ScalaMetaUtils.packageRefFromParts(packageParts)
    val packageClause = Option.when(packageParts.nonEmpty)(m"package ${packageRef}")

    val className = scala.meta.Type.Name(sym.name.toString)
    val fieldLines = sym.tree.get.rhs.constr.paramLists match
      case Nil => Nil
      case List(Left(termParams)) =>
        termParams.map: param =>
          val fieldName = scala.meta.Term.Name(param.name.toString)
          val fieldType = typeRenderer.typeToCode(param.tpt.toType)
          m"  ${fieldName}: ${fieldType}"
      case lists =>
        throw Exception(s"Cannot generate code for class: ${sym} - only case classes with a single parameter list are supported")

    val sourceCode =
      m"""|/*
          | * This file was generated by yaga. Do not modify it manually!
          | */
          |
          |package ${packageRef}
          |
          |import _root_.besom.json.defaultProtocol
          |import _root_.besom.json.defaultProtocol.given
          |
          |case class ${className} (
          |${fieldLines.mkString(",\n")}
          |) derives _root_.besom.json.JsonFormat
          |""".stripMargin
    SourceFile(
      FilePath(packageParts :+ s"${sym.name}.scala"),
      sourceCode
    )

  def generateJvmLambda(jarPath: Path)(using Context): SourceFile =
    val lamdaHandler = (lambdaApi.handlerClassPackageParts :+ lambdaApi.handlerClassName).mkString(".") // TODO don't handle this logic here
    generateLambda(
      lambdaHandler = lamdaHandler,
      runtime = "java21",
      codeArchivePath = jarPath
    )

  def generateNodejsLambda(deployableArchivePath: Path)(using Context): SourceFile =
    generateLambda(
      lambdaHandler = "index.handler",
      runtime = "nodejs22.x",
      codeArchivePath = deployableArchivePath
    )

  def generateLambda(
    lambdaHandler: String,
    runtime: String,
    codeArchivePath: Path
  )(using Context): SourceFile =
    val packagesSuffixParts = lambdaApi.handlerClassPackageParts
    val packageParts = packagePrefixParts ++ packagesSuffixParts
    val packageRef = ScalaMetaUtils.packageRefFromParts(packageParts)
    val packageClause = Option.when(packageParts.nonEmpty)(m"package ${packageRef}")

    val lambdaClassName = scala.meta.Type.Name(lambdaApi.handlerClassName)

    val configTypeCode = typeRenderer.typeToCode(lambdaApi.handlerConfigType)
    val inputTypeCode = typeRenderer.typeToCode(lambdaApi.handlerInputType)
    val outputTypeCode = typeRenderer.typeToCode(lambdaApi.handlerOutputType)

    val UnitClass = ctx.defn.UnitClass

    val defaultConfigValueSnippet = lambdaApi.handlerConfigType.showBasic match // TODO handle other types in an extensible / more generic way
      case "scala.Unit" => " = ()"
      case _ => ""

    val sourceCode =
      m"""|/*
          | * This file was generated by yaga. Do not modify it manually!
          | */
          |
          |${packageClause}
          |
          |class ${lambdaClassName} private(
          |  underlyingFunction: _root_.besom.api.aws.lambda.Function,
          |  lambdaHandle: _root_.yaga.extensions.aws.lambda.LambdaHandle[${lambdaClassName}.Input, ${lambdaClassName}.Output]
          |) extends _root_.yaga.extensions.aws.lambda.internal.Lambda[${lambdaClassName}.Input, ${lambdaClassName}.Output](
          |  underlyingFunction = underlyingFunction,
          |  lambdaHandle = lambdaHandle
          |)
          |
          |object ${lambdaClassName}:
          |  type Config = ${configTypeCode}
          |  type Input = ${inputTypeCode}
          |  type Output = ${outputTypeCode}
          |
          |  def apply(
          |    name: _root_.besom.util.NonEmptyString,
          |    args: _root_.besom.api.aws.lambda.FunctionArgs,
          |    config: _root_.besom.types.Input[Config]${defaultConfigValueSnippet},
          |    opts: _root_.besom.ResourceOptsVariant.Custom ?=> _root_.besom.CustomResourceOptions = _root_.besom.CustomResourceOptions()
          |  ): _root_.besom.types.Output[${lambdaClassName}] =
          |    val runtime = "$runtime"
          |    val handlerName = "${lambdaHandler}"
          |    val codeArchivePath = "${codeArchivePath.toAbsolutePath.toString}"
          |
          |    for
          |      lambda <- _root_.yaga.extensions.aws.lambda.internal.Lambda[Config, Input, Output](
          |        name = name,
          |        codeArchive = _root_.besom.types.Archive.FileArchive(codeArchivePath),
          |        handlerName = handlerName,
          |        runtime = runtime,
          |        config = config,
          |        args = args,
          |        opts = opts
          |      )
          |    yield new ${lambdaClassName}(
          |      underlyingFunction = lambda.underlyingFunction,
          |      lambdaHandle = lambda.lambdaHandle
          |    )
          |
          |""".stripMargin

    SourceFile(
      FilePath(packageParts :+ s"${lambdaClassName}.scala"),
      sourceCode
    )
