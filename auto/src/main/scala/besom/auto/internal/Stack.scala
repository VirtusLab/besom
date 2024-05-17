package besom.auto.internal

import besom.internal.{LanguageRuntimeServer, LanguageRuntimeService}
import besom.json.*
import besom.util.*

import scala.util.Try

/** Stack is an isolated, independently configurable instance of a Pulumi program. Stack exposes methods for the full pulumi lifecycle
  * (up/preview/refresh/destroy), as well as managing configuration. Multiple Stacks are commonly used to denote different phases of
  * development (such as development, staging and production) or feature branches (such as feature-x-dev, jane-feature-x-dev).
  *
  * @param name
  *   the name identifying the stack
  * @param workspace
  *   the underlying [[Workspace]] backing the [[Stack]]
  */
case class Stack(name: String, workspace: Workspace):
  import besom.auto.internal.Stack.*

  protected[auto] def pulumi(additional: os.Shellable*)(opts: shell.ShellOption*): Either[ShellAutoError | AutoError, shell.Result] =
    for
      _ <-
        if workspace.remote then
          workspace
            .serializeArgsForOp(name)
            .left
            .map(e => AutoError("Failed to run command, error getting additional args", e))
        else Right(())

      result <- workspace.pulumi(additional)(opts*)

      _ <-
        if workspace.remote then
          workspace
            .postCommandCallback(name)
            .left
            .map(e => AutoError("Command ran successfully, but failed to run post command callback", e))
        else Right(())
    yield result
  end pulumi

  /** Edits the secrets provider for the stack.
    *
    * @see
    *   [[Workspace.changeStackSecretsProvider]]
    *
    * @param newSecretsProvider
    *   The new secrets provider.
    * @param options
    *   The options for changing the secrets provider.
    * @return
    *   Unit indicating success or failure.
    */
  def changeSecretsProvider(newSecretsProvider: String, options: ChangeSecretsProviderOption*): Either[Exception, Unit] =
    workspace.changeStackSecretsProvider(name, newSecretsProvider, options*)

  /** Preview preforms a dry-run update to a stack, returning pending changes.
    * @see
    *   [[https://www.pulumi.com/docs/reference/cli/pulumi_preview/]]
    * @param options
    *   the options for the preview operation
    * @return
    *   the preview result or an error if any
    */
  def preview(options: PreviewOption*): Either[Exception, PreviewResult] =
    val opts = PreviewOptions.from(options*)

    val sharedArgs: Seq[String] = opts.debugLogOpts.asArgs
      ++ opts.message.map(s"--message=" + _)
      ++ Option.when(opts.expectNoChanges)("--expect-no-changes")
      ++ Option.when(opts.diff)("--diff")
      ++ opts.replace.map("--replace=" + _)
      ++ opts.target.map("--target=" + _)
      ++ opts.policyPacks.map("--policy-pack=" + _)
      ++ opts.policyPackConfigs.map("--policy-pack-config=" + _)
      ++ Option.when(opts.targetDependents)("--target-dependents")
      ++ opts.parallel.filter(_ > 0).map(s"--parallel=" + _)
      ++ opts.userAgent.map(s"--exec-agent=" + _)
      ++ opts.color.map(s"--color=" + _)
      ++ opts.plan.map(s"--save-plan=" + _)
      ++ remoteArgs // Apply the remote args, if needed

    val kindArgs: Seq[String] =
      if workspace.program.isDefined then
        val address = startLanguageRuntimeServer()
        Seq(s"--exec-kind=${ExecKind.AutoInline}", s"--client=$address")
      else Seq(s"--exec-kind=${ExecKind.AutoLocal}")

    for
      tailerPath <- tailLogsPath("preview")
      // FIXME: tailer is borked
      /*tailer <- shell
        .Tailer(tailerPath) { line =>
          println(s"> $line")
        }
        .tail*/
      r <- {
        val watchArgs: Seq[String] = Seq("--event-log=" + tailerPath)
        val args: Seq[String]      = Seq("preview") ++ sharedArgs ++ kindArgs ++ watchArgs
        pulumi(args)().left.map(AutoError("Preview failed", _))
      }
      summary <-
        os.read
          .lines(tailerPath)
          .map(EngineEvent.fromJson(_))
          .collectFirst {
            case Right(ee) if ee.summaryEvent.isDefined => ee.summaryEvent.get
          }
          .toRight(AutoError("Failed to get preview summary, got no summary event"))
    yield PreviewResult(stdout = r.out, stderr = r.err, summary = summary.resourceChanges)
    end for
  end preview

  /** Create or update the resources in a stack by executing the program in the Workspace. Update updates the resources in a stack by
    * executing the program in the Workspace associated with this stack, if one is provided.
    * @see
    *   [[https://www.pulumi.com/docs/reference/cli/pulumi_up/]]
    * @param options
    *   the options for the update operation
    * @return
    *   the update result or an error if any
    */
  def up(options: UpOption*): Either[Exception, UpResult] =
    val opts = UpOptions.from(options*)

    val sharedArgs = opts.debugLogOpts.asArgs
      ++ opts.message.map(s"--message=" + _)
      ++ Option.when(opts.expectNoChanges)("--expect-no-changes")
      ++ Option.when(opts.diff)("--diff")
      ++ opts.replace.map("--replace=" + _)
      ++ opts.target.map("--target=" + _)
      ++ opts.policyPacks.map("--policy-pack=" + _)
      ++ opts.policyPackConfigs.map("--policy-pack-config=" + _)
      ++ Option.when(opts.targetDependents)("--target-dependents")
      ++ opts.parallel.filter(_ > 0).map(s"--parallel=" + _)
      ++ opts.userAgent.map(s"--exec-agent=" + _)
      ++ opts.color.map(s"--color=" + _)
      ++ opts.plan.map(s"--plan=" + _)
      ++ remoteArgs // Apply the remote args, if needed

    val kindArgs: Seq[String] =
      if workspace.program.isDefined then
        val address = startLanguageRuntimeServer()
        Seq(s"--exec-kind=${ExecKind.AutoInline}", s"--client=$address")
      else Seq(s"--exec-kind=${ExecKind.AutoLocal}")

    val watchArgs: Seq[String] = Seq.empty // TODO: missing event stream, implement watchArgs

    val args: Seq[String] = Seq("up", "--yes", "--skip-preview") ++ sharedArgs ++ kindArgs ++ watchArgs

    pulumi(args)(
// FIXME: missing streams, implement progressStreams and errorProgressStreams
//      shell.Option.Stdout(opts.progressStreams),
//      shell.Option.Stderr(opts.errorProgressStreams)
    ) match
      case Left(e) => Left(AutoError("Up failed", e))
      case Right(r) =>
        for
          outputs <- outputs
          history <- history(
            pageSize = 1,
            page = 1,
            /* If it's a remote workspace, don't set ShowSecrets to prevent attempting to load the project file. */
            Option.when(opts.showSecrets && !isRemote)(HistoryOption.ShowSecrets).toSeq*
          ).flatMap(_.headOption.toRight(AutoError("Failed to get history, result was empty")))
        yield UpResult(
          stdout = r.out,
          stderr = r.err,
          outputs = outputs,
          summary = history
        )
  end up

  /** Refresh compares the current stack’s resource state with the state known to exist in the actual cloud provider. Any such changes are
    * adopted into the current stack.
    *
    * @see
    *   [[https://www.pulumi.com/docs/reference/cli/pulumi_refresh/]]
    * @param options
    *   the options for the refresh operation
    * @return
    *   the refresh result or an error if any
    */
  def refresh(options: RefreshOption*): Either[Exception, RefreshResult] =
    val opts = RefreshOptions.from(options*)

    val sharedArgs = opts.debugLogOpts.asArgs
      ++ opts.message.map(s"--message=" + _)
      ++ Option.when(opts.expectNoChanges)("--expect-no-changes")
      ++ opts.target.map("--target=" + _)
      ++ opts.parallel.filter(_ > 0).map(s"--parallel=" + _)
      ++ opts.userAgent.map(s"--exec-agent=" + _)
      ++ opts.color.map(s"--color=" + _)
      ++ (if workspace.program.isDefined then Seq(s"--exec-kind=${ExecKind.AutoInline}") else Seq(s"--exec-kind=${ExecKind.AutoLocal}"))
      ++ remoteArgs // Apply the remote args, if needed

    val args: Seq[String] = Seq("refresh", "--yes", "--skip-preview") ++ sharedArgs
    pulumi(args)() match
      case Left(e) => Left(AutoError("Refresh failed", e))
      case Right(r) =>
        for history <- history(
            pageSize = 1,
            page = 1,
            /* If it's a remote workspace, don't set ShowSecrets to prevent attempting to load the project file. */
            Option.when(opts.showSecrets && !isRemote)(HistoryOption.ShowSecrets).toSeq*
          ).flatMap(_.headOption.toRight(AutoError("Failed to get history, result was empty")))
        yield RefreshResult(
          stdout = r.out,
          stderr = r.err,
          summary = history
        )
  end refresh

  /** Destroy deletes all resources in a stack, leaving all history and configuration intact.
    *
    * @see
    *   [[https://www.pulumi.com/docs/reference/cli/pulumi_destroy/]]
    * @param options
    *   the options for the destroy operation
    * @return
    *   the destroy result or an error if any
    */
  def destroy(options: DestroyOption*): Either[Exception, DestroyResult] =
    val opts = DestroyOptions.from(options*)

    val sharedArgs = opts.debugLogOpts.asArgs
      ++ opts.message.map(s"--message=" + _)
      ++ opts.target.map("--target=" + _)
      ++ Option.when(opts.targetDependents)("--target-dependents")
      ++ opts.parallel.filter(_ > 0).map(s"--parallel=" + _)
      ++ opts.userAgent.map(s"--exec-agent=" + _)
      ++ opts.color.map(s"--color=" + _)
      ++ (if workspace.program.isDefined then Seq(s"--exec-kind=${ExecKind.AutoInline}") else Seq(s"--exec-kind=${ExecKind.AutoLocal}"))
      ++ remoteArgs // Apply the remote args, if needed

    val args: Seq[String] = Seq("destroy", "--yes", "--skip-preview") ++ sharedArgs
    pulumi(args)() match
      case Left(e) => Left(AutoError("Destroy failed", e))
      case Right(r) =>
        for history <- history(
            pageSize = 1,
            page = 1,
            /* If it's a remote workspace, don't set ShowSecrets to prevent attempting to load the project file. */
            Option.when(opts.showSecrets && !isRemote)(HistoryOption.ShowSecrets).toSeq*
          ).flatMap(_.headOption.toRight(AutoError("Failed to get history, result was empty")))
        yield DestroyResult(
          stdout = r.out,
          stderr = r.err,
          summary = history
        )

  end destroy

  /** Get the current set of [[Stack]] outputs from the last [[Stack.up]]
    */
  def outputs: Either[Exception, OutputMap] = workspace.stackOutputs(name)

  /** History returns a list summarizing all previous and current results from [[Stack]] lifecycle operations (up/preview/refresh/destroy).
    *
    * @param pageSize
    *   used with 'page' to control number of results returned (e.g. 10)
    * @param page
    *   used with 'page-size' to paginate results (e.g. 1)
    * @param options
    *   the options for the history operation
    * @return
    *   the history result or an error if any
    */
  def history(pageSize: Int, page: Int, options: HistoryOption*): Either[Exception, List[UpdateSummary]] =
    val opts = HistoryOptions.from(options*)
    val args = Seq.empty[String]
      ++ Option.when(opts.showSecrets)("--show-secrets")
      ++ Option
        .when(pageSize > 0) {
          Seq(s"--page-size=$pageSize", s"--page=${if page < 1 then 1 else page}")
        }
        .toSeq
        .flatten

    pulumi("stack", "history", "--json", args)().fold(
      e => Left(AutoError("History failed", e)),
      r => UpdateSummary.fromJsonList(r.out).left.map(e => AutoError("Failed to parse history JSON", e))
    )
  end history

  /** Adds environments to the end of a stack's import list. Imported environments are merged in order per the ESC merge rules. The list of
    * environments behaves as if it were the import list in an anonymous environment.
    *
    * @param envs
    *   the environments to add
    * @return
    *   returns nothing if successful, or an error if any
    */
  def addEnvironments(envs: String*): Either[Exception, Unit] =
    workspace.addEnvironments(name, envs*)

  /** ListEnvironments returns the list of environments from the stack's configuration.
    *
    * @return
    *   the list of environments or an error if any
    */
  def listEnvironments: Either[Exception, List[String]] =
    workspace.listEnvironments(name)

  /** Removes an environment from a stack's configuration.
    *
    * @param env
    *   the environment to remove
    * @return
    *   nothing if successful, or an error if any
    */
  def removeEnvironment(env: String): Either[Exception, Unit] =
    workspace.removeEnvironment(name, env)

  /** Gets the config value associated with the specified key and the optional [[ConfigOption]]s.
    *
    * @param key
    *   the key of the config
    * @param opts
    *   the optional [[ConfigOption]]s
    * @return
    *   the [[ConfigValue]] or an error if any
    */
  def getConfig(key: String, opts: ConfigOption*): Either[Exception, ConfigValue] =
    workspace.getConfig(name, key, opts*)

  /** Gets the full config map.
    *
    * @return
    *   the ConfigMap or an error if any
    */
  def getAllConfig: Either[Exception, ConfigMap] =
    workspace.getAllConfig(name)

  /** Sets the specified config key-value pair using the optional [[ConfigOption]]s.
    *
    * @param key
    *   the key of the config
    * @param value
    *   the value to set
    * @return
    *   nothing if successful, or an error if any
    */
  def setConfig(key: String, value: ConfigValue, options: ConfigOption*): Either[Exception, Unit] =
    workspace.setConfig(name, key, value, options*)

  /** Sets all values in the provided config map using the optional [[ConfigOption]]s.
    *
    * @param config
    *   the config map to set.
    * @param opts
    *   the optional [[ConfigOption]]s.
    * @return
    *   nothing if successful, or an error if any
    */
  def setAllConfig(config: ConfigMap, opts: ConfigOption*): Either[Exception, Unit] =
    workspace.setAllConfig(name, config, opts*)

  /** Removes the specified config key-value pair using the optional [[ConfigOption]]s.
    *
    * @param key
    *   the key of the config to remove
    * @param opts
    *   the optional ConfigOptions
    * @return
    *   nothing if successful, or an error if any
    */
  def removeConfig(key: String, opts: ConfigOption*): Either[Exception, Unit] =
    workspace.removeConfig(name, key, opts*)

  /** Gets and sets the config map used with the last Update.
    * @return
    *   the ConfigMap or an error if any
    */
  def refreshConfig(): Either[Exception, ConfigMap] =
    workspace.refreshConfig(name)

  /** Gets the tag value associated with specified key.
    *
    * @param key
    *   whe key of the tag
    * @return
    *   the tag value or an error if any
    */
  def getTag(key: String): Either[Exception, String] =
    workspace.getTag(name, key)

  /** Sets a tag key-value pair on the stack.
    *
    * @param key
    *   the key of the tag
    * @param value
    *   the value of the tag
    * @return
    *   nothing if successful, or an error if any
    */
  def setTag(key: String, value: String): Either[Exception, Unit] =
    workspace.setTag(name, key, value)

  /** Removes the specified tag key-value pair from the stack.
    *
    * @param key
    *   the key of the tag to remove
    * @return
    *   nothing if successful, or an error if any
    */
  def removeTag(key: String): Either[Exception, Unit] =
    workspace.removeTag(name, key)

  /** Returns the full key-value tag map associated with the stack.
    *
    * @return
    *   the tag map or an error if any
    */
  def listTags: Either[Exception, Map[String, String]] =
    workspace.listTags(name)

  /** @return
    *   a summary of the Stack including its URL
    */
  def info: Either[Exception, Option[StackSummary]] =
    for
      _ <- workspace.selectStack(name)
      s <- workspace.stack
    yield s
  end info

  /** Cancels a stack's currently running update. It returns an error if no update is currently running. Note that this operation is _very
    * dangerous_, and may leave the stack in an inconsistent state if a resource operation was pending when the update was canceled. This
    * command is not supported for local backends.
    *
    * @return
    *   nothing if successful, or an error if any
    */
  def cancel(): Either[Exception, Unit] =
    pulumi("cancel", "--yes")().fold(
      e => Left(AutoError("Failed to cancel update", e)),
      _ => Right(())
    )
  end cancel

  /** Exports the deployment state of the stack. This can be combined with [[Stack.importState]] to edit a stack's state (such as recovery
    * from failed deployments).
    *
    * @return
    *   the deployment state of the stack or an error if any
    */
  def exportState(): Either[Exception, UntypedDeployment] =
    workspace.exportStack(name)

  /** Imports the specified deployment state into the stack. This can be combined with [[Stack.exportState]] to edit a stack's state (such
    * as recovery from failed deployments).
    *
    * @param state
    *   The deployment state to import into the stack.
    * @return
    *   nothing if successful, or an error if any
    */
  def importState(state: UntypedDeployment): Either[Exception, Unit] =
    workspace.importStack(name, state)

  /** @return
    *   returns true if the stack is remote
    */
  def isRemote: Boolean = workspace.remote

  private def remoteArgs: Seq[String] =
    def args(
      repo: Option[GitRepo],
      preRunCommands: List[String] = List.empty,
      remoteEnvVars: Map[String, EnvVarValue] = Map.empty,
      skipInstallDependencies: Boolean = false
    ): Seq[String] =
      repo.map(_.url).toSeq
        ++ repo.flatMap(_.branch.map("--remote-git-branch=" + _))
        ++ repo.flatMap(_.commitHash.map("--remote-git-commit=" + _))
        ++ repo.flatMap(_.projectPath.map("--remote-git-repo-dir=" + _))
        ++ repo.toSeq.flatMap(
          _.auth.asSeq.flatMap {
            case GitAuth.PersonalAccessToken(token) =>
              Seq(s"--remote-git-auth-access-token=$token")
            case GitAuth.SSHPrivateKey(key, passphrase) =>
              Seq(s"--remote-git-auth-ssh-private-key=$key") ++ passphrase.map("-remote-git-auth-password=" + _)
            case GitAuth.SSHPrivateKeyPath(path, passphrase) =>
              Seq(s"--remote-git-auth-ssh-private-key-path=$path") ++ passphrase.map("-remote-git-auth-password=" + _)
            case GitAuth.UsernameAndPassword(username, password) =>
              Seq(s"--remote-git-auth-username=$username", s"--remote-git-auth-password=$password")
          }
        )
        ++ preRunCommands.map("--remote-pre-run-command=" + _)
        ++ remoteEnvVars.map { case (k, v) => s"--remote-env${if v.secret then "-secret" else ""}=$k=${v.value}" }
        ++ Option.when(skipInstallDependencies)("--remote-skip-install-dependencies")

    workspace match
      case ws: LocalWorkspace =>
        if !ws.remote then Seq.empty
        else
          args(
            repo = ws.repo,
            preRunCommands = ws.preRunCommands,
            remoteEnvVars = ws.remoteEnvVars,
            skipInstallDependencies = ws.remoteSkipInstallDependencies
          )
      // TODO: implement RemoteWorkspace
      case null => throw AutoError("Workspace is null")
      case _    => throw AutoError(s"Unknown workspace type: ${workspace.getClass.getTypeName}")
  end remoteArgs

  private def tailLogsPath(command: String): Either[Exception, os.Path] =
    Try {
      val logsDir = os.temp.dir(prefix = s"automation-logs-$command-")
      val path    = logsDir / "eventlog.txt"
      os.write(path, "")
      os.exists(path) match
        case true  => println(os.proc("ls", "-la", path).call().out.text()) // FIXME: remove the println, use logger
        case false => throw AutoError(s"Failed to create event log file: $path")
      path
    }.toEither.left.map(e => AutoError("Failed to create temporary directory for event logs", e))
  end tailLogsPath

  private def startLanguageRuntimeServer(): String =
    import concurrent.ExecutionContext.Implicits.global // FIXME: use a custom execution context
    LanguageRuntimeServer(LanguageRuntimeService).start().toString
  end startLanguageRuntimeServer

