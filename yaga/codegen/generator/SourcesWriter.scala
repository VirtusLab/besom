package yaga.codegen.core.generator

class SourcesWriter:
  def writeSources(
    outputDir: os.Path,
    sources: Seq[SourceFile]
  ): Unit =
    os.remove.all(outputDir)
    os.makeDir.all(outputDir)

    sources.foreach: sourceFile =>
      val filePath = outputDir / sourceFile.filePath.osSubPath
      os.makeDir.all(filePath / os.up)
      scribe.debug(s"Writing source file: '${filePath.relativeTo(os.pwd)}'")
      try {
        os.write(filePath, sourceFile.sourceCode, createFolders = true)
      } catch {
        case e: java.nio.file.FileAlreadyExistsException =>
          // write the duplicate class for debugging purposes
          val fileDuplicate = filePath / os.up / s"${filePath.last}.duplicate"
          os.write(fileDuplicate, sourceFile.sourceCode, createFolders = true)
          val message = s"Duplicate file in codegen: ${fileDuplicate.relativeTo(os.pwd)} - \n${e.getMessage}"
          scribe.warn(message)
      }
