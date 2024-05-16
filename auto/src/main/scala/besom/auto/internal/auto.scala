package besom.auto.internal

import LocalWorkspaceOption.*
import LocalWorkspace.*
import besom.util.*

/** Log in to a local Pulumi backend.
  *
  * Will store your state information on your computer underneath the provided directory `file://[pulumiHome]`. It is then up to you to
  * manage this state, including backing it up, using it in a team environment, and so on. Will set PULUMI_HOME to the provided path.
  *
  * Equivalent to `pulumi login file://[pulumiHome]/..` or `pulumi login file://~` or `pulumi login --local`
  *
  * @param pulumiHome
  *   the path to use for the local backend
  * @return
  *   a [[Unit]] or an error if any
  */
def loginLocal(pulumiHome: NotProvidedOr[os.Path] = NotProvided): Either[Exception, Unit] =
  pulumiHome match
    case NotProvided   => login(LoginOption.Local())
    case path: os.Path => login(LoginOption.Local(path / os.up), LoginOption.PulumiHome(path))
end loginLocal

/** Pulumi log in initialized the Pulumi state storage using the provided options.
  *
  * @param options
  *   the options to configure the login behavior
  * @return
  *   a [[Unit]] or an error if any
  */
def login(options: LoginOption*): Either[Exception, Unit] =
  val opts = LoginOptions.from(options.toList)
  val args: Seq[String] = Seq.empty[String]
    ++ opts.cloud.map("--cloud-url=" + _)
    ++ opts.local.bimap("--local")("file://" + _)
    ++ opts.defaultOrg.map("--default-org=" + _)
    ++ Option.when(opts.allowInsecure)("--insecure")
  val sOpts = Seq.empty[shell.ShellOption.Env]
    ++ opts.pulumiHome.map(p => shell.ShellOption.Env(shell.pulumi.env.PulumiHomeEnv -> p.toString))
    ++ opts.pulumiAccessToken.map(t => shell.ShellOption.Env(shell.pulumi.env.PulumiAccessTokenEnv -> t))
  shell
    .pulumi("login", args)(sOpts*)
    .fold(
      err => Left(AutoError(s"Failed to login: ${err.getMessage}", err)),
      _ => Right(())
    )
end login

/** Log in to a Pulumi backend.
  *
  * Available backends are:
  *   - the managed Pulumi Cloud backend, e.g. `PulumiAccessToken("...")`
  *   - a self-hosted Pulumi Cloud backend, e.g. `Cloud("https://api.pulumi.acmecorp.com)`
  *   - an object storage backends, e.g. `Cloud("s3://my-bucket")`
  *   - a local computer backend, e.g. `Local(os.home)`
  */
sealed trait LoginOption
object LoginOption:
  /** The URL of the Pulumi service to log in to.
    *
    * Equivalent to `pulumi login --cloud-url=[url]`
    */
  case class Cloud(url: String) extends LoginOption

  /** The path to the Pulumi home directory.
    *
    * Overrides the metadata directory for `pulumi login`. This customizes the location of `$PULUMI_HOME` where metadata is stored. If not
    * provided, will be read from the environment variable `PULUMI_HOME` or default to `~/.pulumi`
    */
  case class PulumiHome(path: os.Path) extends LoginOption

  /** The Pulumi access token to use for logging in.
    *
    * If not provided, will be read from the environment variable `PULUMI_ACCESS_TOKEN`.
    */
  case class PulumiAccessToken(token: String) extends LoginOption

  /** Use local log in to initialize the Pulumi state storage using the provided path.
    *
    * Will store your state information on your computer underneath the provided directory `file://[path]/.pulumi` It is then up to you to
    * manage this state, including backing it up, using it in a team environment, and so on.
    *
    * Equivalent to `pulumi login file://[path]` or `pulumi login file://~` or `pulumi login --local`
    *
    * @param path
    *   the path to use for the local backend
    */
  case class Local(path: NotProvidedOr[os.Path] = NotProvided) extends LoginOption

  /** A default org to associate with the login. Please note, currently, only the managed and self-hosted backends support organizations.
    *
    * Equivalent to `pulumi login --default-org=[org]`
    *
    * @param org
    *   the org to associate with the login
    */
  case class DefaultOrg(org: String) extends LoginOption

  /** Allow insecure server connections when using SSL/TLS.
    *
    * Equivalent to `pulumi login --insecure`
    */
  case object AllowInsecure extends LoginOption
end LoginOption

