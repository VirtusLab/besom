//> using scala "3.3.0"

//> using lib "com.lihaoyi::os-lib:0.9.1"
//> using lib "org.scoverage::scalac-scoverage-reporter:2.0.10"
//> using lib "org.scoverage::scalac-scoverage-domain:2.0.10"
//> using lib "org.scoverage::scalac-scoverage-serializer:2.0.10"
//> using buildInfo

import scoverage.reporter.CoberturaXmlWriter
import scoverage.domain.Constants
import scoverage.domain.Coverage
import scoverage.reporter.CoverageAggregator
import scoverage.reporter.IOUtils
import scoverage.reporter.ScoverageHtmlWriter
import scoverage.reporter.ScoverageXmlWriter
import scoverage.serialize.Serializer
import java.io.File

given scala.util.CommandLineParser.FromString[File] with
  def fromString(str: String): File = File(str)

object Coverage:
  def main(args: Array[String]): Unit =
    val command :: pathToSourceRoot :: pathToCoverageData :: pathToReports :: Nil = args.toList
    main(command, File(pathToSourceRoot), File(pathToCoverageData), File(pathToReports))

  def main(command: String, pathToSourceRoot: File, pathToCoverageData: File, pathToReports: File): Unit =
    command match
      case "report" =>
        println(s"source root: ${pathToSourceRoot.toPath().toAbsolutePath()}")
        println(s"coverage data: $pathToCoverageData")
        println(s"reports: $pathToReports")

        val coverageFile = Serializer.coverageFile(pathToCoverageData)
        if coverageFile.exists then
          val coverage         = Serializer.deserialize(coverageFile, pathToSourceRoot)
          val measurementFiles = IOUtils.findMeasurementFiles(pathToCoverageData)
          val measurements     = IOUtils.invoked(measurementFiles)
          coverage.apply(measurements)

          // new CoberturaXmlWriter()
        else
          println("coverage file does not exist, skipping")
          sys.exit(1)

      case "aggregate" => println("not implemented")

      case anyOther => println(s"unknown command: $anyOther, available commands: report, aggregate")
