package besom.codegen

import java.util.Arrays

object Main {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case schemasDirPath :: outputDirBasePath :: providerName :: Nil =>
        generatePackageSources(
          schemasDirPath = os.Path(schemasDirPath),
          outputDirBasePath = os.Path(outputDirBasePath),
          providerName = providerName
        )
      case _ =>
        System.err.println("Codegen's expected arguments: <schemasDirPath> <outputDirBasePath> <providerName>")
        sys.exit(1)
    }
  }

  def generatePackageSources(schemasDirPath: os.Path, outputDirBasePath: os.Path, providerName: String): Unit = {
    val schemaFilePath = schemasDirPath / s"${providerName}.json"
    val destinationDir = outputDirBasePath / providerName

    println(s"Generating provider SDK for $providerName")

    os.remove.all(destinationDir)
    os.makeDir.all(destinationDir)

    val pulumiPackage = metaschema.PulumiPackage.fromFile(schemaFilePath)
    val providerConfig = Config.providersConfigs(providerName)

    implicit val logger: Logger = new Logger

    try {
      CodeGen.sourcesFromPulumiPackage(
        pulumiPackage,
        providerConfig,
        besomVersion = "0.0.1-SNAPSHOT"
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