end Stack

object Stack:
  /** A flag used to identify the origin of a command. */
  enum ExecKind(val value: String) extends Enum[ExecKind]:
    override def toString: String = value

    /** Command originating from automation API using a traditional Pulumi project. */
    case AutoLocal extends ExecKind("auto.local")

    /** Command originating from automation API using an inline Pulumi project.
      */
    case AutoInline extends ExecKind("auto.inline")

    /** Command originating from the CLI using a traditional Pulumi project.
      */
    case CLI extends ExecKind("cli")

  def apply(name: String, workspace: Workspace): Stack = new Stack(name, workspace)

  /** Creates a new [[Stack]] using the given [[Workspace]] and stack name. It fails if a stack with that name already exists.
    *
    * @param stackName
    *   the name of the stack
    * @param workspace
    *   the [[Workspace]] to use
    * @return
    *   the [[Stack]] or an error if any
    */
  def create(stackName: String, workspace: Workspace): Either[Exception, Stack] =
    workspace.createStack(stackName)

  /** Selects a [[Stack]] using the given [[Workspace]] and stack name. It returns an error if the given [[Stack]] does not exist.
    * @param stackName
    *   the name of the stack
    * @param workspace
    *   the [[Workspace]] to use
    * @return
    *   the [[Stack]] or an error if any
    */
  def select(stackName: String, workspace: Workspace): Either[Exception, Stack] =
    workspace.selectStack(stackName)

  /** Tries to select a [[Stack]] using the given [[Workspace]] and stack name, or falls back to trying to create the stack if it does not
    * exist.
    *
    * @param stackName
    *   the name of the stack
    * @param workspace
    *   the workspace to use
    * @return
    *   the [[Stack]] or an error if any
    */
  def upsert(stackName: String, workspace: Workspace): Either[Exception, Stack] = {
    select(stackName, workspace).fold(
      {
        // If the stack is not found, attempt to create it.
        case e: ShellAutoError if e.isSelectStack404Error => create(stackName, workspace)
        case err                                          => Left(err)
      },
      Right(_) // If the stack was found, return it.
    )
  }

