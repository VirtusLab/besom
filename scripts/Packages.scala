//> using lib "com.lihaoyi::os-lib:0.9.1"

import java.util.Date
import scala.util.control.NonFatal

@main def run(args: String*) =
  args match
    case "generate-all" :: pluginsListPath :: Nil =>
      generateAll(os.Path(pluginsListPath, os.pwd))
    case "publish-local-all" :: pluginsListPath :: Nil =>
      publishLocalAll(os.Path(pluginsListPath, os.pwd))
    case "publish-maven-all" :: pluginsListPath :: Nil =>
      publishMavenAll(os.Path(pluginsListPath, os.pwd))

  def generateAll(pluginsListPath: os.Path): Unit =
    os.read.lines(pluginsListPath).filter(_.nonEmpty).foreach: line =>
      line match
        case s"package: ${packageName}, version: v${schemaVersion}, server: ${server}" =>
          println(s"Generating codebase for provider '${packageName}' version '${schemaVersion}'")
          println(new Date)
          try
            os.proc("time", "just", "generate-provider-sdk", packageName, schemaVersion).call()
            println(s"Successfully generate codebase for provider '${packageName}' version '${schemaVersion}'")
          catch
            case NonFatal(_) =>
              println(s"Code generation failed for provider '${packageName}' version '${schemaVersion}'")
          finally
            println()
        case _ =>
          println(s"Skipping package: ${line}")
          println()

  def publishLocalAll(pluginsListPath: os.Path): Unit =
    val logDir = os.pwd / ".out" / "publishLocal"
    os.read.lines(pluginsListPath).filter(_.nonEmpty).foreach: line =>
      line match
        case s"package: ${packageName}, version: v${schemaVersion}, server: ${server}" =>
          println(s"Publishing locally provider '${packageName}' version '${schemaVersion}'")
          println(new Date)
          try
            val logFile = logDir / s"${packageName}-${schemaVersion}.log"
            os.proc("time", "just", "publish-local-provider-sdk", packageName, schemaVersion).call(stdout = logFile, stderr = logFile)
            println(new Date)
            println(s"Successfully published provider '${packageName}' version '${schemaVersion}'")
          catch
            case NonFatal(_) =>
              println(new Date)
              println(s"Publish failed for provider '${packageName}' version '${schemaVersion}'")
          finally
            println()
        case _ =>
          println(s"Skipping package: ${line}")
          println()

  def publishMavenAll(pluginsListPath: os.Path): Unit =
    val logDir = os.pwd / ".out" / "publishMaven"
    os.read.lines(pluginsListPath).filter(_.nonEmpty).foreach: line =>
      line match
        case s"package: ${packageName}, version: v${schemaVersion}, server: ${server}" =>
          println(s"Publishing provider '${packageName}' version '${schemaVersion}'")
          println(new Date)
          try
            val logFile = logDir / s"${packageName}-${schemaVersion}.log"
            os.proc("time", "just", "publish-maven-provider-sdk", packageName, schemaVersion).call(stdout = logFile, stderr = logFile)
            println(new Date)
            println(s"Successfully published provider '${packageName}' version '${schemaVersion}'")
          catch
            case NonFatal(e) =>
              println(new Date)
              println(s"Publish failed for provider '${packageName}' version '${schemaVersion}'")
          finally
            println()
        case _ =>
          println(s"Skipping package: ${line}")
          println()
