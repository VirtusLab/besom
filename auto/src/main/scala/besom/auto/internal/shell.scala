package besom.auto.internal

import besom.util.*

import scala.util.control.NonFatal

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

  /** Runs a command to completion.
    *
    * This is a faithful transcription of os-lib's `os.proc(...).call(...)` - which is itself `spawn` + collect + `join` - with one
    * addition: [[ShellOptions.onStart]] is handed a [[ChildProcess]] right after the subprocess is spawned, which is what makes
    * cancellation possible. The handler runs on the calling thread before the process is joined, so it must not block - stash the handle
    * and return. Exceptions it throws are swallowed; the command still runs to completion.
    */
  def apply(command: os.Shellable*)(opts: ShellOption*): Either[ShellAutoError, Result] =
    val options = ShellOptions.from(opts*)

    val chunks = new java.util.concurrent.ConcurrentLinkedQueue[Either[geny.Bytes, geny.Bytes]]

    val p = os.proc(command*)
    val sub = p.spawn(
      cwd = options.cwd.asOption.orNull,
      env = options.env,
      stdin = options.stdin,
      // a caller supplied ProcessOutput wins, exactly as in os-lib - we only collect when the stream is left at os.Pipe
      stdout =
        if options.stdout ne os.Pipe then options.stdout
        else os.ProcessOutput.ReadBytes((buf, n) => chunks.add(Left(new geny.Bytes(java.util.Arrays.copyOf(buf, n))))),
      stderr =
        if options.stderr ne os.Pipe then options.stderr
        else os.ProcessOutput.ReadBytes((buf, n) => chunks.add(Right(new geny.Bytes(java.util.Arrays.copyOf(buf, n))))),
      mergeErrIntoOut = options.mergeErrIntoOut,
      propagateEnv = options.propagateEnv
    )

    // a broken consumer must neither leak the process it was handed nor kill it - skipping the join below would leave a live
    // `pulumi up` running until JVM exit, and destroying it would leave the stack locked with a pending operation. Same contract
    // as the OnEvent handlers: a consumer's exception cannot break the command it is observing.
    try options.onStart.foreach(_(ChildProcess(sub)))
    catch case NonFatal(_) => ()

    sub.join(timeout = options.timeout, timeoutGracePeriod = 100)

    import scala.jdk.CollectionConverters.*
    val result = os.CommandResult(p.commandChunks, sub.exitCode(), chunks.iterator.asScala.toIndexedSeq)
    if result.exitCode != 0 && options.check then throw os.SubprocessException(result)

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

    /** A handler receiving a [[ChildProcess]] handle to the spawned subprocess, for callers that want to be able to cancel it.
      *
      * The handler runs on the calling thread right after the subprocess is spawned and before it is joined, so it must not block - stash
      * the handle and return. Exceptions it throws are swallowed: the command it was handed still runs to completion, the caller simply
      * ends up without a handle.
      */
    case class OnStart(handler: ChildProcess => Unit) extends ShellOption
  end ShellOption

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
    * @param onStart
    *   an optional handler receiving a handle to the spawned subprocess, invoked on the calling thread before the process is joined
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
    propagateEnv: Boolean = true,
    onStart: Option[ChildProcess => Unit] = None
  )

  /** Merge options, last specified value wins.
    *
    * [[ShellOption.Env]] is the exception: entries accumulate across occurrences and only a repeated key resolves to the last occurrence.
    */
  object ShellOptions:
    def from(opts: ShellOption*): ShellOptions = from(opts.toList)
    def from(opts: List[ShellOption]): ShellOptions =
      opts.foldLeft(ShellOptions()) { (acc, opt) =>
        opt match
          case ShellOption.Cwd(path)        => acc.copy(cwd = path)
          case ShellOption.Env(env)         => acc.copy(env = acc.env ++ env)
          case ShellOption.Stdin(input)     => acc.copy(stdin = input)
          case ShellOption.Stdout(output)   => acc.copy(stdout = output)
          case ShellOption.Stderr(output)   => acc.copy(stderr = output)
          case ShellOption.MergeErrIntoOut  => acc.copy(mergeErrIntoOut = true)
          case ShellOption.Timeout(timeout) => acc.copy(timeout = timeout)
          case ShellOption.Check            => acc.copy(check = true)
          case ShellOption.DontPropagateEnv => acc.copy(propagateEnv = false)
          case ShellOption.OnStart(handler) => acc.copy(onStart = Some(handler))
      }

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

  import ma.chinespirit.tailf.{Follower, Tail}

  /** Opens a file for tailing, in the `tail -f` sense - the returned [[Follower]] is an `InputStream` that blocks at the end of the file
    * instead of signalling EOF.
    *
    * The [[Follower]] itself is handed back rather than an iterator so that callers get `stop()`, which drains what is already written and
    * only then signals EOF, in addition to the abrupt `close()`.
    *
    * @param path
    *   the file to tail, which must already exist
    * @return
    *   the follower or an error if the file could not be opened
    */
  def tail(path: os.Path): Either[Exception, Follower] =
    Tail.follow(path.toIO).left.map(e => Exception(s"Failed to open $path for tailing", e))

end shell
