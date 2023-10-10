package besom.codegen

object Main {
  def main(args: Array[String]): Unit = {
    if (args(0) == "test")
      args.drop(1).toList match {
        case schemaPath :: outputPath :: providerName :: schemaVersion :: besomVersion :: Nil =>
          generateTestPackageSources(
            schemaPath = os.Path(schemaPath),
            outputPath = os.Path(outputPath),
            providerName = providerName,
            schemaVersion = schemaVersion,
            besomVersion = besomVersion
          )
          sys.exit(0)
        case _ =>
          System.err.println("Codegen's expected test arguments: <schemaPath> <outputPath> <providerName> <schemaVersion> <besomVersion>")
          sys.exit(1)
      }
    
    args.toList match {
      case schemasDirPath :: outputDirBasePath :: providerName :: schemaVersion :: besomVersion :: Nil =>
        generatePackageSources(
          schemasDirPath = os.Path(schemasDirPath),
          outputDirBasePath = os.Path(outputDirBasePath),
          providerName = providerName,
          schemaVersion = schemaVersion,
          besomVersion = besomVersion
        )
      case _ =>
        System.err.println("Codegen's expected arguments: <schemasDirPath> <outputDirBasePath> <providerName> <schemaVersion> <besomVersion>")
        sys.exit(1)
    }
  }

  def generatePackageSources(schemasDirPath: os.Path, outputDirBasePath: os.Path, providerName: String, schemaVersion: String, besomVersion: String): Unit = {
    val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = schemasDirPath)
    val destinationDir = outputDirBasePath / providerName / schemaVersion

    generatePackageSources(schemaProvider, destinationDir, providerName, schemaVersion, besomVersion)
  }

  def generatePackageSources(schemaProvider: SchemaProvider, destinationDir: os.Path, providerName: String, schemaVersion: String, besomVersion: String): Unit = {
    println(s"Generating provider SDK for $providerName")
 
    val pulumiPackage = schemaProvider.pulumiPackage(providerName = providerName, schemaVersion = schemaVersion)

    implicit val providerConfig: Config.ProviderConfig = Config.providersConfigs(providerName)
    implicit val logger: Logger = new Logger

    implicit val typeMapper: TypeMapper = new TypeMapper(
      defaultProviderName = providerName,
      defaultSchemaVersion = schemaVersion,
      schemaProvider = schemaProvider,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )

    // make sure we don't have a dirty state
    os.remove.all(destinationDir)
    os.makeDir.all(destinationDir)

    val codeGen = new CodeGen
    try {
      codeGen.sourcesFromPulumiPackage(
        pulumiPackage,
        schemaVersion = schemaVersion,
        besomVersion = besomVersion
      ).foreach { sourceFile =>
        val filePath = destinationDir / sourceFile.filePath.osSubPath
        os.makeDir.all(filePath / os.up)
        os.write(filePath, sourceFile.sourceCode)
      }
      println("Finished generating SDK codebase")
    } finally {
      if (logger.nonEmpty) {
        val logFile = destinationDir / ".codegen-log.txt"
        println(s"Some problems were encountered during the code generation. See ${logFile}")
        logger.writeToFile(logFile)
      }
    }
  }

  def generateTestPackageSources(schemaPath: os.Path, outputPath: os.Path, providerName: String, schemaVersion: String, besomVersion: String): Unit = {
    val schemaProvider = new TestSchemaProvider(schemaPath)
    generatePackageSources(schemaProvider, outputPath, providerName, schemaVersion, besomVersion)
  }
}