/** Log in to a Pulumi backend.
  *
  * Available backends are:
  *   - the managed Pulumi Cloud backend, e.g. `PulumiAccessToken("...")`
  *   - a self-hosted Pulumi Cloud backend, e.g. `Cloud("https://api.pulumi.acmecorp.com)`
  *   - an object storage backends, e.g. `Cloud("s3://my-bucket")`
  *   - a local computer backend, e.g. `Local(os.home)`
  *
  * @param cloud
  *   the URL of the Pulumi service to log in to
  * @param local
  *   the path to use for the local backend
  * @param pulumiHome
  *   the path to the Pulumi home directory
  * @param pulumiAccessToken
  *   the Pulumi access token to use for logging in
  * @param defaultOrg
  *   a default org to associate with the login
  * @param allowInsecure
  *   allow insecure server connections when using SSL/TLS
  */
case class LoginOptions(
  cloud: NotProvidedOr[String] = NotProvided,
  local: NotProvidedOr[os.Path] = NotProvided,
  pulumiHome: NotProvidedOr[os.Path] = NotProvided,
  pulumiAccessToken: NotProvidedOr[String] = NotProvided,
  defaultOrg: NotProvidedOr[String] = NotProvided,
  allowInsecure: Boolean = false
)
object LoginOptions:
  def from(options: LoginOption*): LoginOptions = from(options.toList)
  def from(options: List[LoginOption]): LoginOptions =
    options match
      case LoginOption.Cloud(url) :: tail               => from(tail*).copy(cloud = url)
      case LoginOption.Local(path) :: tail              => from(tail*).copy(local = path)
      case LoginOption.PulumiHome(path) :: tail         => from(tail*).copy(pulumiHome = path)
      case LoginOption.PulumiAccessToken(token) :: tail => from(tail*).copy(pulumiAccessToken = token)
      case LoginOption.DefaultOrg(org) :: tail          => from(tail*).copy(defaultOrg = org)
      case LoginOption.AllowInsecure :: tail            => from(tail*).copy(allowInsecure = true)
      case Nil                                          => LoginOptions()
      case o                                            => throw AutoError(s"Unknown login option: $o")
  end from

/** Log out of the Pulumi backend.
  * @param options
  *   the options to configure the logout behavior
  * @return
  *   a [[Unit]] or an error if any
  */
def logout(options: LogoutOption*): Either[Exception, Unit] =
  val opts = LogoutOptions.from(options.toList)
  val args = Seq.empty[String]
    ++ opts.local.bimap("--local")(_.toString)
    ++ opts.cloud.map("--cloud-url=" + _)
    ++ Option.when(opts.all)("--all")
  val sOpts = Seq.empty[shell.ShellOption.Env]
    ++ opts.pulumiHome.map(p => shell.ShellOption.Env(shell.pulumi.env.PulumiHomeEnv -> p.toString))
  shell
    .pulumi("logout", args)(sOpts*)
    .fold(
      err => Left(AutoError(s"Failed to logout: ${err.getMessage}", err)),
      _ => Right(())
    )
end logout

/** Can be used to configure the [[logout]] behavior.
  */
sealed trait LogoutOption
object LogoutOption:
  /** Logout of all backends.
    */
  case object All extends LogoutOption

  /** A cloud URL to log out of (defaults to current cloud).
    */
  case class Cloud(url: String) extends LogoutOption

  /** Log out of using local mode
    */
  case class Local(path: NotProvidedOr[os.Path] = NotProvided) extends LogoutOption

  /** The path to the Pulumi home directory.
    *
    * Overrides the metadata directory for `pulumi logout`. This customizes the location of `$PULUMI_HOME` where metadata is stored. If not
    * provided, will be read from the environment variable `PULUMI_HOME` or default to `~/.pulumi`
    */
  case class PulumiHome(path: os.Path) extends LogoutOption

/** Can be used to configure the [[logout]] behavior.
  * @param all
  *   if true, log out of all backends
  * @param local
  *   log out of using local mode with the provided path (defaults to `~`)
  * @param cloud
  *   a cloud URL to log out of (defaults to current cloud)
  * @param pulumiHome
  *   the path to the Pulumi home directory, if not provided, will be read from the environment variable `PULUMI_HOME` or default to
  *   `~/.pulumi`
  */
