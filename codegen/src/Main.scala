package besom.codegen

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen.PackageMetadata.{SchemaFile, SchemaName}
import besom.codegen.PackageVersion.PackageVersion
import besom.codegen.metaschema.PulumiPackage

object Main {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case "named" :: name :: version :: Nil =>
        implicit val codegenConfig: CodegenConfig = CodegenConfig()
        generator.generatePackageSources(metadata = PackageMetadata(name, version))
      case "named" :: name :: version :: outputDir :: Nil =>
        implicit val codegenConfig: CodegenConfig = CodegenConfig(outputDir = Some(os.rel / outputDir))
        generator.generatePackageSources(metadata = PackageMetadata(name, version))
      case "metadata" :: metadataPath :: Nil =>
        implicit val codegenConfig: CodegenConfig = CodegenConfig()
        generator.generatePackageSources(metadata = PackageMetadata.fromJsonFile(os.Path(metadataPath)))
      case "metadata" :: metadataPath :: outputDir :: Nil =>
        implicit val codegenConfig: CodegenConfig = CodegenConfig(outputDir = Some(os.rel / outputDir))
        generator.generatePackageSources(metadata = PackageMetadata.fromJsonFile(os.Path(metadataPath)))
      case "schema" :: name :: version :: schemaPath :: Nil =>
        implicit val codegenConfig: CodegenConfig = CodegenConfig()
        generator.generatePackageSources(
          metadata = PackageMetadata(name, version),
          schema = Some(os.Path(schemaPath))
        )
      case _ =>
        System.err.println(
          s"""|Unknown arguments: '${args.mkString(" ")}'
              |
              |Usage:
              |  named <name> <version> [outputDir]               - Generate package from name and version
              |  metadata <metadataPath> [outputDir]              - Generate package from metadata file
              |  schema <name> <version> <schemaPath> [outputDir] - Generate package from schema file
              |""".stripMargin
        )
        sys.exit(1)
    }
  }
}

object generator {
  case class Result(
    schemaName: SchemaName,
    packageVersion: PackageVersion,
    dependencies: List[PackageMetadata],
    outputDir: os.Path
  )

  // noinspection ScalaWeakerAccess
  def generatePackageSources(
    metadata: PackageMetadata,
    schema: Option[SchemaFile] = None
  )(implicit config: CodegenConfig): Result = {
    implicit val logger: Logger = new Logger(config.logLevel)
    implicit val schemaProvider: DownloadingSchemaProvider = new DownloadingSchemaProvider(
      schemaCacheDirPath = config.schemasDir
    )

    // detect possible problems with GH API throttling
    //noinspection ScalaUnusedSymbol
    if (!sys.env.contains("GITHUB_TOKEN"))
      logger.warn("Setting GITHUB_TOKEN environment variable might solve some problems")

    val (pulumiPackage, packageInfo) = schemaProvider.packageInfo(metadata, schema)
    val packageName                  = packageInfo.name
    val packageVersion               = packageInfo.version

    implicit val providerConfig: ProviderConfig = Config.providersConfigs(packageName)

    val outputDir: os.Path =
      config.outputDir.getOrElse(os.rel / packageName / packageVersion).resolveFrom(config.codegenDir)

    generatePackageSources(pulumiPackage, packageInfo, outputDir)
    logger.info(s"Finished generating package '$packageName:$packageVersion' codebase")

    val dependencies = schemaProvider.dependencies(packageName, packageVersion).map { case (name, version) =>
      PackageMetadata(name, version)
    }
    logger.debug(
      s"Dependencies: \n${dependencies.map { case PackageMetadata(name, version, _) => s"- $name:$version\n" }}"
    )

    Result(
      schemaName = packageName,
      packageVersion = packageVersion,
      dependencies = dependencies,
      outputDir = outputDir
    )
  }

  private def generatePackageSources(
    pulumiPackage: PulumiPackage,
    packageInfo: PulumiPackageInfo,
    outputDir: os.Path
  )(implicit
    logger: Logger,
    codegenConfig: CodegenConfig,
    providerConfig: ProviderConfig,
    schemaProvider: SchemaProvider
  ): Unit = {
    // Print diagnostic information
    logger.info {
      val relOutputDir = outputDir.relativeTo(os.pwd)
      s"""|Generating package '${packageInfo.name}:${packageInfo.version}' into '$relOutputDir'
          | - Besom version   : ${codegenConfig.besomVersion}
          | - Scala version   : ${codegenConfig.scalaVersion}
          | - Java version    : ${codegenConfig.javaVersion}
          |
          | - Resources: ${pulumiPackage.resources.size}
          | - Types    : ${pulumiPackage.types.size}
          | - Functions: ${pulumiPackage.functions.size}
          | - Config   : ${pulumiPackage.config.variables.size}
          |""".stripMargin
    }

    implicit val typeMapper: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

    // make sure we don't have a dirty state
    os.remove.all(outputDir)
    os.makeDir.all(outputDir)

    val codeGen = new CodeGen
    try {
      codeGen
        .sourcesFromPulumiPackage(pulumiPackage, packageInfo)
        .foreach { sourceFile =>
          val filePath = outputDir / sourceFile.filePath.osSubPath
          os.makeDir.all(filePath / os.up)
          logger.debug(s"Writing source file: '${filePath.relativeTo(os.pwd)}'")
          try {
            os.write(filePath, sourceFile.sourceCode, createFolders = true)
          } catch {
            case e: java.nio.file.FileAlreadyExistsException =>
              // write the duplicate class for debugging purposes
              val fileDuplicate = filePath / os.up / s"${filePath.last}.duplicate"
              os.write(fileDuplicate, sourceFile.sourceCode, createFolders = true)
              val message = s"Duplicate file '${fileDuplicate.relativeTo(os.pwd)}' while, " +
                s"generating package '${packageInfo.name}:${packageInfo.version}', error: ${e.getMessage}"
              logger.error(message)
          }
        }
    } catch {
      case e: Throwable =>
        logger.error(s"Error generating package '${packageInfo.name}:${packageInfo.version}', error: ${e.getMessage}")
        throw e
    } finally {
      val logFile = outputDir / ".codegen-log.txt"
      if (logger.hasProblems) {
        logger.error(s"Some problems were encountered during the code generation. See ${logFile.relativeTo(os.pwd)}")
      } else {
        logger.info(s"Code generation finished successfully. See ${logFile.relativeTo(os.pwd)}")
      }
      logger.writeToFile(logFile)
    }
  }
}