end Stack

/** [[Stack.preview]] options
  * @see
  *   [[PreviewOptions]]
  */
sealed trait PreviewOption
object PreviewOption:

  /** The number of resource operations to run in parallel at once during the update (1 for no parallelism). Defaults to unbounded. (default
    * 2147483647)
    */
  case class Parallel(n: Int) extends PreviewOption

  /** A message to associate with the preview operation
    */
  case class Message(message: String) extends PreviewOption

  /** Will cause the preview to return an error if any changes occur
    */
  case object ExpectNoChanges extends PreviewOption

  /** Displays operation as a rich diff showing the overall change
    */
  case object Diff extends PreviewOption

  /** Specifies an array of resource URNs to explicitly replace during the preview
    */
  case class Replace(urns: String*) extends PreviewOption

  /** Specifies an exclusive list of resource URNs to update
    */
  case class Target(urns: String*) extends PreviewOption

  /** Allows updating of dependent targets discovered but not specified in the [[Target]] list
    */
  case object TargetDependents extends PreviewOption

  /** Provides options for verbose logging to standard error, and enabling plugin logs.
    */
  case class DebugLogging(debugOpts: LoggingOptions) extends PreviewOption

  // TODO: missing streams

  /** Allows specifying one or more Writers to redirect incremental preview stdout
    */
