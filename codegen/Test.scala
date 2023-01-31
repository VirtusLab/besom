package besom.codegen

import java.util.Arrays
import java.io.File
import java.nio.file.Files

object Test {
  def main(args: Array[String]): Unit = {
    val filePath = "/tmp/k8s.json"
    val packagePrefix = "besom.api"
    val destinationDir = new File("./.codegen-out")

    val packageMappings = SchemaReader.readPackageMappings(filePath)
    val codeGen = new CodeGen(
      packagePrefix,
      packageMappings
    )

    val resourceSpecs = SchemaReader.readResources(filePath)
    destinationDir.delete()
    
    resourceSpecs
      .foreach{ res =>
        val (fileContent, relativePath) = codeGen.genForResource(res)
        val absolutePath = destinationDir.toPath.resolve(relativePath).toAbsolutePath.normalize
        absolutePath.getParent.toFile.mkdirs()
        Files.write(absolutePath, fileContent.getBytes)
      }
  }
}