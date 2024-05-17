package besom.auto.internal

import besom.util.*
import os.CommandResult

import java.nio.channels.Channels
import java.util.concurrent.atomic.AtomicLong
import scala.io.Source
import scala.util.Using

object shell:
  case class Result private (
    command: Seq[String],
    exitCode: Int,
    out: String,
    err: String,
    envVars: Map[String, String]
  ):
    def asError: ShellAutoError = ShellAutoError(
      exitCode = exitCode,
      stdout = out,
      stderr = err,
      command = command,
      envVars = envVars
    )

  object Result:
    def from(result: os.CommandResult, envVars: Map[String, String]): Either[ShellAutoError, Result] =
      val res = Result(result.command, result.exitCode, result.out.text(), result.err.text(), envVars)
      if res.exitCode == 0 then Right(res) else Left(res.asError)
  end Result

  def apply(command: os.Shellable*)(opts: ShellOption*): Either[ShellAutoError, Result] =
    val options = ShellOptions.from(opts*)
    val result = os
      .proc(command*)
      .call(
        cwd = options.cwd.asOption.orNull,
        env = options.env,
        stdin = options.stdin,
        stdout = options.stdout,
        stderr = options.stderr,
        mergeErrIntoOut = options.mergeErrIntoOut,
        timeout = options.timeout,
        check = options.check,
        propagateEnv = options.propagateEnv
      )
    Result.from(result, options.env)
  end apply

  sealed trait ShellOption
  object ShellOption:
    /** the working directory of the subprocess */
    case class Cwd(path: os.Path) extends ShellOption

    /** any additional environment variables you wish to set in the subprocess */
    case class Env(env: Map[String, String]) extends ShellOption
    object Env:
      def apply(env: (String, String)*): Env     = new Env(env.toMap)
      def apply(key: String, value: String): Env = Env(Map(key -> value))

    /** Any data you wish to pass to the subprocess standard input. */
    case class Stdin(input: os.ProcessInput) extends ShellOption

    /** How the subprocess output stream is configured. */
    case class Stdout(output: os.ProcessOutput) extends ShellOption

    /** How the subprocess error stream is configured. */
    case class Stderr(output: os.ProcessOutput) extends ShellOption

    /** Whether to merge the subprocess error stream into its output stream. */
    case object MergeErrIntoOut extends ShellOption

    /** How long to wait for the subprocess to complete, in milliseconds. */
    case class Timeout(timeout: Long) extends ShellOption

    /** Whether to check the subprocess exit code and throw an exception if it is non-zero. Disable this to avoid throwing an exception if
      * the subprocess.
      */
    case object Check extends ShellOption

    /** Whether to propagate the current environment variables to the subprocess. Disable this to avoid passing in this parent process's
      * environment variables to the subprocess.
      */
    case object DontPropagateEnv extends ShellOption

  /** Options for the subprocess execution.
    * @param cwd
    *   the working directory of the subprocess
    * @param env
    *   any additional environment variables you wish to set in the subprocess
    * @param stdin
    *   any data you wish to pass to the subprocess standard input
    * @param stdout
    *   how the subprocess output stream is configured
    * @param stderr
    *   how the subprocess error stream is configured
    * @param mergeErrIntoOut
    *   whether to merge the subprocess error stream into its output stream
    * @param timeout
    *   how long to wait for the subprocess to complete, in milliseconds
    * @param check
    *   whether to check the subprocess exit code and throw an exception if it is non-zero
    * @param propagateEnv
    *   whether to propagate the current environment variables to the subprocess
    */
  case class ShellOptions(
    cwd: NotProvidedOr[os.Path] = NotProvided,
    env: Map[String, String] = Map.empty,
    stdin: os.ProcessInput = os.Pipe,
    stdout: os.ProcessOutput = os.Pipe,
    stderr: os.ProcessOutput = os.Pipe, // in contrast to os lib we default to Pipe, because we use our own error handling
    mergeErrIntoOut: Boolean = false,
    timeout: Long = -1,
    check: Boolean = false, // in contrast to os lib we default to false, because we use our own error handling
    propagateEnv: Boolean = true
  )

  object ShellOptions:
    def from(opts: ShellOption*): ShellOptions = from(opts.toList)
    def from(opts: List[ShellOption]): ShellOptions =
      opts match
        case ShellOption.Cwd(path) :: tail        => from(tail).copy(cwd = path)
        case ShellOption.Stdin(input) :: tail     => from(tail).copy(stdin = input)
        case ShellOption.Stdout(output) :: tail   => from(tail).copy(stdout = output)
        case ShellOption.Stderr(output) :: tail   => from(tail).copy(stderr = output)
        case ShellOption.MergeErrIntoOut :: tail  => from(tail).copy(mergeErrIntoOut = true)
        case ShellOption.Timeout(timeout) :: tail => from(tail).copy(timeout = timeout)
        case ShellOption.Check :: tail            => from(tail).copy(check = true)
        case ShellOption.DontPropagateEnv :: tail => from(tail).copy(propagateEnv = false)
        case ShellOption.Env(env) :: tail => {
          val old = from(tail*)
          old.copy(env = old.env ++ env)
        }
        case Nil => ShellOptions()
        case o   => throw AutoError(s"Unknown shell option: $o")

  def env(name: String): Either[Exception, String] =
    sys.env.get(name) match
      case Some(v) =>
        Option(v).filter(_.trim.nonEmpty) match
          case Some(value) => Right(value)
          case None        => Left(Exception(s"Environment variable $name is empty"))
      case None => Left(Exception(s"Environment variable $name is not set"))

  object pulumi:
    def ProjectFileName(ext: String = "yaml")                  = s"Pulumi.$ext"
    def StackFileName(stackName: String, ext: String = "yaml") = s"Pulumi.$stackName.$ext"

    object env:
      val PulumiHomeEnv                          = "PULUMI_HOME"
      val PulumiAutomationApiSkipVersionCheckEnv = "PULUMI_AUTOMATION_API_SKIP_VERSION_CHECK"
      val PulumiDebugCommandsEnv                 = "PULUMI_DEBUG_COMMANDS"
      val PulumiExperimentalEnv                  = "PULUMI_EXPERIMENTAL"
      val PulumiSkipUpdateCheckEnv               = "PULUMI_SKIP_UPDATE_CHECK"
      val PulumiAccessTokenEnv                   = "PULUMI_ACCESS_TOKEN"
      val PulumiConfigPassphraseEnv              = "PULUMI_CONFIG_PASSPHRASE"
      val PulumiConfigPassphraseFileEnv          = "PULUMI_CONFIG_PASSPHRASE_FILE"

      lazy val pulumiHome: Either[Exception, os.Path] =
        shell.env(PulumiHomeEnv).map(os.Path(_))
      lazy val pulumiAutomationApiSkipVersionCheck: Boolean =
        shell.env(PulumiAutomationApiSkipVersionCheckEnv).map(isTruthy).getOrElse(false)
      lazy val pulumiAccessToken: Either[Exception, String] =
        shell.env(PulumiAccessTokenEnv)

    end env

    // all commands should be run in non - interactive mode
    // this causes commands to fail rather than prompting for input (and thus hanging indefinitely)
    private val commonArgs: List[os.Shellable] = List("--non-interactive", "--logtostderr")
    private val commonOpts: List[ShellOption]  = List(ShellOption.Env(env.PulumiSkipUpdateCheckEnv -> "true"))

    def apply(additional: os.Shellable*)(opts: ShellOption*): Either[ShellAutoError, shell.Result] =
      shell("pulumi", commonArgs ++ additional)(commonOpts ++ opts*)

  end pulumi

  private def dummyLogger(key: String, value: Any): Unit = println(s"[$key]: $value")

  def watch(path: os.Path, onEvent: os.Path => Unit): Either[Exception, Unit] =
    Using(
      os.watch.watch(
        roots = Seq(path),
        onEvent = (ps: Set[os.Path]) => ps.filter(_ == path).foreach(onEvent),
        logger = dummyLogger /* TODO: pass a logger adapter here */
      ) // FIXME: throws SIGSEGV
    )(_ => ()).toEither.left.map(AutoError("Failed to watch a path", _))

  case class Tailer(path: os.Path)(onLine: String => Unit):
    private val lastPosition: AtomicLong = AtomicLong(0L)

    def tail: Either[Exception, Unit] =
      watch(
        path,
        onEvent = p => {
          val _ = lastPosition.getAndUpdate(pos => {
            val channel     = os.read.channel(p).position(pos)
            val inputStream = Channels.newInputStream(channel)
            Using(Source.fromInputStream(inputStream)) {
              _.getLines().foreach(onLine)
            }
            channel.position()
          })
        }
      )
  end Tailer

end shell
