package besom.codegen

import java.util.Arrays
import java.io.File
import java.nio.file.Files

object Main {
  def main(args: Array[String]): Unit = {
    args.toList match {
      case schemasDirPath :: outputDirBasePath :: packageName :: Nil =>
        generatePackageSources(
          schemasDirPath = schemasDirPath,
          outputDirBasePath = outputDirBasePath,
          packageName = packageName
        )
      case _ =>
        System.err.println("Codegen's expected arguments: <schemasDirPath> <outputDirBasePath> <packageName>")
        sys.exit(1)
    }
  }

  def generatePackageSources(schemasDirPath: String, outputDirBasePath: String, packageName: String): Unit = {
    val schemaFilePath = s"${schemasDirPath}/${packageName}.json"
    val destinationDir = new File(s"${outputDirBasePath}/${packageName}")
    destinationDir.delete()

    val pulumiPackage = metaschema.PulumiPackage.fromFile(schemaFilePath)

    CodeGen.sourcesFromPulumiPackage(
      pulumiPackage,
      besomVersion = "0.0.1-SNAPSHOT"
    ).foreach { sourceFile =>
      val absolutePath = destinationDir.toPath.resolve(sourceFile.relativePath).toAbsolutePath.normalize
      absolutePath.getParent.toFile.mkdirs()
      Files.write(absolutePath, sourceFile.sourceCode.getBytes)
    }
  }
}