//  case class ProgressStreams(writers: os.ProcessOutput*) extends PreviewOption

  /** Allows specifying one or more Writers to redirect incremental preview stderr
    */
//  case class ErrorProgressStreams(writers: os.ProcessOutput*) extends PreviewOption

  /** Allows specifying one or more channels to receive the Pulumi event stream
    */
//  case class EventStreams(channels: EngineEvent*) extends PreviewOption

  /** Specifies the agent responsible for the update, stored in backends as "environment.exec.agent"
    */
  case class UserAgent(agent: String) extends PreviewOption

  /** Colorize output. Choices are: [[besom.auto.internal.Color.Always]], [[besom.auto.internal.Color.Never]],
    * [[besom.auto.internal.Color.Raw]], [[besom.auto.internal.Color.Auto]] (default "Auto")
    */
  case class Color(color: besom.auto.internal.Color) extends PreviewOption

  /** Saves an update plan to the given path.
    */
  case class Plan(path: os.Path) extends PreviewOption

  /** Runs one or more policy packs as part of this update
    */
  case class PolicyPacks(packs: String*) extends PreviewOption

  /** Path to a JSON file containing the config for the policy pack of the corresponding `--policy-pack` flag
    */
  case class PolicyPackConfigs(path: os.Path*) extends PreviewOption
end PreviewOption

/** [[Stack.preview]] options
  *
  * @see
  *   [[PreviewOption]]
  *
  * @param parallel
  *   the number of resource operations to run in parallel at once (1 for no parallelism). Defaults to unbounded. (default 2147483647)
  * @param message
  *   message (optional) to associate with the preview operation
  * @param expectNoChanges
  *   return an error if any changes occur during this preview
  * @param diff
  *   diff displays operation as a rich diff showing the overall change
  * @param replace
  *   Specify resources to replace
  * @param target
  *   specify an exclusive list of resource URNs to update
  * @param targetDependents
  *   allows updating of dependent targets discovered but not specified in the [[target]] list
  * @param debugLogOpts
  *   specifies additional settings for debug logging
  * @param progressStreams
  *   allows specifying one or more io.Writers to redirect incremental preview stdout
  * @param errorProgressStreams
  *   allows specifying one or more io.Writers to redirect incremental preview stderr
  * @param eventStreams
  *   allows specifying one or more channels to receive the Pulumi event stream
  * @param userAgent
  *   the agent responsible for the update, stored in backends as "environment.exec.agent"
  * @param color
  *   colorize output, choices are: always, never, raw, auto (default "auto")
  * @param plan
  *   save an update plan to the given path
  * @param policyPacks
  *   run one or more policy packs as part of this update
  * @param policyPackConfigs
  *   path to JSON file containing the config for the policy pack of the corresponding "--policy-pack" flag
  */
private[auto] case class PreviewOptions(
  parallel: NotProvidedOr[Int] = NotProvided,
  message: NotProvidedOr[String] = NotProvided,
  expectNoChanges: Boolean = false,
  diff: Boolean = false,
  replace: List[String] = List.empty,
  target: List[String] = List.empty,
  targetDependents: Boolean = false,
  debugLogOpts: LoggingOptions = LoggingOptions(),
//  progressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//  errorProgressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//  eventStreams: List[EngineEvent] = List.empty,  // TODO: implement EngineEvent
  userAgent: NotProvidedOr[String] = NotProvided,
  color: NotProvidedOr[Color] = NotProvided,
  plan: NotProvidedOr[os.Path] = NotProvided,
  policyPacks: List[String] = List.empty,
  policyPackConfigs: List[os.Path] = List.empty
)
object PreviewOptions:
  /** Merge options, last specified value wins
    *
    * @return
    *   a new [[PreviewOptions]]
    */
  def from(options: PreviewOption*): PreviewOptions = from(options.toList)
  def from(options: List[PreviewOption]): PreviewOptions =
    options match
      case PreviewOption.Parallel(n) :: tail             => from(tail*).copy(parallel = n)
      case PreviewOption.Message(m) :: tail              => from(tail*).copy(message = m)
      case PreviewOption.ExpectNoChanges :: tail         => from(tail*).copy(expectNoChanges = true)
      case PreviewOption.Diff :: tail                    => from(tail*).copy(diff = true)
      case PreviewOption.Replace(urns*) :: tail          => from(tail*).copy(replace = urns.toList)
      case PreviewOption.Target(urns*) :: tail           => from(tail*).copy(target = urns.toList)
      case PreviewOption.TargetDependents :: tail        => from(tail*).copy(targetDependents = true)
      case PreviewOption.DebugLogging(debugOpts) :: tail => from(tail*).copy(debugLogOpts = debugOpts)
// TODO: missing streams
//      case PreviewOption.ProgressStreams(writers) :: tail      => from(tail*).copy(progressStreams = writers)
//      case PreviewOption.ErrorProgressStreams(writers) :: tail => from(tail*).copy(errorProgressStreams = writers)
//      case PreviewOption.EventStreams(channels) :: tail        => from(tail*).copy(eventStreams = channels)
      case PreviewOption.UserAgent(agent) :: tail          => from(tail*).copy(userAgent = agent)
      case PreviewOption.Color(color) :: tail              => from(tail*).copy(color = color)
      case PreviewOption.Plan(path) :: tail                => from(tail*).copy(plan = path)
      case PreviewOption.PolicyPacks(packs*) :: tail       => from(tail*).copy(policyPacks = packs.toList)
      case PreviewOption.PolicyPackConfigs(paths*) :: tail => from(tail*).copy(policyPackConfigs = paths.toList)
      case Nil                                             => PreviewOptions()
      case o                                               => throw AutoError(s"Unknown preview option: $o")

