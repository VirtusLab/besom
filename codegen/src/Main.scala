package besom.codegen

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen.SchemaProvider.{SchemaFile, SchemaName, SchemaVersion}
import besom.codegen.metaschema.PulumiPackage

object Main {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case schemasDirPath :: outputDirBasePath :: providerName :: schemaVersion :: besomVersion :: Nil =>
        implicit val codegenConfig: CodegenConfig = CodegenConfig(
          schemasDir = os.Path(schemasDirPath),
          codegenDir = os.Path(outputDirBasePath),
          besomVersion = besomVersion,
        )
        generator.generatePackageSources(
          schema = Right((providerName, schemaVersion))
        )
      case _ =>
        System.err.println(
          "Codegen's expected arguments: <schemasDirPath> <outputDirBasePath> <providerName> <schemaVersion> <besomVersion>"
        )
        sys.exit(1)
    }
  }
}

object generator {
  case class Result(
    schemaName: SchemaName,
    schemaVersion: SchemaVersion,
    dependencies: List[Dependency],
    outputDir: os.Path
  )

  case class Dependency(
    schemaName: SchemaProvider.SchemaName,
    schemaVersion: SchemaProvider.SchemaVersion
  )

  // noinspection ScalaWeakerAccess
  def generatePackageSources(
    schema: Either[SchemaFile, (SchemaName, SchemaVersion)]
  )(implicit config: CodegenConfig): Result = {
    implicit val logger: Logger = new Logger
    implicit val schemaProvider: DownloadingSchemaProvider = new DownloadingSchemaProvider(
      schemaCacheDirPath = config.schemasDir
    )

    val pulumiPackage = schema match {
      case Left(schema)           => schemaProvider.pulumiPackage(schema)
      case Right((name, version)) => schemaProvider.pulumiPackage(name, version)
    }
    val packageInfo   = schemaProvider.packageInfo(pulumiPackage)
    val schemaName    = packageInfo.schemaName
    val schemaVersion = packageInfo.schemaVersion

    implicit val providerConfig: ProviderConfig = Config.providersConfigs(schemaName)
    
    val outputDir: os.Path = config.outputDir.getOrElse(os.rel / schemaName / schemaVersion).resolveFrom(config.codegenDir)

    generatePackageSources(pulumiPackage, packageInfo, outputDir)
    logger.info(s"Finished generating package '$schemaName:$schemaVersion' codebase")

    val dependencies = schemaProvider.dependencies(schemaName, schemaVersion).map { case (name, version) =>
      Dependency(name, version)
    }
    logger.debug(
      s"Dependencies: \n${dependencies.map { case Dependency(name, version) => s"- $name:$version\n" }}"
    )

    Result(
      schemaName = schemaName,
      schemaVersion = schemaVersion,
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
    logger.info({
      val relOutputDir = outputDir.relativeTo(os.pwd)
      s"""|Generating package '${packageInfo.schemaName}:${packageInfo.schemaVersion}' into '$relOutputDir'
          | - Besom version   : ${codegenConfig.besomVersion}
          | - Scala version   : ${codegenConfig.scalaVersion}
          | - Java version    : ${codegenConfig.javaVersion}
          |
          | - Resources: ${pulumiPackage.resources.size}
          | - Types    : ${pulumiPackage.types.size}
          | - Functions: ${pulumiPackage.functions.size}
          | - Config   : ${pulumiPackage.config.variables.size}
          |""".stripMargin
    })

    implicit val typeMapper: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

    // make sure we don't have a dirty state
    os.remove.all(outputDir)
    os.makeDir.all(outputDir)

    val codeGen = new CodeGen
    try {
      codeGen
        .sourcesFromPulumiPackage(
          pulumiPackage,
          packageInfo
        )
        .foreach { sourceFile =>
          val filePath = outputDir / sourceFile.filePath.osSubPath
          os.makeDir.all(filePath / os.up)
          logger.debug(s"Writing source file: '${filePath.relativeTo(os.pwd)}'")
          os.write(filePath, sourceFile.sourceCode, createFolders = true)
        }
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
