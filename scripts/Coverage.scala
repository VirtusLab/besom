//> using scala "3.3.0"

//> using lib "com.lihaoyi::os-lib:0.9.1"
//> using lib "org.scoverage::scalac-scoverage-reporter:2.0.10"
//> using lib "org.scoverage::scalac-scoverage-domain:2.0.10"
//> using lib "org.scoverage::scalac-scoverage-serializer:2.0.10"

import scoverage.domain.{ Constants, Coverage }
import scoverage.reporter.{ CoverageAggregator, IOUtils, ScoverageHtmlWriter, ScoverageXmlWriter }
import scoverage.serialize.Serializer
import java.io.File

given scala.util.CommandLineParser.FromString[File] with
  def fromString(str: String): File = File(str)

object Coverage:
  def main(args: Array[String]): Unit = args.toList match
    case "report" :: pathToSourceRoot :: reportsHTMLOutDir :: reportsXMLOutDir :: pathToCoverageData :: Nil =>
      report(File(pathToSourceRoot), File(pathToCoverageData), File(reportsHTMLOutDir), File(reportsXMLOutDir))
    case "report-module" :: modulePath :: Nil => // very specific to our project
      val pathToSourceRootPath = File(modulePath).toPath()
      val moduleName = pathToSourceRootPath.getFileName().toString()
      val besomDir = pathToSourceRootPath.getParent()
      val outDir = besomDir.resolve(".out")
      val coverageDir = outDir.resolve("coverage").resolve(moduleName)
      report(
        pathToSourceRootPath.toFile(),
        coverageDir.toFile(),
        outDir.resolve("scoverage-report").resolve(moduleName).toFile(),
        outDir.resolve("coverage-report").resolve(moduleName).toFile()
      )
    case "aggregate" :: pathToSourceRoot :: reportsHTMLOutDir :: reportsXMLOutDir :: pathsToCoverageData =>
      aggregate(
        File(pathToSourceRoot),
        File(reportsHTMLOutDir),
        File(reportsXMLOutDir),
        pathsToCoverageData.map(File(_))
      )
    case "aggregate-custom" :: pathToSourceRoot :: reportsHTMLOutDir :: reportsXMLOutDir :: paths =>
      val validPaths = paths.map(File(_)).sliding(2, 2).map {
          case List(sourcePath, dataPath) =>
            if sourcePath.exists() && dataPath.exists() then
              sourcePath -> dataPath
            else
              println(s"invalid path pair: $sourcePath $dataPath")
              sys.exit(1)
          case _ =>
            throw new Exception("paths must be in pairs: sourcePath dataPath")
        }.toList
      if !validPaths.isEmpty then
        aggregateCustom(
          File(pathToSourceRoot),
          validPaths,
          File(reportsHTMLOutDir),
          File(reportsXMLOutDir)
        )
      else
        println("no valid paths found, skipping")
        sys.exit(1)
    case _ =>
      println("usage: <command> <command-args>")

  def report(
    pathToSourceRoot: File,
    pathToCoverageData: File,
    reportsHTMLOutDir: File,
    reportsXMLOutDir: File
  ): Unit =
    val coverageFile = Serializer.coverageFile(pathToCoverageData)
    if coverageFile.exists then
      val coverage         = Serializer.deserialize(coverageFile, pathToSourceRoot)
      val measurementFiles = IOUtils.findMeasurementFiles(pathToCoverageData)
      val measurements     = IOUtils.invoked(measurementFiles)
      coverage.apply(measurements)

      writeReport(coverage, pathToSourceRoot, reportsHTMLOutDir, reportsXMLOutDir)
    else
      println("coverage file does not exist, skipping")
      sys.exit(1)

  // This is a custom way to aggregate coverage data
  def aggregateCustom(
    rootSourcePath: File,
    paths: List[(File, File)],
    reportsHTMLOutDir: File,
    reportsXMLOutDir: File
  ): Unit =
    val coverage = CustomCoverageAggregator.aggregate(paths)
    writeReport(coverage, rootSourcePath, reportsHTMLOutDir, reportsXMLOutDir)

  // This is the usual way aggregate is used in othe build tools.
  // The problem here is that scala-cli treats each subproject as
  // a separate project and so the source paths are local to the subproject.
  def aggregate(
    pathToSourceRoot: File,
    reportsHTMLOutDir: File,
    reportsXMLOutDir: File,
    pathsToCoverageData: List[File]
  ): Unit =
    CoverageAggregator.aggregate(pathsToCoverageData, pathToSourceRoot) match
      case Some(coverage) =>
        writeReport(coverage, pathToSourceRoot, reportsHTMLOutDir, reportsXMLOutDir)
      case _ =>
        println("no coverage data found, skipping")
        sys.exit(1)

  def writeReport(
    coverage: Coverage,
    pathToSourceRoot: File,
    reportsHTMLOutDir: File,
    reportsXMLOutDir: File
  ): Unit =
    println(s"Coverage percentage: ${coverage.statementCoverageFormatted}%")

    // Write out the HTML coverage report
    if (!reportsHTMLOutDir.exists()) reportsHTMLOutDir.mkdirs()
    val htmlWriter = new ScoverageHtmlWriter(pathToSourceRoot, reportsHTMLOutDir)
    htmlWriter.write(coverage)
    println("HTML coverage report written to " + reportsHTMLOutDir.getAbsolutePath)

    // Write out the XML coverage report
    if (!reportsXMLOutDir.exists()) reportsXMLOutDir.mkdirs()
    val xmlWriter = new ScoverageXmlWriter(pathToSourceRoot, reportsXMLOutDir, false, None)
    xmlWriter.write(coverage)
    println("XML coverage report written to " + reportsXMLOutDir.getAbsolutePath)

object CustomCoverageAggregator {
   def aggregate(paths: Seq[(File, File)]): Coverage = {
    var id = 0
    val coverage = new Coverage()
    paths foreach { (sourceDir, dataDir) =>
      val coverageFile: File = Serializer.coverageFile(dataDir)
      if (coverageFile.exists) {
        val subcoverage: Coverage =
          Serializer.deserialize(coverageFile, sourceDir)
        val measurementFiles: Array[File] =
          IOUtils.findMeasurementFiles(dataDir)
        val measurements = IOUtils.invoked(measurementFiles.toIndexedSeq)
        subcoverage.apply(measurements)
        subcoverage.statements foreach { stmt =>
          // need to ensure all the ids are unique otherwise the coverage object will have stmt collisions
          id = id + 1
          coverage add stmt.copy(id = id)
        }
      }
    }
    coverage
  }
}
