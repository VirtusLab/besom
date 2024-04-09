package besom.codegen

import scala.collection.mutable.ListBuffer

class Logger(val printLevel: Logger.Level) {
  import Logger.Level.*

  private val buffer     = ListBuffer.empty[String]
  private var errorCount = 0
  private var warnCount  = 0

  def error(message: String): Unit = {
    if (printLevel >= Error) println(s"Error: ${message}")
    buffer.append(s"Error: ${message}\n")
    errorCount += 1
  }

  def warn(message: String): Unit = {
    if (printLevel >= Warn) println(s"Warning: ${message}")
    buffer.append(s"Warning: ${message}\n")
    warnCount += 1
  }

  def info(message: String): Unit = {
    if (printLevel >= Info) println(message)
    buffer.append(s"Info: ${message}\n")
  }

  def debug(message: String): Unit = {
    if (printLevel >= Debug) println(message)
    buffer.append(s"Debug: ${message}\n")
  }

  def writeToFile(file: os.Path): Unit =
    os.write(file, buffer, createFolders = true)

  def hasWarnings: Boolean = warnCount > 0

  def hasErrors: Boolean = errorCount > 0
}

object Logger {
  def apply()(using config: Config) = new Logger(config.logLevel)

  sealed abstract class Level(val level: Int) extends Ordered[Level] {
    override def compare(that: Level): Int = level.compare(that.level)
  }

  // noinspection ScalaWeakerAccess
  object Level {
    case object Error extends Level(level = 0)
    case object Warn extends Level(level = 1)
    case object Info extends Level(level = 2)
    case object Debug extends Level(level = 3)
  }
}
