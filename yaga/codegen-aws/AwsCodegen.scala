package yaga.codegen.aws

import yaga.codegen.core.extractor.CodegenSource
import yaga.codegen.core.extractor.ContextSetup
import yaga.codegen.core.generator.SourcesWriter
import yaga.codegen.core.generator.SourceFile
import yaga.codegen.aws.extractor.LambdaApiExtractor
import yaga.codegen.aws.generator.AwsGenerator

import tastyquery.Contexts.*
import tastyquery.Symbols.*

object AwsCodegen:
  def doCodegen(
    codegenSources: List[CodegenSource],
    handlerClassName: Option[String],
    packagePrefix: String,
    generateInfra: Boolean,
  ): Seq[SourceFile] =
    given Context = ContextSetup.contextFromCodegenSources(codegenSources)

    val packagePrefixParts = packagePrefix.split('.').toSeq.filter(_.nonEmpty)

    val extractedApi = handlerClassName.map { handlerClassName =>
      LambdaApiExtractor().extractLambdaApi(handlerClassName = handlerClassName)
    }.getOrElse {
      LambdaApiExtractor().extractLambdaApi(codegenSources = codegenSources)
    }

    val generator = AwsGenerator(packagePrefixParts, extractedApi)
    
    val modelSources = generator.generateModelSources()

    if generateInfra then
      codegenSources match
        case List(CodegenSource.LocalJar(absoluteJarPath)) =>
          modelSources :+ generator.generateLambda(
            jarPath = absoluteJarPath,
          )
        case sources =>
          throw Exception(s"Only codegen from a single local artifact is supported for infra generation, but got the following sources: $sources")
    else
      modelSources


  @main
  def runCodegen(args: String*) =
    val codegenMainArgs = MainArgsParser.parse(args.toList)

    val sources = doCodegen(
      codegenSources = codegenMainArgs.codegenSources,
      handlerClassName = codegenMainArgs.handlerClassName,
      packagePrefix = codegenMainArgs.packagePrefix,
      generateInfra = codegenMainArgs.generateInfra
    )
    val outputDirPath = os.Path(codegenMainArgs.outputDir)
    val summaryFilePath = codegenMainArgs.summaryFile.map(os.Path(_))

    SourcesWriter().writeSources(outputDirPath, sources, summaryFile = summaryFilePath, cleanUpOutputDir = !codegenMainArgs.noCleanup)
