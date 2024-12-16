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
  val generatedPackagePrefixParts = ModelExtractor.generatedPackagePrefixParts ++ packagePrefixParts
  val apiSymbolSet = lambdaApi.modelSymbols.toSet
  val typeRenderer = TypeRenderer(generatedPackagePrefixParts, apiSymbolSet)

  def generateModelSources()(using Context): Seq[SourceFile] =
    lambdaApi.modelSymbols.flatMap:
      case sym: ClassSymbol if sym.isCaseClass =>
        Seq(sourceForCaseClass(sym))
      case sym =>
        throw Exception(s"Code generation unsupported for symbol: ${sym}")
         // TODO handle other types

  def sourceForCaseClass(sym: ClassSymbol)(using Context): SourceFile =
    val packagesSuffixParts = ModelExtractor.ownerPackageNamesChain(sym.owner)
    val packageParts = generatedPackagePrefixParts ++ packagesSuffixParts
    val packageRef = ScalaMetaUtils.packageRefFromParts(packageParts)

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
          | * This file was generated. Do not modify it manually!
          | */
          |
          |package ${packageRef}
          |
          |import besom.json.defaultProtocol
          |import besom.json.defaultProtocol.given
          |
          |case class ${className} (
          |${fieldLines.mkString(",\n")}
          |) derives besom.json.JsonFormat
          |""".stripMargin
    SourceFile(
      FilePath(packageParts :+ s"${sym.name}.scala"),
      sourceCode
    )

  // TODO Re-enable referring to published artifacts
  // def generateLambda(artifactOrgName: String, artifactModuleName: String, artifactVersion: String)(using Context): SourceFile =
  def generateLambda(jarPath: Path)(using Context): SourceFile =
    val packageParts = generatedPackagePrefixParts
    val packageRef = ScalaMetaUtils.packageRefFromParts(packageParts)

    val configType = typeRenderer.typeToCode(lambdaApi.handlerConfigType)
    val inputTypeCode = typeRenderer.typeToCode(lambdaApi.handlerInputType)
    val outputTypeCode = typeRenderer.typeToCode(lambdaApi.handlerOutputType)

    val UnitClass = ctx.defn.UnitClass

    val defaultConfigValueCode = lambdaApi.handlerConfigType.showBasic match // TODO handle other types in an extensible / more generic way
      case "scala.Unit" => " = ()"
      case _ => ""

    val javaRuntime = "java21"

    // val handlerMetadataSnippet = m"""yaga.extensions.aws.lambda.internal.LambdaHandlerUtils.lambdaHandlerMetadataFromMavenCoordinates[Config, Input, Output](orgName = "${artifactOrgName}", moduleName = "${artifactModuleName}",version = "${artifactVersion}")"""
    val handlerMetadataSnippet = m"""yaga.extensions.aws.lambda.internal.LambdaHandlerUtils.lambdaHandlerMetadataFromLocalFatJar[Config, Input, Output](filePath = "${jarPath.toAbsolutePath.toString}")"""

    val sourceCode =
      m"""|/*
          | * This file was generated. Do not modify it manually!
          | */
          |
          |package ${packageRef}
          |
          |class Lambda private(
          |  underlyingFunction: besom.api.aws.lambda.Function,
          |  lambdaHandle: yaga.extensions.aws.lambda.LambdaHandle[Lambda.Input, Lambda.Output]
          |) extends yaga.extensions.aws.lambda.internal.Lambda[Lambda.Input, Lambda.Output](
          |  underlyingFunction = underlyingFunction,
          |  lambdaHandle = lambdaHandle
          |)
          |
          |object Lambda:
          |  type Config = ${configType}
          |  type Input = ${inputTypeCode}
          |  type Output = ${outputTypeCode}
          |
          |  def apply(
          |    name: besom.util.NonEmptyString,
          |    args: besom.api.aws.lambda.FunctionArgs,
          |    config: besom.types.Input[Config]${defaultConfigValueCode},
          |    opts: besom.ResourceOptsVariant.Custom ?=> besom.CustomResourceOptions = besom.CustomResourceOptions()
          |  ): besom.types.Output[Lambda] =
          |    val metadata = ${handlerMetadataSnippet}
          |    val javaRuntime = "$javaRuntime"
          |
          |    import besom.json.DefaultJsonProtocol.given
          |
          |    for
          |      lambda <- yaga.extensions.aws.lambda.internal.Lambda[Config, Input, Output](
          |        name = name,
          |        codeArchive = besom.types.Archive.FileArchive(metadata.artifactAbsolutePath),
          |        handlerClassName = metadata.handlerClassName,
          |        runtime = javaRuntime,
          |        config = config,
          |        args = args,
          |        opts = opts
          |      )
          |    yield new Lambda(
          |      underlyingFunction = lambda.underlyingFunction,
          |      lambdaHandle = lambda.lambdaHandle
          |    )
          |
          |""".stripMargin

    SourceFile(
      FilePath(packageParts :+ "Lambda.scala"),
      sourceCode
    )
