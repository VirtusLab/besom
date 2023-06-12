package besom.codegen

import scala.collection.mutable.ListBuffer

class Logger {
  private val buffer = ListBuffer.empty[String]

  def warn(message: String): Unit =
    buffer.append(s"Warning: ${message}\n" )

  def writeToFile(file: os.Path): Unit =
    os.write(file, buffer)

  def nonEmpty = buffer.nonEmpty
}
