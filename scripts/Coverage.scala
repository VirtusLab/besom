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
    case _ =>
      println("usage: coverage <command> <command-args>")

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

  def writeReport(
    coverage: Coverage,
    pathToSourceRoot: File,
    reportsHTMLOutDir: File,
    reportsXMLOutDir: File
  ): Unit =
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