case class LogoutOptions(
  all: Boolean = false,
  local: NotProvidedOr[os.Path] = NotProvided,
  cloud: NotProvidedOr[String] = NotProvided,
  pulumiHome: NotProvidedOr[os.Path] = NotProvided
)
object LogoutOptions:
  /** Creates a [[LogoutOptions]] from a list of [[LogoutOption]]s.
    * @param options
    *   the list of [[LogoutOption]]s
    * @return
    *   a [[LogoutOptions]]
    */
  def from(options: LogoutOption*): LogoutOptions = from(options.toList)

  /** Creates a [[LogoutOptions]] from a list of [[LogoutOption]]s.
    * @param options
    *   the list of [[LogoutOption]]s
    * @return
    *   a [[LogoutOptions]]
    */
  def from(options: List[LogoutOption]): LogoutOptions =
    options match
      case LogoutOption.All :: tail              => from(tail*).copy(all = true)
      case LogoutOption.Local(path) :: tail      => from(tail*).copy(local = path)
      case LogoutOption.Cloud(url) :: tail       => from(tail*).copy(cloud = url)
      case LogoutOption.PulumiHome(path) :: tail => from(tail*).copy(pulumiHome = path)
      case Nil                                   => LogoutOptions()
      case o                                     => throw AutoError(s"Unknown logout option: $o")
  end from
end LogoutOptions

/** Creates and configures a [[LocalWorkspace]].
  *
  * [[LocalWorkspaceOption]]s can be used to configure things like e.g.:
  *   - [[WorkDir]] - the working directory
  *   - [[Program]] - the program to execute
  *   - [[Repo]] - the git repository to clone
  *
  * @param options
  *   options to pass to the [[LocalWorkspace]]
  * @return
  *   a [[LocalWorkspace]] or an error if any
  */
def localWorkspace(options: LocalWorkspaceOption*): Either[Exception, LocalWorkspace] = LocalWorkspace(options*)

