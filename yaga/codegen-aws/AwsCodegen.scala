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
    handlerClassName: Option[String],
    packagePrefix: String,
    outputDir: String,
    generateInfra: Boolean
  )

  // TODO: Better parsing
  def parseArgs(args: Seq[String]): CodegenArgs =
    args.toList match
      case
        "--maven-artifact" :: artifactMavenCoordinates ::
        //"--handler-class" :: handlerClassName ::
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
            handlerClassName = None, // = Some(handlerClassName),
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

    val artifactJarPath = ContextSetup.jarPathFromMavenCoordinates(
      orgName = artifactOrgName,
      moduleName = artifactModuleName,
      version = artifactVersion
    )

    given Context = ContextSetup.contextFromJar(artifactJarPath)

    val packagePrefixParts = codegenArgs.packagePrefix.split('.').toSeq.filter(_.nonEmpty)

    val outputDirPath = os.Path(codegenArgs.outputDir)

    val extractedApi = codegenArgs.handlerClassName.map { handlerClassName =>
      LambdaApiExtractor().extractLambdaApi(handlerClassName = handlerClassName)
    }.getOrElse {
      LambdaApiExtractor().extractLambdaApi(artifactJarPath = artifactJarPath)
    }

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