end PreviewOptions

/** [[Stack.up]] options
  *
  * @see
  *   [[UpOption]]
  */
sealed trait UpOption
object UpOption:
  /** The number of resource operations to run in parallel at once during the update (1 for no parallelism). Defaults to unbounded. (default
    * 2147483647)
    */
  case class Parallel(n: Int) extends UpOption

  /** Message (optional) to associate with the update operation
    */
  case class Message(message: String) extends UpOption

  /** Will cause the update to return an error if any changes occur
    */
  case object ExpectNoChanges extends UpOption

  /** Displays operation as a rich diff showing the overall change
    */
  case object Diff extends UpOption

  /** Specifies an array of resource URNs to explicitly replace during the update
    */
  case class Replace(urns: String*) extends UpOption

  /** Specifies an exclusive list of resource URNs to update
    */
  case class Target(urns: String*) extends UpOption

  /** Allows updating of dependent targets discovered but not specified in the [[Target]] list
    */
  case object TargetDependents extends UpOption

  /** Specifies additional settings for debug logging
    */
  case class DebugLogging(debugOpts: LoggingOptions) extends UpOption

  // TODO: missing streams

  /** Allows specifying one or more io.Writers to redirect incremental update stdout
    */
  //  case class ProgressStreams(writers: os.ProcessOutput*) extends UpOption
  /** Allows specifying one or more io.Writers to redirect incremental update stderr
    */
//  case class ErrorProgressStreams(writers: os.ProcessOutput*) extends UpOption
  /** Allows specifying one or more channels to receive the Pulumi event stream
    */
// case class EventStreams(channels: EngineEvent*) extends UpOption

  /** Specifies the agent responsible for the update, stored in backends as "environment.exec.agent"
    */
  case class UserAgent(agent: String) extends UpOption

  /** Colorize output. Choices are: [[besom.auto.internal.Color.Always]], [[besom.auto.internal.Color.Never]],
    * [[besom.auto.internal.Color.Raw]],
    */
  case class Color(color: besom.auto.internal.Color) extends UpOption

  /** Use the update plan at the given path.
    */
  case class Plan(path: os.Path) extends UpOption

  /** Run one or more policy packs as part of this update
    */
  case class PolicyPacks(packs: String*) extends UpOption

  /** Path to a JSON file containing the config for the policy pack of the corresponding `--policy-pack` flag
    */
  case class PolicyPackConfigs(path: os.Path*) extends UpOption

  /** Show config secrets when they appear.
    */
  case object ShowSecrets extends UpOption
end UpOption

/** [[Stack.up]] options
  *
  * @see
  *   [[UpOption]]
  *
  * @param parallel
  *   the number of resource operations to run in parallel at once (1 for no parallelism). Defaults to unbounded. (default 2147483647)
  * @param message
  *   message (optional) to associate with the preview operation
  * @param expectNoChanges
  *   return an error if any changes occur during this preview
  * @param diff
  *   diff displays operation as a rich diff showing the overall change
  * @param replace
  *   specify resources to replace
  * @param target
  *   specify an exclusive list of resource URNs to update
  * @param targetDependents
  *   allows updating of dependent targets discovered but not specified in the Target list
  * @param debugLogOpts
  *   specifies additional settings for debug logging
  * @param progressStreams
  *   allows specifying one or more io.Writers to redirect incremental preview stdout
  * @param errorProgressStreams
  *   allows specifying one or more io.Writers to redirect incremental preview stderr
  * @param eventStreams
  *   allows specifying one or more channels to receive the Pulumi event stream
  * @param userAgent
  *   specifies the agent responsible for the update, stored in backends as "environment.exec.agent"
  * @param color
  *   colorize output, choices are: always, never, raw, auto (default "auto")
  * @param plan
  *   save an update plan to the given path
  * @param policyPacks
  *   run one or more policy packs as part of this update
  * @param policyPackConfigs
  *   path to JSON file containing the config for the policy pack of the corresponding "--policy-pack" flag
  * @param showSecrets
  *   show config secrets when they appear
  */
case class UpOptions(
  parallel: NotProvidedOr[Int] = NotProvided,
  message: NotProvidedOr[String] = NotProvided,
  expectNoChanges: Boolean = false,
  diff: Boolean = false,
  replace: List[String] = List.empty,
  target: List[String] = List.empty,
  targetDependents: Boolean = false,
  debugLogOpts: LoggingOptions = LoggingOptions(),
//  progressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//  errorProgressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//  eventStreams: List[EngineEvent] = List.empty,  // TODO: implement EngineEvent
  userAgent: NotProvidedOr[String] = NotProvided,
  color: NotProvidedOr[Color] = NotProvided,
  plan: NotProvidedOr[os.Path] = NotProvided,
  policyPacks: List[String] = List.empty,
  policyPackConfigs: List[os.Path] = List.empty,
  showSecrets: Boolean = false
)
object UpOptions:
  def from(options: UpOption*): UpOptions = from(options.toList)
  def from(options: List[UpOption]): UpOptions =
    options match
      case UpOption.Parallel(n) :: tail             => from(tail*).copy(parallel = n)
      case UpOption.Message(m) :: tail              => from(tail*).copy(message = m)
      case UpOption.ExpectNoChanges :: tail         => from(tail*).copy(expectNoChanges = true)
      case UpOption.Diff :: tail                    => from(tail*).copy(diff = true)
      case UpOption.Replace(urns*) :: tail          => from(tail*).copy(replace = urns.toList)
      case UpOption.Target(urns*) :: tail           => from(tail*).copy(target = urns.toList)
      case UpOption.TargetDependents :: tail        => from(tail*).copy(targetDependents = true)
      case UpOption.DebugLogging(debugOpts) :: tail => from(tail*).copy(debugLogOpts = debugOpts)
// TODO: missing streams
//      case UpOption.ProgressStreams(writers) :: tail      => from(tail*).copy(progressStreams = writers)
//      case UpOption.ErrorProgressStreams(writers) :: tail => from(tail*).copy(errorProgressStreams = writers)
//      case UpOption.EventStreams(channels) :: tail        => from(tail*).copy(eventStreams = channels)
      case UpOption.UserAgent(agent) :: tail          => from(tail*).copy(userAgent = agent)
      case UpOption.Color(color) :: tail              => from(tail*).copy(color = color)
      case UpOption.Plan(path) :: tail                => from(tail*).copy(plan = path)
      case UpOption.PolicyPacks(packs*) :: tail       => from(tail*).copy(policyPacks = packs.toList)
      case UpOption.PolicyPackConfigs(paths*) :: tail => from(tail*).copy(policyPackConfigs = paths.toList)
      case UpOption.ShowSecrets :: tail               => from(tail*).copy(showSecrets = true)
      case Nil                                        => UpOptions()
      case o                                          => throw AutoError(s"Unknown up option: $o")

end UpOptions

/** [[Stack.refresh]] options
  *
  * @see
  *   [[RefreshOption]]
  */
