package besom.auto

import scala.util.matching.Regex

@SerialVersionUID(1L)
sealed abstract class BaseAutoError(message: Option[String], cause: Option[Throwable])
    extends Exception(message.orElse(cause.map(_.toString)).orNull, cause.orNull)
    with Product
    with Serializable

@SerialVersionUID(1L)
case class AutoError(message: Option[String], cause: Option[Throwable]) extends BaseAutoError(message, cause)
object AutoError:
  def apply(message: String)                   = new AutoError(Some(message), None)
  def apply(message: String, cause: Throwable) = new AutoError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new AutoError(None, Some(cause))

@SerialVersionUID(1L)
case class ShellAutoError(
  message: Option[String],
  cause: Option[Throwable],
  exitCode: Int,
  stdout: String,
  stderr: String,
  command: Seq[String],
  envVars: Map[String, String]
) extends BaseAutoError(message, cause):
  def withMessage(message: String): ShellAutoError = copy(message = this.message.map(message + "; " + _).orElse(Some(message)))

  /** Returns true if the error was a result of selecting a stack that does not exist.
    *
    * @return
    *   `true` if the error was due to a non-existent stack.
    */
  def isSelectStack404Error: Boolean =
    val regex: Regex = "no stack named.*found".r
    regex.findFirstIn(stderr).isDefined

  /** Returns true if the error was a result of a conflicting update locking the stack.
    *
    * @return
    *   `true` if the error was due to a conflicting update.
    */
  def isConcurrentUpdateError: Boolean =
    val conflictText             = "[409] Conflict: Another update is currently in progress."
    val localBackendConflictText = "the stack is currently locked by"
    stderr.contains(conflictText) || stderr.contains(localBackendConflictText)

  /** Returns true if the error was a result of creating a stack that already exists.
    *
    * @return
    *   `true` if the error was due to a stack that already exists.
    */
  def isCreateStack409Error: Boolean =
    val regex: Regex = "stack.*already exists".r
    regex.findFirstIn(stderr).isDefined

  /** Returns true if the pulumi core engine encountered an error (most likely a bug).
    *
    * @return
    *   `true` if the error was due to an unexpected engine error.
    */
  def isUnexpectedEngineError: Boolean =
    stdout.contains("The Pulumi CLI encountered a fatal error. This is a bug!")

end ShellAutoError
object ShellAutoError:
  def apply(message: String, exitCode: Int, stdout: String, stderr: String, command: Seq[String], envVars: Map[String, String]) =
    new ShellAutoError(
      Some(msg(Some(message), None, exitCode, stdout, stderr, command, envVars)),
      None,
      exitCode,
      stdout,
      stderr,
      command,
      envVars
    )
  def apply(
    message: String,
    cause: Throwable,
    exitCode: Int,
    stdout: String,
    stderr: String,
    command: Seq[String],
    envVars: Map[String, String]
  ) =
    new ShellAutoError(
      Some(msg(Some(message), Some(cause), exitCode, stdout, stderr, command, envVars)),
      Some(cause),
      exitCode,
      stdout,
      stderr,
      command,
      envVars
    )
  def apply(cause: Throwable, exitCode: Int, stdout: String, stderr: String, command: Seq[String], envVars: Map[String, String]) =
    new ShellAutoError(
      Some(msg(None, Some(cause), exitCode, stdout, stderr, command, envVars)),
      Some(cause),
      exitCode,
      stdout,
      stderr,
      command,
      envVars
    )
  def apply(exitCode: Int, stdout: String, stderr: String, command: Seq[String], envVars: Map[String, String]) =
    new ShellAutoError(
      Some(msg(None, None, exitCode, stdout, stderr, command, envVars)),
      None,
      exitCode,
      stdout,
      stderr,
      command,
      envVars
    )

  private def msg(
    message: Option[String],
    cause: Option[Throwable],
    exitCode: Int,
    stdout: String,
    stderr: String,
    command: Seq[String],
    envVars: Map[String, String]
  ): String = {
    s"""|${message.map(_ + "\n").getOrElse("")}${cause.map("cause: " + _.getMessage + "\n").getOrElse("")}
        |command: ${redacted(command)}
        |code: $exitCode
        |stdout: 
        |$stdout
        |stderr: 
        |$stderr
        |env:
        |${envVars.map { case (k, v) => s"  $k=$v" }.mkString("\n")}
        |""".stripMargin
  }

  private def redacted(command: Seq[String]) =
    val parts = if command.contains("-secret") then command.take(2) :+ "...[REDACTED]" else command
    parts.mkString(" ")

end ShellAutoError
