package yaga.codegen.aws

import yaga.codegen.core.generator.SourcesWriter
import yaga.codegen.core.generator.SourceFile
import yaga.codegen.aws.extractor.LambdaApiExtractor
import yaga.codegen.aws.generator.AwsGenerator
import yaga.codegen.core.ContextSetup

import tastyquery.Contexts.*
import tastyquery.Symbols.*

object AwsCodegen:
  case class CodegenArgs(
    artifactMavenCoordinates: String,
    handlerClassName: String,
    packagePrefix: String,
    outputDir: String,
    generateInfra: Boolean
  )

  def parseArgs(args: Seq[String]): CodegenArgs =
    args.toList match
      case
        "--maven-artifact" :: artifactMavenCoordinates ::
        "--handler-class" :: handlerClassName ::
        "--package-prefix" :: packagePrefix ::
        "--output-dir" :: outputDir ::
        rest =>
          val generateInfra = rest match
            case Nil =>
              false
            case "--with-infra" :: Nil =>
              true
            case _ =>
              throw Exception(s"Invalid arguments: ${args}")
          
          CodegenArgs(
            artifactMavenCoordinates = artifactMavenCoordinates,
            handlerClassName = handlerClassName,
            packagePrefix = packagePrefix,
            outputDir = outputDir,
            generateInfra = generateInfra
          )
      case _ =>
        throw Exception(s"Invalid arguments: ${args}")

  @main
  def runCodegen(args: String*) =
    val codegenArgs = parseArgs(args)

    val List(artifactOrgName, artifactModuleName, artifactVersion) = codegenArgs.artifactMavenCoordinates.split(":").toList

    given Context = ContextSetup.contextFromMavenCoordinates(
      orgName = artifactOrgName,
      moduleName = artifactModuleName,
      version = artifactVersion
    )

    val packagePrefixParts = codegenArgs.packagePrefix.split('.').toSeq.filter(_.nonEmpty)

    val outputDirPath = os.Path(codegenArgs.outputDir)

    val extractedApi = LambdaApiExtractor().extractLambdaApi(
      handlerClassName = codegenArgs.handlerClassName
    )

    val generator = AwsGenerator(packagePrefixParts, extractedApi)
    
    val modelSources = generator.generateModelSources()
    val sources = if codegenArgs.generateInfra then
      modelSources :+ generator.generateLambda(
        artifactOrgName = artifactOrgName,
        artifactModuleName = artifactModuleName,
        artifactVersion = artifactVersion,
      )
    else
      modelSources

    SourcesWriter().writeSources(outputDirPath, sources)
