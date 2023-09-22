package besom.codegen

import java.util.Arrays

object Main {
  def main(args: Array[String]): Unit = {
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
    println(s"Generating provider SDK for $providerName")
    
    val schemaProvider = new SchemaProvider(schemaCacheDirPath = schemasDirPath)
    val destinationDir = outputDirBasePath / providerName / schemaVersion

    os.remove.all(destinationDir)
    os.makeDir.all(destinationDir)

    val pulumiPackage = schemaProvider.pulumiPackage(providerName = providerName, schemaVersion = schemaVersion)
    implicit val providerConfig = Config.providersConfigs(providerName)

    implicit val logger: Logger = new Logger

    implicit val typeMapper: TypeMapper = new TypeMapper(
      defaultProviderName = providerName,
      defaultSchemaVersion = schemaVersion,
      schemaProvider = schemaProvider,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )

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
}
