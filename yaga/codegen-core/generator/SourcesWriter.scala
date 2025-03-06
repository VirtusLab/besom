package yaga.codegen.core.generator

class SourcesWriter:
  def writeSources(
    outputDir: os.Path,
    sources: Seq[SourceFile],
    summaryFile: Option[os.Path],
    cleanUpOutputDir: Boolean
  ): Unit =
    if cleanUpOutputDir then
      os.remove.all(outputDir)
    os.makeDir.all(outputDir)

    val writtenPaths = sources.map: sourceFile =>
      val filePath = outputDir / sourceFile.filePath.osSubPath
      os.makeDir.all(filePath / os.up)
      os.write(filePath, sourceFile.sourceCode, createFolders = true)
      filePath

    summaryFile.foreach { summaryFilePath =>
      os.write.over(summaryFilePath, writtenPaths.map(_.toString).mkString("\n"))
    }
