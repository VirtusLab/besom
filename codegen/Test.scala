package besom.codegen

import java.util.Arrays
import java.io.File
import java.nio.file.Files

object Test {
  def main(args: Array[String]): Unit = {
    val filePath = "/tmp/k8s.json"
    val packagePrefix = "besom.api"
    val destinationDir = new File("./.codegen-out")
    destinationDir.delete()

    val k8sPackage = PulumiPackage.fromFile(filePath)

    CodeGen.sourcesFromPulumiPackage(k8sPackage).foreach { sourceFile =>
      val absolutePath = destinationDir.toPath.resolve(sourceFile.relativePath).toAbsolutePath.normalize
      absolutePath.getParent.toFile.mkdirs()
      Files.write(absolutePath, sourceFile.sourceCode.getBytes)
    }
  }
}