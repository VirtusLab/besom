package besom.codegen

import besom.codegen.SchemaProvider.{ProviderName, SchemaVersion}

object Main {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case schemasDirPath :: outputDirBasePath :: providerName :: schemaVersion :: besomVersion :: Nil =>
        generatePackageSources(
          schemasDir = os.Path(schemasDirPath),
          codegenDir = os.Path(outputDirBasePath),
          providerName = providerName,
          schemaVersion = schemaVersion,
          besomVersion = besomVersion
        )
      case _ =>
        System.err.println(
          "Codegen's expected arguments: <schemasDirPath> <outputDirBasePath> <providerName> <schemaVersion> <besomVersion>"
        )
        sys.exit(1)
    }
  }

  // noinspection ScalaWeakerAccess
  def generatePackageSources(
    schemasDir: os.Path,
    codegenDir: os.Path,
    providerName: String,
    schemaVersion: String,
    besomVersion: String,
    preLoadSchemas: Map[(ProviderName, SchemaVersion), os.Path] = Map()
  ): os.Path = {
    implicit val providerConfig: Config.ProviderConfig = Config.providersConfigs(providerName)
    implicit val logger: Logger                        = new Logger

    val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = schemasDir)
    val outputDir      = codegenDir / providerName / schemaVersion

    // Print diagnostic information
    val preLoadInfo = if (preLoadSchemas.nonEmpty) {
      val preloadList = preLoadSchemas
        .map { case ((name, version), path) =>
          s"   - $name:$version -> ${path.relativeTo(os.pwd)}"
        }
        .mkString("\n")
      s"""| - Pre-load schemas:
          |$preloadList
          |""".stripMargin
    } else {
      ""
    }
    logger.info(
      s"""|Generating package '$providerName:$schemaVersion' into '${outputDir.relativeTo(os.pwd)}'
          | - Besom version   : $besomVersion
          | - Scala version   : ${CodeGen.scalaVersion}
          | - Java version    : ${CodeGen.javaVersion}
          |""".stripMargin + preLoadInfo
    )

    // Pre-load schemas from files if needed
    preLoadSchemas.foreach { case ((name, version), path) =>
      schemaProvider.addSchemaFile(name, version, path)
    }

    generatePackageSources(schemaProvider, outputDir, providerName, schemaVersion, besomVersion)
    logger.info(s"Finished generating provider '$providerName' codebase")

    outputDir
  }

  private def generatePackageSources(
    schemaProvider: SchemaProvider,
    outputDir: os.Path,
    providerName: String,
    schemaVersion: String,
    besomVersion: String
  )(implicit logger: Logger, providerConfig: Config.ProviderConfig): Unit = {
    val pulumiPackage = schemaProvider.pulumiPackage(providerName = providerName, schemaVersion = schemaVersion)
    logger.info(
      s"""|Loaded package: ${pulumiPackage.name} ${pulumiPackage.version.getOrElse("")}
          | - Resources: ${pulumiPackage.resources.size}
          | - Types    : ${pulumiPackage.types.size}
          | - Functions: ${pulumiPackage.functions.size}
          | - Config   : ${pulumiPackage.config.variables.size}
          |""".stripMargin
    )

    implicit val typeMapper: TypeMapper = new TypeMapper(
      defaultProviderName = providerName,
      defaultSchemaVersion = schemaVersion,
      schemaProvider = schemaProvider,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )

    // make sure we don't have a dirty state
    os.remove.all(outputDir)
    os.makeDir.all(outputDir)

    val codeGen = new CodeGen
    try {
      codeGen
        .sourcesFromPulumiPackage(
          pulumiPackage,
          schemaVersion = schemaVersion,
          besomVersion = besomVersion
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