sealed trait RefreshOption
object RefreshOption:

  /** the number of resource operations to run in parallel at once (1 for no parallelism). Defaults to unbounded. (default 2147483647)
    */
  case class Parallel(n: Int) extends RefreshOption

  /** Message (optional) to associate with the refresh operation
    */
  case class Message(message: String) extends RefreshOption

  /** Will cause the refresh to return an error if any changes occur
    */
  case object ExpectNoChanges extends RefreshOption

  /** Specifies an array of resource URNs to explicitly refresh
    */
  case class Target(urns: String*) extends RefreshOption

  // TODO: missing streams

  /** Allows specifying one or more io.Writers to redirect incremental refresh stdout
    */
//  case class ProgressStreams(writers: os.ProcessOutput*) extends RefreshOption
  /** Allows specifying one or more io.Writers to redirect incremental refresh stderr
    */
//  case class ErrorProgressStreams(writers: os.ProcessOutput*) extends RefreshOption
  /** Allows specifying one or more channels to receive the Pulumi event stream
    */
// case class EventStreams(channels: EngineEvent*) extends RefreshOption

  /** Specifies additional settings for debug logging
    */
  case class DebugLogging(debugOpts: LoggingOptions) extends RefreshOption

  /** Specifies the agent responsible for the refresh, stored in backends as "environment.exec.agent"
    */
  case class UserAgent(agent: String) extends RefreshOption

  /** Colorize output. Choices are: [[besom.auto.internal.Color.Always]], [[besom.auto.internal.Color.Never]],
    * [[besom.auto.internal.Color.Raw]],
    */
  case class Color(color: besom.auto.internal.Color) extends RefreshOption

  /** Show config secrets when they appear.
    */
  case object ShowSecrets extends RefreshOption

end RefreshOption

/** [[Stack.refresh]] options
  *
  * @see
  *   [[RefreshOption]]
  *
  * @param parallel
  *   the number of resource operations to run in parallel at once (1 for no parallelism). Defaults to unbounded. (default 2147483647)
  * @param message
  *   message (optional) to associate with the refresh operation
  * @param expectNoChanges
  *   return an error if any changes occur during this refresh
  * @param target
  *   specify an exclusive list of resource URNs to update
  * @param debugLogOpts
  *   specifies additional settings for debug logging
  * @param progressStreams
  *   allows specifying one or more io.Writers to redirect incremental refresh stdout
  * @param errorProgressStreams
  *   allows specifying one or more io.Writers to redirect incremental refresh stderr
  * @param eventStreams
  *   allows specifying one or more channels to receive the Pulumi event stream
  * @param userAgent
  *   specifies the agent responsible for the refresh, stored in backends as "environment.exec.agent"
  * @param color
  *   colorize output, choices are: always, never, raw, auto (default "auto")
  * @param showSecrets
  *   show config secrets when they appear
  */
case class RefreshOptions(
  parallel: NotProvidedOr[Int] = NotProvided,
  message: NotProvidedOr[String] = NotProvided,
  expectNoChanges: Boolean = false,
  target: List[String] = List.empty,
  debugLogOpts: LoggingOptions = LoggingOptions(),
//    progressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//    errorProgressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//    eventStreams: List[EngineEvent] = List.empty,  // TODO: implement EngineEvent
  userAgent: NotProvidedOr[String] = NotProvided,
  color: NotProvidedOr[Color] = NotProvided,
  showSecrets: Boolean = false
)
object RefreshOptions:
  def from(options: RefreshOption*): RefreshOptions = from(options.toList)
  def from(options: List[RefreshOption]): RefreshOptions =
    options match
      case RefreshOption.Parallel(n) :: tail             => from(tail*).copy(parallel = n)
      case RefreshOption.Message(m) :: tail              => from(tail*).copy(message = m)
      case RefreshOption.ExpectNoChanges :: tail         => from(tail*).copy(expectNoChanges = true)
      case RefreshOption.Target(urns*) :: tail           => from(tail*).copy(target = urns.toList)
      case RefreshOption.DebugLogging(debugOpts) :: tail => from(tail*).copy(debugLogOpts = debugOpts)
// TODO: missing streams
//      case RefreshOption.ProgressStreams(writers) :: tail      => from(tail*).copy(progressStreams = writers)
//      case RefreshOption.ErrorProgressStreams(writers) :: tail => from(tail*).copy(errorProgressStreams = writers)
//      case RefreshOption.EventStreams(channels) :: tail        => from(tail*).copy(eventStreams = channels)
      case RefreshOption.UserAgent(agent) :: tail => from(tail*).copy(userAgent = agent)
      case RefreshOption.Color(color) :: tail     => from(tail*).copy(color = color)
      case RefreshOption.ShowSecrets :: tail      => from(tail*).copy(showSecrets = true)
      case Nil                                    => RefreshOptions()
      case o                                      => throw AutoError(s"Unknown refresh option: $o")

end RefreshOptions

/** [[Stack.destroy]] options
  *
  * @see
  *   [[DestroyOption]]
  */
sealed trait DestroyOption
object DestroyOption:

  /** the number of resource operations to run in parallel at once (1 for no parallelism). Defaults to unbounded. (default 2147483647)
    */
  case class Parallel(n: Int) extends DestroyOption

  /** Message (optional) to associate with the destroy operation
    */
  case class Message(message: String) extends DestroyOption

  /** Specify exclusive list of resource URNs to destroy
    */
  case class Target(urns: String*) extends DestroyOption

  /** Allows deleting of dependent targets discovered but not specified in the [[Target]] list
    */
  case object TargetDependents extends DestroyOption

  /** Allows specifying one or more io.Writers to redirect incremental destroy stdout
    */
//  case class ProgressStreams(writers: os.ProcessOutput*) extends DestroyOption
  /** Allows specifying one or more io.Writers to redirect incremental destroy stderr
    */
//  case class ErrorProgressStreams(writers: os.ProcessOutput*) extends DestroyOption
  /** Allows specifying one or more channels to receive the Pulumi event stream
    */
// case class EventStreams(channels: EngineEvent*) extends DestroyOption

  /** Specifies additional settings for debug logging
    */
  case class DebugLogging(debugOpts: LoggingOptions) extends DestroyOption

  /** Specifies the agent responsible for the destroy, stored in backends as "environment.exec.agent"
    */
  case class UserAgent(agent: String) extends DestroyOption

  /** Colorize output. Choices are: [[besom.auto.internal.Color.Always]], [[besom.auto.internal.Color.Never]],
    * [[besom.auto.internal.Color.Raw]],
    */
  case class Color(color: besom.auto.internal.Color) extends DestroyOption

  /** Show config secrets when they appear.
    */
  case object ShowSecrets extends DestroyOption

end DestroyOption

/** [[Stack.destroy]] options
  *
  * @see
  *   [[DestroyOption]]
  *
  * @param parallel
  *   the number of resource operations to run in parallel at once (1 for no parallelism). Defaults to unbounded. (default 2147483647)
  * @param message
  *   message (optional) to associate with the destroy operation
  * @param target
  *   specify an exclusive list of resource URNs to update
  * @param targetDependents
  *   allows deleting of dependent targets discovered but not specified in the [[target]] list
  * @param debugLogOpts
  *   specifies additional settings for debug logging
  * @param progressStreams
  *   allows specifying one or more io.Writers to redirect incremental destroy stdout
  * @param errorProgressStreams
  *   allows specifying one or more io.Writers to redirect incremental destroy stderr
  * @param eventStreams
  *   allows specifying one or more channels to receive the Pulumi event stream
  * @param userAgent
  *   specifies the agent responsible for the destroy, stored in backends as "environment.exec.agent"
  * @param color
  *   colorize output, choices are: always, never, raw, auto (default "auto")
  * @param showSecrets
  *   show config secrets when they appear
  */
