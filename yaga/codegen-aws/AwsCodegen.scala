package yaga.codegen.aws

import yaga.codegen.core.extractor.{CodegenSource, ContextSetup, CoursierHelpers}
import yaga.codegen.core.generator.SourcesWriter
import yaga.codegen.core.generator.SourceFile
import yaga.codegen.aws.extractor.LambdaApiExtractor
import yaga.codegen.aws.generator.AwsGenerator

import tastyquery.Contexts.*
import tastyquery.Symbols.*
import java.nio.file.Path

object AwsCodegen:
  def doCodegen(
    codegenSources: List[CodegenSource],
    handlerClassFullName: Option[String],
    packagePrefix: String,
    generateInfra: Boolean,
    lambdaArtifactAbsolutePath: Option[Path],
    lambdaRuntime: Option[String]
  ): Seq[SourceFile] =
    given Context = ContextSetup.contextFromCodegenSources(codegenSources)

    val packagePrefixParts = packagePrefix.split('.').toSeq.filter(_.nonEmpty)

    val extractedApi = handlerClassFullName.map { handlerClassName =>
      LambdaApiExtractor().extractLambdaApi(handlerClassFullName = handlerClassName)
    }.getOrElse {
      LambdaApiExtractor().extractLambdaApi(codegenSources = codegenSources)
    }

    val generator = AwsGenerator(packagePrefixParts, extractedApi)
    
    val modelSources = generator.generateModelSources()

    val infraSources: Seq[SourceFile] = 
      if generateInfra then
        lambdaRuntime match
          case Some(runtime @ "java21") =>
            Seq(
              generator.generateJvmLambda(jarPath = lambdaArtifactAbsolutePath.get),
            )
          case Some(runtime @ "nodejs22.x") =>
            Seq(
              generator.generateNodejsLambda(deployableArchivePath = lambdaArtifactAbsolutePath.get)
            )
          case Some(runtime) =>
            throw Exception(s"Unsupported lambda runtime: $runtime. Should be java21 or nodejs22.x")

          case None =>
            throw Exception("Lambda runtime must be specified for infra generation")
      else
        Seq.empty

    modelSources ++ infraSources


  @main
  def runCodegen(args: String*) =
    val codegenMainArgs = MainArgsParser.parse(args.toList)

    val sources = doCodegen(
      codegenSources = codegenMainArgs.codegenSources,
      handlerClassFullName = codegenMainArgs.handlerClassFullName,
      packagePrefix = codegenMainArgs.packagePrefix,
      generateInfra = codegenMainArgs.generateInfra,
      lambdaArtifactAbsolutePath = codegenMainArgs.lambdaArtifactAbsolutePath,
      lambdaRuntime = codegenMainArgs.lambdaRuntime
    )
    val outputDirPath = os.Path(codegenMainArgs.outputDir)
    val summaryFilePath = codegenMainArgs.summaryFile.map(os.Path(_))

    SourcesWriter().writeSources(outputDirPath, sources, summaryFile = summaryFilePath, cleanUpOutputDir = !codegenMainArgs.noCleanup)
