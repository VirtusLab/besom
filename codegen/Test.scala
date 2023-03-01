package besom.codegen

import java.util.Arrays
import java.io.File
import java.nio.file.Files

object Test {
  def main(args: Array[String]): Unit = {
    val packageName = "k8s"
    val filePath = s"/tmp/${packageName}.json"
    val packagePrefix = "besom.api"
    val destinationDir = new File(s"./.out/codegen/${packageName}")
    destinationDir.delete()

    val k8sPackage = metaschema.PulumiPackage.fromFile(filePath)

    CodeGen.sourcesFromPulumiPackage(k8sPackage).foreach { sourceFile =>
      val absolutePath = destinationDir.toPath.resolve(sourceFile.relativePath).toAbsolutePath.normalize
      absolutePath.getParent.toFile.mkdirs()
      Files.write(absolutePath, sourceFile.sourceCode.getBytes)
    }
  }
}