case class DestroyOptions(
  parallel: NotProvidedOr[Int] = NotProvided,
  message: NotProvidedOr[String] = NotProvided,
  target: List[String] = List.empty,
  targetDependents: Boolean = false,
  debugLogOpts: LoggingOptions = LoggingOptions(),
//    progressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//    errorProgressStreams: List[os.ProcessOutput] = List.empty, // TODO: implement multiple writers
//    eventStreams: List[EngineEvent] = List.empty,  // TODO: implement EngineEvent
  userAgent: NotProvidedOr[String] = NotProvided,
  color: NotProvidedOr[Color] = NotProvided,
  showSecrets: Boolean = false
)
object DestroyOptions:
  def from(options: DestroyOption*): DestroyOptions = from(options.toList)
  def from(options: List[DestroyOption]): DestroyOptions =
    options match
      case DestroyOption.Parallel(n) :: tail             => from(tail*).copy(parallel = n)
      case DestroyOption.Message(m) :: tail              => from(tail*).copy(message = m)
      case DestroyOption.Target(urns*) :: tail           => from(tail*).copy(target = urns.toList)
      case DestroyOption.TargetDependents :: tail        => from(tail*).copy(targetDependents = true)
      case DestroyOption.DebugLogging(debugOpts) :: tail => from(tail*).copy(debugLogOpts = debugOpts)
//      case DestroyOption.ProgressStreams(writers) :: tail      => from(tail*).copy(progressStreams = writers)
//      case DestroyOption.ErrorProgressStreams(writers) :: tail => from(tail*).copy(errorProgressStreams = writers)
//      case DestroyOption.EventStreams(channels) :: tail        => from(tail*).copy(eventStreams = channels)
      case DestroyOption.UserAgent(agent) :: tail => from(tail*).copy(userAgent = agent)
      case DestroyOption.Color(color) :: tail     => from(tail*).copy(color = color)
      case DestroyOption.ShowSecrets :: tail      => from(tail*).copy(showSecrets = true)
      case Nil                                    => DestroyOptions()
      case o                                      => throw AutoError(s"Unknown destroy option: $o")

end DestroyOptions

/** [[Stack.history]] options
  *
  * @see
  *   [[HistoryOption]]
  */
sealed trait HistoryOption
object HistoryOption:

  /** Show config secrets when they appear.
    */
  case object ShowSecrets extends HistoryOption

end HistoryOption

/** [[Stack.history]] options
  *
  * @see
  *   [[HistoryOption]]
  *
  * @param showSecrets
  *   show config secrets when they appear
  */
case class HistoryOptions(
  showSecrets: Boolean = false
)
object HistoryOptions:
  def from(options: HistoryOption*): HistoryOptions = from(options.toList)
  def from(options: List[HistoryOption]): HistoryOptions =
    options match
      case HistoryOption.ShowSecrets :: tail => from(tail*).copy(showSecrets = true)
      case Nil                               => HistoryOptions()
      case null                              => throw AutoError(s"Unexpected null history option")

end HistoryOptions

/** Options for verbose logging to standard error, and enabling plugin logs.
  *
  * Note - These logs may include sensitive information that is provided from your execution environment to your cloud provider (and which
  * Pulumi may not even itself be aware of). These options should be used with care.
  *
  * @param logLevel
  *   choose verbosity level of at least 1 (least verbose), if not specified, reverts to default log level
  * @param logToStdErr
  *   specifies that all logs should be sent directly to stderr - making it more accessible and avoiding OS level buffering
  * @param flowToPlugins
  *   reflects the logging settings to plugins as well as the CLI. This is useful when debugging a plugin
  * @param tracing
  *   emit tracing to the specified endpoint. Use the `file:`` schema to write tracing data to a local file
  * @param debug
  *   print detailed debugging output during resource operations
  */
case class LoggingOptions(
  logLevel: NotProvidedOr[Int] = NotProvided,
  logToStdErr: Boolean = false,
  flowToPlugins: Boolean = false,
  tracing: NotProvidedOr[String] = NotProvided,
  debug: Boolean = false
):
  private[auto] def asArgs: Seq[String] =
    Seq(
      logLevel.map(lvl => if lvl < 0 then 1 else lvl).map(lvl => s"-v=$lvl"),
      Option.when(logToStdErr)("--logtostderr"),
      Option.when(flowToPlugins)("--logflow"),
      tracing.map(t => s"--tracing=$t"),
      Option.when(debug)("--debug")
    ).flatten

/** Colorize output. Choices are: [[besom.auto.internal.Color.Always]], [[besom.auto.internal.Color.Never]],
  * [[besom.auto.internal.Color.Raw]], [[besom.auto.internal.Color.Auto]] (default "Auto")
  */
enum Color(val value: String):
  override def toString: String = value

  /** Always colorize output */
  case Always extends Color("always")

  /** Never colorize output.
    */
  case Never extends Color("never")

  /** Raw colorize output.
    */
  case Raw extends Color("raw")

  /** Automatically colorize output (default).
    */
  case Auto extends Color("auto")
end Color

/** PreviewResult is the output of [[Stack.preview]] describing the expected set of changes from the next [[Stack.up]] operation.
  * @param stdout
  *   standard output
  * @param stderr
  *   standard error
  * @param summary
  *   the expected changes
  */
case class PreviewResult(
  stdout: String,
  stderr: String,
  summary: Map[OpType, Int]
):
  def permalink: Either[Exception, String] = ??? // TODO: implement GetPermalink
end PreviewResult

/** UpResult contains information about a [[Stack.up]] operation, including [[outputs]], and a [[summary]] of the deployed changes.
  *
  * @param stdout
  *   standard output
  * @param stderr
  *   standard error
  * @param outputs
  *   the stack outputs
  * @param summary
  *   the deployed changes
  */
case class UpResult(
  stdout: String,
  stderr: String,
  outputs: OutputMap,
  summary: UpdateSummary
):
  def permalink: Either[Exception, String] = ??? // TODO: implement GetPermalink
end UpResult

/** RefreshResult contains information about a [[Stack.refresh]] operation, including a [[summary]] of the deployed changes.
  *
  * @param stdout
  *   standard output
  * @param stderr
  *   standard error
  * @param summary
  *   the deployed changes
  */
case class RefreshResult(
  stdout: String,
  stderr: String,
  summary: UpdateSummary
):
  def permalink: Either[Exception, String] = ??? // TODO: implement GetPermalink
end RefreshResult

/** DestroyResult contains information about a [[Stack.destroy]] operation, including a [[summary]] of the deployed changes.
  *
  * @param stdout
  *   standard output
  * @param stderr
  *   standard error
  * @param summary
  *   the deployed changes
  */
case class DestroyResult(
  stdout: String,
  stderr: String,
  summary: UpdateSummary
):
  def permalink: Either[Exception, String] = ??? // TODO: implement GetPermalink
end DestroyResult