/** Creates a [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, from the specified [[WorkDir]].
  *
  * This Workspace will pick up any available Settings files (`Pulumi.yaml`, `Pulumi.[stack].yaml`).
  * @param stackName
  *   the name of the stack to create
  * @param options
  *   options to pass to the [[LocalWorkspace]]
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def createStackLocalSource(
  stackName: String,
  workDir: os.Path,
  options: LocalWorkspaceOption*
): Either[Exception, Stack] =
  for
    ws <- localWorkspace(WorkDir(workDir) +: options*)
    s  <- Stack.create(stackName, ws)
  yield s

/** Creates or selects a [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, from the specified [[WorkDir]]. If the
  * [[Stack]] already exists, it will not throw an error and proceed to selecting the [[Stack]]. This [[Workspace]] will pick up any
  * available Settings files (`Pulumi.yaml`, `Pulumi.[stack].yaml`).
  *
  * @param stackName
  *   the name of the stack to upsert
  * @param workDir
  *   the working directory containing the project
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def upsertStackLocalSource(
  stackName: String,
  workDir: os.Path,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  for
    ws <- localWorkspace(WorkDir(workDir) +: opts*)
    s  <- Stack.upsert(stackName, ws)
  yield s

/** Selects an existing [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, from the specified [[WorkDir]]. This
  * [[Workspace]] will pick up any available Settings files (`Pulumi.yaml`, `Pulumi.[stack].yaml`).
  *
  * @param stackName
  *   the name of the stack to select
  * @param workDir
  *   the working directory containing the project
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def selectStackLocalSource(
  stackName: String,
  workDir: os.Path,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  for
    ws <- localWorkspace(WorkDir(workDir) +: opts*)
    s  <- Stack.select(stackName, ws)
  yield s

/** Creates a [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, with source code cloned from the specified
  * [[GitRepo]]. This [[Workspace]] will pick up any available Settings files (`Pulumi.yaml`, `Pulumi.[stack].yaml`) that are cloned into
  * the Workspace. Unless a [[WorkDir]] option is specified, the [[GitRepo]] will be clone into a new temporary directory provided by the
  * OS.
  *
  * @param stackName
  *   the name of the stack to create
  * @param repo
  *   the Git repository to clone
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def createStackRemoteSource(
  stackName: String,
  repo: GitRepo,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  for
    ws <- localWorkspace(Repo(repo) +: opts*)
    s  <- Stack.create(stackName, ws)
  yield s

/** Creates a [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, with source code cloned from the specified
  * [[GitRepo]]. If the [[Stack]] already exists, it will not error and proceed to selecting the [[Stack]]. This [[Workspace]] will pick up
  * any available Settings files (`Pulumi.yaml`, `Pulumi.[stack].yaml`) that are cloned into the [[Workspace]]. Unless a [[WorkDir]] option
  * is specified, the [[GitRepo]] will be clone into a new temporary directory provided by the OS.
  * @param stackName
  *   the name of the stack to upsert
  * @param repo
  *   the Git repository to clone
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def upsertStackRemoteSource(
  stackName: String,
  repo: GitRepo,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  for
    ws <- localWorkspace(Repo(repo) +: opts*)
    s  <- Stack.upsert(stackName, ws)
  yield s

/** Selects an existing [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, with source code cloned from the specified
  * [[GitRepo]]. This [[Workspace]] will pick up any available Settings files (`Pulumi.yaml`, `Pulumi.[stack].yaml`) that are cloned into
  * the [[Workspace]]. Unless a [[WorkDir]] option is specified, the [[GitRepo]] will be clone into a new temporary directory provided by
  * the OS.
  * @param stackName
  *   the name of the stack to select
  * @param repo
  *   the Git repository to clone
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def selectStackRemoteSource(
  stackName: String,
  repo: GitRepo,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  for
    ws <- localWorkspace(Repo(repo) +: opts*)
    s  <- Stack.select(stackName, ws)
  yield s

/** Creates a [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, with the specified program. If no [[Project]] option
  * is specified, default project settings will be created on behalf of the user. Similarly, unless a [[WorkDir]] option is specified, the
  * working directory will default to a new temporary directory provided by the OS.
  *
  * @param stackName
  *   the name of the stack to create
  * @param projectName
  *   the name of the project
  * @param program
  *   the Pulumi program to execute
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def createStackInlineSource(
  stackName: String,
  projectName: String,
  program: RunFunc,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  val optsWithProgram = Program(program) +: opts
  val allOpts: Either[AutoError, Seq[LocalWorkspaceOption]] =
    getProjectSettings(projectName, optsWithProgram) match
      case Left(e)     => Left(AutoError(s"Failed to create stack '$stackName': ${e.getMessage}", e))
      case Right(proj) => Right(Project(proj) +: opts)
  for
    allOpts <- allOpts
    ws      <- localWorkspace(allOpts*)
    s       <- Stack.create(stackName, ws)
  yield s
end createStackInlineSource

/** Creates a [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, with the specified program. If the [[Stack]] already
  * exists, it will not error and proceed to selecting the [[Stack]]. If no [[Project]] option is specified, default project settings will
  * be created on behalf of the user. Similarly, unless a [[WorkDir]] option is specified, the working directory will default to a new
  * temporary directory provided by the OS.
  *
  * @param stackName
  *   the name of the stack to upsert
  * @param projectName
  *   the name of the project
  * @param program
  *   the Pulumi program to execute
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def upsertStackInlineSource(
  stackName: String,
  projectName: String,
  program: RunFunc,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  val optsWithProgram = Program(program) +: opts
  val allOpts: Either[AutoError, Seq[LocalWorkspaceOption]] =
    getProjectSettings(projectName, optsWithProgram) match
      case Left(e)     => Left(AutoError(s"Failed to upsert stack '$stackName': ${e.getMessage}", e))
      case Right(proj) => Right(Project(proj) +: opts)
  for
    allOpts <- allOpts
    ws      <- localWorkspace(allOpts*)
    s       <- Stack.upsert(stackName, ws)
  yield s
end upsertStackInlineSource

/** Selects an existing [[Stack]] backed by a [[LocalWorkspace]] created on behalf of the user, with the specified program. If no
  * [[Project]] option is specified, default project settings will be created on behalf of the user. Similarly, unless a [[WorkDir]] option
  * is specified, the working directory will default to a new temporary directory provided by the OS.
  * @param stackName
  *   the name of the stack to select
  * @param projectName
  *   the name of the project
  * @param program
  *   the Pulumi program to execute
  * @param opts
  *   the options for the local workspace
  * @return
  *   a [[Stack]] backed by a [[LocalWorkspace]] or an error if any
  */
def selectStackInlineSource(
  stackName: String,
  projectName: String,
  program: RunFunc,
  opts: LocalWorkspaceOption*
): Either[Exception, Stack] =
  val optsWithProgram = Program(program) +: opts
  val allOpts: Either[AutoError, Seq[LocalWorkspaceOption]] =
    getProjectSettings(projectName, optsWithProgram) match
      case Left(e)     => Left(AutoError(s"Failed to select stack '$stackName': ${e.getMessage}", e))
      case Right(proj) => Right(Project(proj) +: opts)
  for
    allOpts <- allOpts
    ws      <- localWorkspace(allOpts*)
    s       <- Stack.select(stackName, ws)
  yield s
end selectStackInlineSource