/** Provides a summary of a Stack lifecycle operation (up/preview/refresh/destroy).
  *
  * @param version
  *   The version of the update.
  * @param kind
  *   The kind of operation being performed.
  * @param startTime
  *   The start time of the operation.
  * @param message
  *   The message associated with the operation.
  * @param environment
  *   The environment variables during the operation.
  * @param config
  *   The configuration used for the operation.
  * @param result
  *   The result of the operation.
  * @param endTime
  *   The end time of the operation. This is only present once the update finishes.
  * @param resourceChanges
  *   The changes in resources during the operation. This is only present once the update finishes.
  */
case class UpdateSummary(
  version: Int,
  kind: String,
  startTime: String,
  message: String,
  environment: Map[String, String],
  config: ConfigMap,
  result: Option[String],
  endTime: Option[String],
  resourceChanges: Option[Map[String, Int]]
) derives JsonFormat
object UpdateSummary:
  def fromJsonList(json: String): Either[Exception, List[UpdateSummary]] = json.parseJson
end UpdateSummary

/** Describes a Pulumi engine event, such as a change to a resource or diagnostic message. [[EngineEvent]] is a discriminated union of all
  * possible event types, and exactly one field will be non-nil.
  *
  * @param sequence
  *   is a unique, and monotonically increasing number for each engine event sent to the Pulumi Service. Since events may be sent
  *   concurrently, and/or delayed via network routing, the sequence number is to ensure events can be placed into a total ordering.
  * @param timestamp
  *   is a Unix timestamp (seconds) of when the event was emitted.
  * @param summaryEvent
  *   represents a [[SummaryEvent]]
  */
case class EngineEvent(
  sequence: Int,
  timestamp: Int,
  summaryEvent: Option[SummaryEvent]
  // Ignore there rest for now
)
object EngineEvent:
  implicit object EngineEventFormat extends RootJsonFormat[EngineEvent] {
    def write(e: EngineEvent): JsObject = JsObject(
      "sequence" -> JsNumber(e.sequence),
      "timestamp" -> JsNumber(e.timestamp),
      "summaryEvent" -> e.summaryEvent.toJson
    )

    def read(value: JsValue): EngineEvent = {
      value.asJsObject.getFields("sequence", "timestamp", "summaryEvent") match {
        case Seq(JsNumber(sequence), JsNumber(timestamp), summaryEvent) =>
          new EngineEvent(
            sequence.toInt,
            timestamp.toInt,
            summaryEvent.convertTo[Option[SummaryEvent]]
          )
        case Seq(JsNumber(sequence), JsNumber(timestamp), _*) => // Ignore events we don't care about
          new EngineEvent(
            sequence.toInt,
            timestamp.toInt,
            None
          )
      }
    }
  }

  def fromJson(json: String): Either[Exception, EngineEvent] = json.parseJson
end EngineEvent

/** SummaryEvent is emitted at the end of an update, with a summary of the changes made.
  *
  * @param maybeCorrupt
  *   is set if one or more of the resources is in an invalid state.
  * @param durationSeconds
  *   is the number of seconds the update was executing.
  * @param resourceChanges
  *   contains the count for resource change by type.
  * @param policyPacks
  *   run during update. Maps PolicyPackName -> version.
  */
case class SummaryEvent(
  maybeCorrupt: Boolean,
  durationSeconds: Int,
  resourceChanges: Map[OpType, Int],
  policyPacks: Map[String, String]
)
object SummaryEvent:
  implicit object SummaryEventFormat extends RootJsonFormat[SummaryEvent] {
    def write(e: SummaryEvent): JsObject = JsObject(
      "maybeCorrupt" -> JsBoolean(e.maybeCorrupt),
      "durationSeconds" -> JsNumber(e.durationSeconds),
      "resourceChanges" -> e.resourceChanges.toJson,
      "PolicyPacks" -> e.policyPacks.toJson
    )

    def read(value: JsValue): SummaryEvent = {
      value.asJsObject.getFields("maybeCorrupt", "durationSeconds", "resourceChanges", "PolicyPacks") match {
        case Seq(JsBoolean(maybeCorrupt), JsNumber(durationSeconds), resourceChanges, policyPacks) =>
          new SummaryEvent(
            maybeCorrupt,
            durationSeconds.toInt,
            resourceChanges.convertTo[Map[OpType, Int]],
            policyPacks.convertTo[Map[String, String]]
          )
        case _ => throw DeserializationException("SummaryEvent expected")
      }
    }
  }
end SummaryEvent

/** OpType describes the type of operation performed to a resource managed by Pulumi. Should generally mirror `deploy.StepOp` in the engine.
  */
enum OpType(value: String):
  override def toString: String = value

  /** Indicates no change was made. */
  case Same extends OpType("same")

  /** Indicates a new resource was created. */
  case Create extends OpType("create")

  /** Indicates an existing resource was updated. */
  case Update extends OpType("update")

  /** Indicates an existing resource was deleted. */
  case Delete extends OpType("delete")

  /** Indicates an existing resource was replaced with a new one. */
  case Replace extends OpType("replace")

  /** Indicates a new resource was created for a replacement. */
  case CreateReplacement extends OpType("create-replacement")

  /** Indicates an existing resource was deleted after replacement. */
  case DeleteReplaced extends OpType("delete-replaced")

  /** Indicates reading an existing resource. */
  case Read extends OpType("read")

  /** Indicates reading an existing resource for a replacement. */
  case ReadReplacement extends OpType("read-replacement")

  /** Indicates refreshing an existing resource. */
  case Refresh extends OpType("refresh")

  /** Indicates removing a resource that was read. */
  case ReadDiscard extends OpType("discard")

  /** Indicates discarding a read resource that was replaced. */
  case DiscardReplaced extends OpType("discard-replaced")

  /** Indicates removing a pending replace resource. */
  case RemovePendingReplace extends OpType("remove-pending-replace")

  /** Indicates importing an existing resource. */
  case Import extends OpType("import")

  /** Indicates replacement of an existing resource with an imported resource. */
  case ImportReplacement extends OpType("import-replacement")
end OpType
object OpType:
  implicit object OpTypeFormat extends RootJsonFormat[OpType] {
    def write(e: OpType): JsObject = JsObject(
      "value" -> JsString(e.toString)
    )

    def read(value: JsValue): OpType = {
      value match {
        case JsString(value) =>
          OpType.from(value).left.map(DeserializationException("OpType expected", _)).fold(throw _, identity)
        case n => throw DeserializationException(s"OpType expected, got: $n")
      }
    }
  }

  def from(value: String): Either[Exception, OpType] =
    value match
      case "same"                   => Right(Same)
      case "create"                 => Right(Create)
      case "update"                 => Right(Update)
      case "delete"                 => Right(Delete)
      case "replace"                => Right(Replace)
      case "create-replacement"     => Right(CreateReplacement)
      case "delete-replaced"        => Right(DeleteReplaced)
      case "read"                   => Right(Read)
      case "read-replacement"       => Right(ReadReplacement)
      case "refresh"                => Right(Refresh)
      case "discard"                => Right(ReadDiscard)
      case "discard-replaced"       => Right(DiscardReplaced)
      case "remove-pending-replace" => Right(RemovePendingReplace)
      case "import"                 => Right(Import)
      case "import-replacement"     => Right(ImportReplacement)
      case _                        => Left(Exception(s"Unknown OpType: $value"))
  end from
end OpType
