package besom.auto

import besom.json.*
import besom.json.DefaultJsonProtocol.*
import besom.model.{PackageName, SemanticVersion, FullyQualifiedStackName, StackName}
import besom.util.*
import besom.model
import os.Path

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.language.implicitConversions
import scala.util.Try

/** LocalWorkspace is a default implementation of the [[Workspace]] trait. A [[Workspace]] is the execution context containing a single
  * Pulumi project, a program, and multiple stacks. Workspaces are used to manage the execution environment, providing various utilities
  * such as plugin installation, environment configuration (`$PULUMI_HOME`), and creation, deletion, and listing of Stacks.
  * [[LocalWorkspace]] relies on `Pulumi.yaml` and `Pulumi.[stack].yaml` as the intermediate format for [[Project]] and [[Stack]] settings.
  * Modifying [[projectSettings]] will alter the [[Workspace]] `Pulumi.yaml` file, and setting config on a [[Stack]] will modify the
  * `Pulumi.[stack].yaml` file. This is identical to the behavior of Pulumi CLI driven workspaces.
  */
trait LocalWorkspace extends Workspace:
  import LocalWorkspace.*

  given Workspace = this

  /** Returns the settings object for the current project if any. [[LocalWorkspace]] reads settings from the `Pulumi.yaml` in the workspace.
    * A workspace can contain only a single project at a time.
    * @return
    *   a [[Project]] settings or error if any
    */
  def projectSettings: Either[Exception, Project] = readProjectSettingsFromDir(workDir)

  /** Overwrites the settings object in the current project. There can only be a single project per workspace. Fails if new project name
    * does not match old. Writes this value to a `Pulumi.yaml` file in [[workDir]].
    *
    * @param settings
    *   the project settings to save
    * @return
    *   an exception if the save failed, otherwise nothing
    */
  def saveProjectSettings(settings: Project): Either[Exception, Unit] = Try {
    val pulumiYamlPath = workDir / shell.pulumi.ProjectFileName()
    settings.save(pulumiYamlPath)
  }.toEither.flatten.left.map(e => AutoError(s"failed to save project settings: ${e.getMessage}", e))

  /** Returns the settings object for the stack matching the specified stack name if any. Reads this from a `Pulumi.[stack].yaml` file in
    * [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @return
    *   a [[ProjectStack]] or an error if any
    */
  def stackSettings(stackName: String): Either[Exception, ProjectStack] =
    readStackSettingFromDir(workDir, stackName)

  /** Overwrites the settings object for the stack matching the specified stack name. Saves this to a `Pulumi.[stack].yaml` file in
    * [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @param settings
    *   The stack settings to save.
    * @return
    *   an exception if the save failed, otherwise nothing.
    */
  def saveStackSettings(stackName: String, settings: ProjectStack): Either[Exception, Unit] = Try {
    val actualStackName = getStackSettingsName(stackName)
    val pulumiYamlPath  = workDir / shell.pulumi.StackFileName(actualStackName)
    settings.save(pulumiYamlPath)
  }.toEither.left.map(e => AutoError(s"failed to save stack settings: ${e.getMessage}", e))

  /** This is a hook to provide additional args to every CLI commands before they are executed. Provided with stack name, returns a list of
    * args to append to an invoked command `["--config=...", ]`.
    *
    * LocalWorkspace does not utilize this extensibility point.
    *
    * @param stackName
    *   the name of the stack
    * @return
    *   a list of args to append to an invoked command
    */
  def serializeArgsForOp(stackName: String): Either[Exception, List[String]] =
    // not utilized for LocalWorkspace
    Left(AutoError("LocalWorkspace does not utilize this extensibility point"))

  /** This is a hook executed after every command. Called with the stack name. An extensibility point to perform workspace cleanup. CLI
    * operations may create/modify a `Pulumi.[stack].yaml` in [[workDir]].
    *
    * LocalWorkspace does not utilize this extensibility point.
    *
    * @param stackName
    *   the name of the stack
    * @return
    *   an exception if the cleanup failed, otherwise nothing
    */
  def postCommandCallback(stackName: String): Either[Exception, Unit] =
    // not utilized for LocalWorkspace
    Left(AutoError("LocalWorkspace does not utilize this extensibility point"))

  /** Adds the specified environments to the provided stack's configuration.
    *
    * @param stackName
    *   the name of the stack
    * @param envs
    *   the environments to add
    * @return
    *   an exception if the add failed, otherwise nothing
    */
  def addEnvironments(
    stackName: String,
    envs: String*
  ): Either[Exception, Unit] =

    // 3.95 added this command (https://github.com/pulumi/pulumi/releases/tag/v3.95.0)
    if pulumiVersion < model.SemanticVersion(3, 95, 0)
    then Left(AutoError("addEnvironments requires Pulumi version >= 3.95.0"))
    else
      val args = Seq("config", "env", "add") ++ envs ++ Seq("--yes", "--stack", stackName)
      pulumi(args)().fold(
        e => Left(e.withMessage(s"failed to add environments to stack: '$stackName'")),
        _ => Right(())
      )
    end if
  end addEnvironments

  /** Lists the environments from the provided stack's configuration.
    *
    * @param stackName
    *   The name of the stack.
    * @return
    *   Either an Exception or a list of environments.
    */
  def listEnvironments(stackName: String): Either[Exception, List[String]] =
    // 3.99 added this command (https://github.com/pulumi/pulumi/releases/tag/v3.99.0)
    if pulumiVersion < model.SemanticVersion(3, 99, 0)
    then Left(AutoError("listEnvironments requires Pulumi version >= 3.99.0"))
    else
      val args = Seq("config", "env", "ls", "--stack", stackName, "--json")
      pulumi(args)().fold(
        e => Left(e.withMessage(s"failed to list environments for stack: '$stackName'")),
        r => r.out.parseJson[List[String]].left.map(e => AutoError(s"failed to parse environments for stack: '$stackName'", e))
      )
  end listEnvironments

  /** Removes the specified environment from the provided stack's configuration.
    *
    * @param stackName
    *   the name of the stack
    * @param env
    *   the environment to remove
    * @return
    *   an exception if the remove failed, otherwise nothing
    */
  def removeEnvironment(stackName: String, env: String): Either[Exception, Unit] =
    // 3.95 added this command (https://github.com/pulumi/pulumi/releases/tag/v3.95.0)
    if pulumiVersion < model.SemanticVersion(3, 95, 0)
    then Left(AutoError("removeEnvironment requires Pulumi version >= 3.95.0"))
    else
      val args = Seq("--stack", stackName, "config", "env", "rm", env, "--yes")
      pulumi(args)()
        .fold(
          e => Left(e.withMessage(s"failed to remove environment from stack: '$stackName'")),
          _ => Right(())
        )
  end removeEnvironment

  /** Returns the value associated with the specified stack name and key using the optional [[ConfigOption]]s, scoped to the current
    * workspace. [[LocalWorkspace]] reads this config from the matching `Pulumi.[stack].yaml` file in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @param key
    *   the key to retrieve the value for
    * @param options
    *   the optional [[ConfigOption]]s to use
    * @return
    *   the config value or an error if any
    */
  def getConfig(
    stackName: String,
    key: String,
    options: ConfigOption*
  ): Either[Exception, ConfigValue] =
    val maybePath = options.collectFirst { case ConfigOption.Path => "--path" }
    val args      = Seq("--stack", stackName, "config", "get") ++ maybePath ++ Seq(key, "--show-secrets", "--json")
    pulumi(args)() match
      case Left(e)  => Left(e.withMessage(s"failed to get config '$key' for stack: '$stackName'"))
      case Right(r) => ConfigValue.fromJson(r.out)
  end getConfig

  /** Returns the config map for the specified stack name, scoped to the current workspace. [[LocalWorkspace]] reads this config from the
    * matching `Pulumi.stack.yaml` file in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @return
    *   the config map or an error if any
    */
  def getAllConfig(stackName: String): Either[Exception, ConfigMap] =
    val args = Seq("--stack", stackName, "config", "--show-secrets", "--json")
    pulumi(args)() match
      case Left(e)  => Left(e.withMessage(s"failed to get all configs for stack: '$stackName'"))
      case Right(r) => ConfigMap.fromJson(r.out)
  end getAllConfig

  /** Sets the specified key-value pair on the provided stack name. [[LocalWorkspace]] writes this value to the matching
    * `Pulumi.[stack].yaml` file in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @param key
    *   the key to set the value for
    * @param value
    *   the config value to set
    * @param options
    *   the optional [[ConfigOption]]s to use
    * @return
    *   an exception if the set failed, otherwise nothing
    */
  def setConfig(
    stackName: String,
    key: String,
    value: ConfigValue,
    options: ConfigOption*
  ): Either[Exception, Unit] =
    val maybePath   = options.collectFirst { case ConfigOption.Path => "--path" }
    val maybeSecret = if value.secret then "--secret" else "--plaintext"
    val args        = Seq("--stack", stackName, "config", "set") ++ maybePath ++ Seq(key, maybeSecret, "--", value.value)
    pulumi(args)() match
      case Left(e)  => Left(e.withMessage(s"failed to set config '$key' for stack: '$stackName'"))
      case Right(_) => Right(())
  end setConfig

  /** Sets all values in the provided config map for the specified stack name. [[LocalWorkspace]] writes the config to the matching
    * `Pulumi.[stack].yaml` file in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @param config
    *   the [[ConfigMap]] to upsert against the existing config
    * @param options
    *   the optional [[ConfigOption]]s to use
    * @return
    *   an exception if the set failed, otherwise nothing
    */
  def setAllConfig(
    stackName: String,
    config: ConfigMap,
    options: ConfigOption*
  ): Either[Exception, Unit] =
    val maybePath = options.collectFirst { case ConfigOption.Path => "--path" }
    val pairs: Seq[String] = config.flatMap { case (k, v) =>
      Seq(if v.secret then "--secret" else "--plaintext", s"$k=${v.value}")
    }.toSeq
    val args = Seq("--stack", stackName, "config", "set-all") ++ maybePath ++ ("--" +: pairs)
    pulumi(args)() match
      case Left(e)  => Left(e.withMessage(s"failed to set all configs for stack: '$stackName'"))
      case Right(_) => Right(())
  end setAllConfig

  /** Removes the specified key-value pair on the provided stack name. It will remove any matching values in the `Pulumi.[stack].yaml` file
    * in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @param key
    *   the config key to remove
    * @param options
    *   the optional [[ConfigOption]]s to use
    * @return
    *   an exception if the remove failed, otherwise nothing
    */
  def removeConfig(
    stackName: String,
    key: String,
    options: ConfigOption*
  ): Either[Exception, Unit] =
    val maybePath = options.collectFirst { case ConfigOption.Path => "--path" }
    val args      = Seq("--stack", stackName, "config", "rm") ++ maybePath ++ Seq(key)
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to remove config '$key' for stack: '$stackName'"))
      case _       => Right(())
  end removeConfig

  /** Removes all values in the provided key list for the specified stack name It will remove any matching values in the
    * `Pulumi.[stack].yaml` file in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    * @param keys
    *   the list of keys to remove from the underlying config
    * @param options
    *   the optional [[ConfigOption]]s to use
    * @return
    *   an exception if the remove failed, otherwise nothing
    */
  def removeAllConfig(
    stackName: String,
    keys: List[String],
    options: ConfigOption*
  ): Either[Exception, Unit] =
    val maybePath = options.collectFirst { case ConfigOption.Path => "--path" }
    val args      = Seq("--stack", stackName, "config", "rm-all") ++ maybePath ++ keys
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to remove config '$keys' for stack: '$stackName'"))
      case _       => Right(())
  end removeAllConfig

  /** Gets and sets the config map used with the last update for Stack matching stack name. It will overwrite all configuration in the
    * `Pulumi.[stack].yaml` file in [[workDir]].
    *
    * @param stackName
    *   the name of the stack
    */
  def refreshConfig(stackName: String): Either[Exception, ConfigMap] =
    val args = Seq("--stack", stackName, "config", "refresh", "--force")
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to refresh config for stack: '$stackName'"))
      case _       => Right(())
    getAllConfig(stackName)
  end refreshConfig

  /** Returns the value associated with the specified stack name and key, scoped to the [[LocalWorkspace]].
    *
    * @param stackName
    *   the stack to read tag metadata from
    * @param key
    *   the key to use for the tag lookup
    * @return
    *   the tag value or an error if any
    */
  def getTag(stackName: String, key: String): Either[Exception, String] =
    val args = Seq("--stack", stackName, "stack", "tag", "get", key)
    pulumi(args)() match
      case Left(e)  => Left(e.withMessage(s"failed to get stack tag '$key' for stack: '$stackName'"))
      case Right(r) => Right(r.out)
  end getTag

  /** Sets the specified key-value pair on the provided stack name.
    *
    * @param stackName
    *   the stack to set tag metadata on
    * @param key
    *   the tag key to set
    * @param value
    *   the tag value to set
    * @return
    *   an exception if the set failed, otherwise nothing
    */
  def setTag(stackName: String, key: String, value: String): Either[Exception, Unit] =
    val args = Seq("--stack", stackName, "stack", "tag", "set", key, value)
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to set stack tag '$key' for stack: '$stackName'"))
      case _       => Right(())
  end setTag

  /** Removes the specified key-value pair on the provided stack name.
    *
    * @param stackName
    *   the stack to remove tag metadata from
    * @param key
    *   the tag key to remove
    * @return
    *   an exception if the remove failed, otherwise nothing
    */
  def removeTag(stackName: String, key: String): Either[Exception, Unit] =
    val args = Seq("--stack", stackName, "stack", "tag", "rm", key)
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to remove stack tag '$key' for stack: '$stackName'"))
      case _       => Right(())
  end removeTag

  /** Returns the tag map for the specified tag name, scoped to the current LocalWorkspace.
    *
    * @param stackName
    *   the stack to read tag metadata from
    * @return
    *   the tag map or an error if any
    */
  def listTags(stackName: String): Either[Exception, TagMap] =
    val args = Seq("--stack", stackName, "stack", "tag", "list", "--json")
    pulumi(args)() match
      case Left(e)  => Left(e.withMessage(s"failed to list stack tags for stack: '$stackName'"))
      case Right(r) => TagMap.fromJson(r.out)
  end listTags

  protected def envVars: AtomicReference[mutable.Map[String, String]]
  def getEnvVars: Map[String, String]                  = envVars.get().toMap
  def setEnvVars(variables: Map[String, String]): Unit = envVars.getAndUpdate(_ ++= variables)
  def setEnvVar(key: String, value: String): Unit      = envVars.getAndUpdate(_ += (key -> value))
  def unsetEnvVar(key: String): Unit                   = envVars.getAndUpdate(_ -= key)

  protected def rawSecretsProvider: AtomicReference[Option[String]]
  def secretsProvider: Option[String] = rawSecretsProvider.get()

  /** Returns detailed information about the currently authenticated user, the logged-in Pulumi identity.
    * @return
    *   the currently authenticated user details
    */
  def whoAmI: Either[Exception, WhoAmIResult] =
    // 3.58 added the --json flag (https://github.com/pulumi/pulumi/releases/tag/v3.58.0)
    if pulumiVersion < model.SemanticVersion(3, 58, 0)
    then Left(AutoError("whoAmI requires Pulumi version >= 3.58.0"))
    else
      val args = Seq("whoami", "--json")
      pulumi(args)() match
        case Left(e)  => Left(e.withMessage(s"failed to get the currently authenticated user"))
        case Right(r) => WhoAmIResult.fromJson(r.out)
  end whoAmI

  /** The secrets provider to use for encryption and decryption of stack secrets. See:
    * https://www.pulumi.com/docs/intro/concepts/secrets/#available-encryption-providers
    *
    * Valid secret providers types are `default`, `passphrase`, `awskms`, `azurekeyvault`, `gcpkms`, `hashivault`.
    *
    * To change the stack to use a cloud secrets backend, use the URL format, e.g.: `awskms://alias/ExampleAlias?region=us-east-1`
    *
    * @param stackName
    *   the name of the stack
    * @param newSecretsProvider
    *   the new secrets provider to use
    * @param options
    *   the optional [[ChangeSecretsProviderOption]]s to use
    * @return
    *   the currently configured secrets provider
    */
  def changeStackSecretsProvider(
    stackName: String,
    newSecretsProvider: String | SecretsProviderType,
    options: ChangeSecretsProviderOption*
  ): Either[Exception, Unit] =
    val args: Seq[String] = Seq("stack", "change-secrets-provider", "--stack", stackName, newSecretsProvider)

    // If we're changing to a passphrase provider, we need to pass the new passphrase.
    val maybeStdin =
      if newSecretsProvider == "passphrase" then
        options
          .collectFirst { case NewPassphraseOption(newPassphrase) =>
            Seq(shell.ShellOption.Stdin(newPassphrase))
          }
          .toRight {
            AutoError("failed to change stack secrets provider, no new passphrase provided")
          }
      else Right(Seq.empty)

    for {
      stdin <- maybeStdin
      result <- pulumi(args)(stdin*) match
        case Left(e) => Left(e.withMessage(s"failed to change stack secrets provider: '$newSecretsProvider'"))
        case _       => Right(())
    } yield
      // make sure we update the workspace secrets provider
      this.rawSecretsProvider.set(Some(newSecretsProvider))
      result
  end changeStackSecretsProvider

  /** Returns a summary of the currently selected stack, if any.
    * @return
    *   the currently selected stack summary
    */
  def stack: Either[Exception, Option[StackSummary]] =
    listStacks.map(_.collectFirst { case s @ StackSummary(_, true, _, _, _, _) => s })

  /** Creates and sets a new stack with the stack name, failing if one already exists.
    *
    * @param stackName
    *   the stack to create
    * @return
    *   an exception if the create failed, otherwise nothing
    */
  def createStack(stackName: String): Either[Exception, Stack] =
    /*val maybeStackArgs = StackName.parse(stackName) match
      case Left(e) => Left(e)
      case Right(value) =>
        value.parts match
          case (Some("organization"), _, stack) => Right(Seq(stack))
          case (Some(_), _, _)                  => Right(Seq("--stack", value))
          case (None, _, stack)                 => Right(Seq(stack))
     */
    val maybeSecretsProvider = secretsProvider.toSeq.flatMap(sp => Seq("--secrets-provider", sp))
    val maybeRemote          = if remote then Seq("--remote") else Seq.empty
    for
      //      stackArgs <- maybeStackArgs
      res <- {
        val args: Seq[String] = Seq("stack", "init") ++ Seq("--stack", stackName) ++ maybeSecretsProvider ++ maybeRemote
        pulumi(args)() match
          case Left(e) => Left(e.withMessage(s"failed to create stack: '$stackName'"))
          case _ =>
            FullyQualifiedStackName
              .parse(stackName)
              .map(Stack(_, this))
              .left
              .map(e => AutoError(s"failed to create stack: ${e.getMessage}", e))
      }
    yield res
  end createStack

  /** Selects and sets an existing stack matching the stack name, failing if none exists.
    *
    * @param stackName
    *   the stack to select
    * @return
    *   an exception if the select failed, otherwise nothing
    */
  def selectStack(stackName: String): Either[Exception, Stack] =
    val maybeSelect = if !remote then Seq("select") else Seq.empty
    val args        = Seq("--stack", stackName, "stack") ++ maybeSelect
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to select stack: '$stackName'"))
      case _       => FullyQualifiedStackName.parse(stackName).map(Stack(_, this))
  end selectStack

  /** Deletes the stack and all associated configuration and history. */
  def removeStack(stackName: String, options: RemoveOption*): Either[Exception, Unit] =
    val maybeForce = options.collectFirst { case RemoveOption.Force => "--force" }
    val args       = Seq("--stack", stackName, "stack", "rm", "--yes") ++ maybeForce
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to remove stack: '$stackName'"))
      case _       => Right(())
  end removeStack

  /** Returns all Stacks created under the current Project. This queries underlying backend may return stacks not present in the
    * [[Workspace]] (as `Pulumi.[stack].yaml` files).
    * @return
    *   a list of stack summaries
    */
  def listStacks: Either[Exception, List[StackSummary]] =
    pulumi("stack", "ls", "--json")() match
      case Left(e)  => Left(e.withMessage(s"failed to list stacks"))
      case Right(r) => StackSummary.fromJsonList(r.out)
  end listStacks

  /** Installs a plugin in the [[Workspace]], for example to use cloud providers like AWS or GCP.
    *
    * @param name
    *   the name of the plugin
    * @param version
    *   the version of the plugin e.g. "v1.0.0"
    * @param kind
    *   the kind of plugin, defaults to "resource"
    * @param server
    *   the optional server to install the plugin from
    * @return
    *   an exception if the install failed, otherwise nothing
    */
  def installPlugin(
    name: String,
    version: String,
    kind: String = "resource",
    server: NotProvidedOr[String] = NotProvided
  ): Either[Exception, Unit] =
    val maybeServer = server.asOption.toSeq.flatMap(s => Seq("--server", s))
    val args        = Seq("plugin", "install", kind, name, version) ++ maybeServer
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to install plugin: '$name'"))
      case _       => Right(())
  end installPlugin

  /** Removes a plugin from the [[Workspace]] matching the specified name and version.
    *
    * @param name
    *   the optional name of the plugin
    * @param versionRange
    *   optional semver range to check when removing plugins matching the given name e.g. "1.0.0", ">1.0.0".
    * @param kind
    *   he kind of plugin, defaults to "resource"
    * @return
    *   an exception if the remove failed, otherwise nothing
    */
  def removePlugin(
    name: NotProvidedOr[String] = NotProvided,
    versionRange: NotProvidedOr[String] = NotProvided,
    kind: String = "resource"
  ): Either[Exception, Unit] =
    val args = Seq("plugin", "rm", kind) ++ name.asOption ++ versionRange.asOption ++ Seq("--yes")
    pulumi(args)() match
      case Left(e) => Left(e.withMessage(s"failed to remove plugin: '$name'"))
      case _       => Right(())
  end removePlugin

  /** Returns a list of all plugins installed in the [[Workspace]].
    * @return
    *   a list of plugin info
    */
  def listPlugins: Either[Exception, List[PluginInfo]] =
    pulumi("plugin", "ls", "--json")() match
      case Left(e)  => Left(e.withMessage(s"failed to list plugins"))
      case Right(r) => PluginInfo.fromJsonList(r.out)
  end listPlugins

  /** exports the deployment state of the stack. This can be combined with [[importStack]] to edit a stack's state (such as recovery from
    * failed deployments).
    *
    * @param stackName
    *   the name of the stack
    * @return
    *   the deployment state of the stack
    */
  def exportStack(stackName: String): Either[Exception, UntypedDeployment] =
    pulumi("--stack", stackName, "stack", "export", "--show-secrets")() match
      case Left(e)  => Left(e.withMessage(s"failed to export stack: '$stackName'"))
      case Right(r) => UntypedDeployment.fromJson(r.out)
  end exportStack

  /** imports the specified deployment state into a pre-existing stack. This can be combined with [[exportStack]] to edit a stack's state
    * (such as recovery from failed deployments).
    *
    * @param stackName
    *   the name of the stack
    * @param state
    *   the stack state to import
    * @return
    *   an exception if the import failed, otherwise nothing
    */
  def importStack(stackName: String, state: UntypedDeployment): Either[Exception, Unit] =
    val maybeState = Try {
      val tempFile = os.temp(prefix = "pulumi_auto", suffix = ".json")
      os.write(tempFile, state.toJson)
    }.toEither.left.map(e => AutoError(s"failed to import stack: ${e.getMessage}", e))

    for tempFile <- maybeState
    yield pulumi("--stack", stackName, "stack", "import", "--file", tempFile.toString)() match
      case Left(e) => Left(e.withMessage(s"failed to import stack: '$stackName'"))
      case _       => Right(())
  end importStack

  /** Gets the current set of [[Stack]] outputs from the last Stack.up operation.
    *
    * @param stackName
    *   the name of the stack
    * @return
    *   the current set of [[Stack]] outputs from the last Stack.up
    */
  def stackOutputs(stackName: String): Either[Exception, OutputMap] =
    val maskedResult: Either[Exception, String] = pulumi("--stack", stackName, "stack", "output", "--json")() match
      case Left(e)  => Left(e.withMessage(s"failed to get stack outputs for stack: '$stackName'"))
      case Right(r) => Right(r.out)
    val plaintextResult: Either[Exception, String] = pulumi("--stack", stackName, "stack", "output", "--json", "--show-secrets")() match
      case Left(e)  => Left(e.withMessage(s"failed to get stack outputs for stack: '$stackName'"))
      case Right(r) => Right(r.out)
    // merge the masked and plaintext results, and mark any masked values as secret
    for
      masked    <- maskedResult
      plaintext <- plaintextResult
      outputs   <- OutputMap.fromJson(masked, plaintext)
    yield outputs

  protected[auto] def repo: Option[GitRepo]
  protected[auto] def remoteEnvVars: Map[String, EnvVarValue]
  protected[auto] def preRunCommands: List[String]
  protected[auto] def remoteSkipInstallDependencies: Boolean

end LocalWorkspace

object LocalWorkspace:
  // minimal version of the Pulumi CLI that supports the Automation API
  private val MinVersion = model.SemanticVersion(3, 2, 0)
  // the extensions for Pulumi settings files
  private val SettingsExtensions = List(".yaml", ".yml", ".json")

  /** Creates a new LocalWorkspace with the default options.
    *
    * @param options
    *   the configuration options for the workspace
    * @return
    *   a new LocalWorkspace
    */
  def apply(options: LocalWorkspaceOption*): Either[Exception, LocalWorkspace] =
    val opts = LocalWorkspaceOptions.from(options*)

    val workDir: os.Path = {
      val wd: os.Path = opts.workDir match
        case NotProvided => os.temp.dir(prefix = "pulumi_auto")
        case wd: os.Path => wd

      // if we have a repo, but no remote, then we need to clone the repo
      opts.repo match
        case repo: GitRepo if !opts.remote =>
          // now do the git clone
          Git.setupGitRepo(wd, repo) match
            case Left(e)           => throw AutoError(s"failed to create workspace, unable to enlist in git repo: ${e.getMessage}", e)
            case Right(projectDir) => projectDir
        case _ => wd
    }

    val pulumiHome = opts.pulumiHome.asOption
    val envVars    = AtomicReference(mutable.Map.from(opts.envVars))

    // optOut indicates we should skip the version check.
    val optOut: Boolean = shell.pulumi.env.pulumiAutomationApiSkipVersionCheck ||
      opts.envVars
        .get(shell.pulumi.env.PulumiAutomationApiSkipVersionCheckEnv)
        .map(isTruthy)
        .getOrElse(false)

    val maybeCurrentVersion: Either[Exception, model.SemanticVersion] = pulumiVersion match
      case Left(e) => Left(AutoError(s"failed to create workspace, unable to determine pulumi version: ${e.getMessage}", e))
      case Right(v) =>
        parseAndValidatePulumiVersion(MinVersion, v, optOut) match
          case Left(e) => Left(AutoError(s"failed to create workspace, pulumi version is not supported: ${e.getMessage}", e))
          case r       => r

    val remoteSupportChecked: Either[Exception, Unit] =
      if !optOut && opts.remote then
        // See if `--remote` is present in `pulumi preview --help`'s output.
        supportsPulumiCmdFlag("--remote", "preview") match
          case Left(e) =>
            Left(AutoError(s"failed to create workspace, unable to determine pulumi remote support: ${e.getMessage}", e))
          case Right(false) => Left(AutoError("Pulumi CLI does not support remote operations; please upgrade"))
          case Right(true)  => Right(())
        end match
      else Right(())

    val maybeWorkspace = for
      currentVersion <- maybeCurrentVersion
      _              <- remoteSupportChecked
    yield InitializedLocalWorkspace(
      workDir = workDir,
      pulumiHome = pulumiHome,
      envVars = envVars,
      pulumiVersion = currentVersion,
      program = opts.program.asOption,
      remote = opts.remote,
      project = opts.project.asOption,
      stacks = opts.stacks,
      rawSecretsProvider = AtomicReference(opts.secretsProvider.asOption),
      repo = opts.repo.asOption,
      remoteEnvVars = opts.remoteEnvVars,
      preRunCommands = opts.preRunCommands,
      remoteSkipInstallDependencies = opts.remoteSkipInstallDependencies
    )

    // save the project settings if provided
    def saveProjectSettings(ws: Workspace): Either[Exception, Unit] =
      opts.project match
        case project: Project =>
          ws.saveProjectSettings(project) match
            case Left(e)  => Left(AutoError(s"failed to create workspace, unable to save project settings: ${e.getMessage}", e))
            case Right(_) => Right(()) // OK, saved
        case NotProvided => Right(()) // no project to save, skip
      end match
    end saveProjectSettings

    // save all provided stack settings
    def saveAllStackSettings(ws: Workspace): Either[Exception, Unit] =
      def save(el: (String, ProjectStack)) =
        val (stackName, stack) = el
        ws.saveStackSettings(stackName, stack).left.map { e =>
          AutoError(s"failed to create workspace, unable to save stack settings: ${e.getMessage}", e)
        }
      end save

      val it = opts.stacks.iterator
      while it.hasNext do
        save(it.next()) match
          case left @ Left(_) => return left
          case Right(_)       => // OK, saved
      Right(())
    end saveAllStackSettings

    // run setup callback if we have a repo and we're not remote
    def runRepoSetupFn(ws: Workspace): Either[Exception, Unit] =
      opts.repo match
        case repo: GitRepo if !opts.remote =>
          repo.setup.asOption match
            // now run the setup callback
            case Some(f: SetupFn) =>
              f.apply(ws).left.map { e =>
                AutoError(s"failed to create workspace, error while running git repository setup function: ${e.getMessage}", e)
              }
            case None => Right(()) // no repo setup callback, skip
        case _ => Right(()) // no repo to setup or remote, skip the setup callback
      end match
    end runRepoSetupFn

    for
      ws <- maybeWorkspace
      _  <- saveProjectSettings(ws)
      _  <- saveAllStackSettings(ws)
      _  <- runRepoSetupFn(ws)
    yield ws
  end apply

  private def pulumiVersion: Either[Exception, String] =
    shell.pulumi("version")() match
      case Left(e)  => Left(e.withMessage("could not determine pulumi version"))
      case Right(r) => Right(r.out.trim)
  end pulumiVersion

  private def parseAndValidatePulumiVersion(
    minimalVersion: model.SemanticVersion,
    currentVersion: String,
    optOut: Boolean
  ): Either[Exception, model.SemanticVersion] =
    model.SemanticVersion.parseTolerant(currentVersion) match
      case Left(e) =>
        Left(
          AutoError(
            s"Unable to parse Pulumi CLI version (skip with ${shell.pulumi.env.PulumiAutomationApiSkipVersionCheckEnv}=true): ${e.getMessage}"
          )
        )
      case r @ Right(v) =>
        if !optOut && v < minimalVersion then
          Left(
            AutoError(
              s"Pulumi CLI version is too old (skip with ${shell.pulumi.env.PulumiAutomationApiSkipVersionCheckEnv}=true): ${currentVersion} < ${v}"
            )
          )
        else r
  end parseAndValidatePulumiVersion

  /** Checks if a specified flag is supported by running a command with `--help` and checking if the flag is found within the resulting
    * output.
    *
    * @param flag
    *   The flag to check for support
    * @param additional
    *   The additional command arguments
    * @return
    *   A boolean indicating if the flag is supported or an exception if the command failed
    */
  private def supportsPulumiCmdFlag(flag: String, additional: os.Shellable*): Either[Exception, Boolean] =
    import shell.ShellOption.*
    // Run the command with `--help`, and then we'll look for the flag in the output.
    val allArgs: Seq[os.Shellable] = "--help" +: additional
    shell.pulumi(allArgs*)(
      Env(shell.pulumi.env.PulumiDebugCommandsEnv, "true"),
      Env(shell.pulumi.env.PulumiExperimentalEnv, "true")
    ) match
      case Left(e)  => Left(e.withMessage(s"could not determine pulumi supported flags"))
      case Right(r) =>
        // Does the help test in stdout mention the flag? If so, assume it's supported.
        Right(r.out.contains(flag))

  end supportsPulumiCmdFlag

  private[auto] final case class InitializedLocalWorkspace private[auto] (
    workDir: os.Path,
    pulumiHome: Option[os.Path],
    protected val envVars: AtomicReference[mutable.Map[String, String]],
    program: Option[RunFunc],
    protected val rawSecretsProvider: AtomicReference[Option[String]],
    pulumiVersion: model.SemanticVersion,
    remote: Boolean,
    project: Option[Project],
    stacks: Map[String, ProjectStack],
    repo: Option[GitRepo],
    remoteEnvVars: Map[String, EnvVarValue],
    preRunCommands: List[String],
    remoteSkipInstallDependencies: Boolean
  ) extends LocalWorkspace

  // stack names come in many forms:
  // s, o/p/s, u/p/s o/s
  // so just return the last chunk which is what will be used in pulumi.<stack>.yaml
  private def getStackSettingsName(stackName: String) = StackName(stackName).parts._3

  private def readProjectSettingsFromDir(workDir: os.Path): Either[Exception, Project] =
    SettingsExtensions
      .map { ext => workDir / shell.pulumi.ProjectFileName(ext) }
      .collectFirst {
        case projectPath if os.exists(projectPath) =>
          Project
            .load(projectPath)
            .left
            .map(e => AutoError(s"failed to load project settings: ${e.getMessage}", e))
      } match
      case Some(value) => value
      case None        => Left(AutoError("unable to find project settings in workspace"))
  end readProjectSettingsFromDir

  private def readStackSettingFromDir(workDir: os.Path, stackName: String): Either[Exception, ProjectStack] =
    val actualStackName = getStackSettingsName(stackName)
    SettingsExtensions
      .map { ext => workDir / shell.pulumi.StackFileName(actualStackName, ext) }
      .collectFirst {
        case projectPath if os.exists(projectPath) =>
          ProjectStack
            .load(projectPath)
            .left
            .map(e => AutoError(s"failed to load stack settings: ${e.getMessage}", e))
      } match
      case Some(value) => value
      case None        => Left(AutoError("unable to find stack settings in workspace"))

  private[auto] def getProjectSettings(
    projectName: String,
    opts: Seq[LocalWorkspaceOption]
  ): Either[Exception, Project] =
    val options = LocalWorkspaceOptions.from(opts*)

    options.project match
      // If the Project is included in the opts, just use that.
      case project: Project => Right(project)
      case NotProvided      =>
        // If WorkDir is specified, try to read any existing project settings before resorting to
        // creating a default project.
        options.workDir match
          case path: os.Path =>
            readProjectSettingsFromDir(path) match
              case Left(AutoError(Some(msg), _)) if msg == "unable to find project settings in workspace" =>
                defaultInlineProject(projectName)
              case Left(e) => Left(AutoError(s"failed to load project '$projectName' settings: $e", e))
              case r       => r
          case NotProvided =>
            // If there was no workdir specified, create the default project.
            defaultInlineProject(projectName)
  end getProjectSettings

  private def defaultInlineProject(projectName: String): Either[Exception, Project] =
    Try {
      Project(
        name = PackageName.parse(projectName),
        runtime = "scala",
        main = Some(os.pwd.toString())
      )
    }.toEither.left.map(e => AutoError(s"failed to create default project settings: ${e.getMessage}", e))
  end defaultInlineProject
end LocalWorkspace

sealed trait LocalWorkspaceOption
object LocalWorkspaceOption:

  /** The directory to execute commands from and store state. Defaults to a tmp dir.
    * @param path
    *   work directory to use
    */
  case class WorkDir(path: os.Path) extends LocalWorkspaceOption

  /** The Pulumi program to execute. If none is supplied, the program identified in `$WORKDIR/Pulumi.yaml` will be used instead.
    * @param program
    *   the Pulumi program to execute
    */
  // not a case class because function won't be useful in equality checks
  class Program(val program: RunFunc) extends LocalWorkspaceOption
  object Program:
    def apply(program: RunFunc): Program     = new Program(program)
    def unapply(p: Program): Option[RunFunc] = Some(p.program)

  /** The path to the Pulumi home directory.
    *
    * Overrides the metadata directory for pulumi commands. This customizes the location of `$PULUMI_HOME` where metadata is stored and
    * plugins are installed. If not provided, will be read from the environment variable `PULUMI_HOME` or default to `~/.pulumi`
    */
  case class PulumiHome(path: os.Path) extends LocalWorkspaceOption

  /** The project settings for the workspace.
    */
  case class Project(project: besom.auto.Project) extends LocalWorkspaceOption

  /** A map of `[stackName -> stack settings objects]` to seed the workspace.
    */
  case class Stacks(stacks: Map[String, besom.auto.ProjectStack]) extends LocalWorkspaceOption

  /** A git repo with a Pulumi Project to clone into the `workDir`.
    */
  case class Repo(repo: GitRepo) extends LocalWorkspaceOption

  /** The Secrets Provider to use with the current Stack.
    */
  case class SecretsProvider(provider: String) extends LocalWorkspaceOption

  /** A map of environment values scoped to the workspace. These values will be passed to all Workspace and Stack level commands.
    */
  case class EnvVars(envVars: Map[String, String]) extends LocalWorkspaceOption
  object EnvVars:
    def apply(env: (String, String)*): EnvVars     = new EnvVars(env.toMap)
    def apply(key: String, value: String): EnvVars = EnvVars(Map(key -> value))

  /** Whether the workspace represents a remote workspace.
    */
  case object Remote extends LocalWorkspaceOption

  /** Remote environment variables to be passed to the remote Pulumi operation.
    */
  case class RemoteEnvVars(envVars: Map[String, EnvVarValue]) extends LocalWorkspaceOption

  /** An optional list of arbitrary commands to run before the remote Pulumi operation is invoked.
    */
  case class PreRunCommands(commands: List[String]) extends LocalWorkspaceOption

  /** Sets whether to skip the default dependency installation step.
    */
  case object RemoteSkipInstallDependencies extends LocalWorkspaceOption

end LocalWorkspaceOption

/** The configuration options for a [[LocalWorkspace]].
  *
  * @param workDir
  *   the directory to execute commands from and store state. Defaults to a tmp dir.
  * @param program
  *   the Pulumi Program to execute. If none is supplied, the program identified in `$WORKDIR/Pulumi.yaml` will be used instead.
  * @param pulumiHome
  *   the path to the Pulumi home directory.* Overrides the metadata directory for pulumi commands. This customizes the location of
  *   `$PULUMI_HOME` where metadata is stored and plugins are installed. If not provided, will be read from the environment variable
  *   `PULUMI_HOME` or default to `~/.pulumi`
  * @param project
  *   the project settings for the workspace.
  * @param stacks
  *   a map of `[stackName -> stack settings objects]` to seed the workspace.
  * @param repo
  *   a git repo with a Pulumi Project to clone into the `workDir`.
  * @param secretsProvider
  *   the Secrets Provider to use with the current Stack.
  * @param envVars
  *   a map of environment values scoped to the workspace. These values will be passed to all Workspace and Stack level commands.
  * @param remote
  *   whether the workspace represents a remote workspace.
  * @param remoteEnvVars
  *   remote environment variables to be passed to the remote Pulumi operation.
  * @param preRunCommands
  *   an optional list of arbitrary commands to run before the remote Pulumi operation is invoked.
  * @param remoteSkipInstallDependencies
  *   sets whether to skip the default dependency installation step.
  */
case class LocalWorkspaceOptions(
  workDir: NotProvidedOr[os.Path] = NotProvided,
  program: NotProvidedOr[RunFunc] = NotProvided,
  pulumiHome: NotProvidedOr[os.Path] = NotProvided,
  project: NotProvidedOr[Project] = NotProvided,
  stacks: Map[String, ProjectStack] = Map.empty,
  repo: NotProvidedOr[GitRepo] = NotProvided,
  secretsProvider: NotProvidedOr[String] = NotProvided,
  envVars: Map[String, String] = Map.empty,
  remote: Boolean = false,
  remoteEnvVars: Map[String, EnvVarValue] = Map.empty,
  preRunCommands: List[String] = List.empty,
  remoteSkipInstallDependencies: Boolean = false
)
object LocalWorkspaceOptions:
  /** Merge options, last specified value wins
    * @return
    *   a new [[LocalWorkspaceOptions]]
    */
  def from(opts: LocalWorkspaceOption*): LocalWorkspaceOptions = from(opts.toList)
  def from(opts: List[LocalWorkspaceOption]): LocalWorkspaceOptions =
    opts match
      case LocalWorkspaceOption.WorkDir(path) :: tail                 => from(tail).copy(workDir = path)
      case LocalWorkspaceOption.Program(program) :: tail              => from(tail).copy(program = program)
      case LocalWorkspaceOption.PulumiHome(path) :: tail              => from(tail).copy(pulumiHome = path)
      case LocalWorkspaceOption.Project(project) :: tail              => from(tail).copy(project = project)
      case LocalWorkspaceOption.Stacks(stacks) :: tail                => from(tail).copy(stacks = stacks)
      case LocalWorkspaceOption.Repo(repo) :: tail                    => from(tail).copy(repo = repo)
      case LocalWorkspaceOption.SecretsProvider(provider) :: tail     => from(tail).copy(secretsProvider = provider)
      case LocalWorkspaceOption.Remote :: tail                        => from(tail).copy(remote = true)
      case LocalWorkspaceOption.RemoteEnvVars(envVars) :: tail        => from(tail).copy(remoteEnvVars = envVars)
      case LocalWorkspaceOption.PreRunCommands(commands) :: tail      => from(tail).copy(preRunCommands = commands)
      case LocalWorkspaceOption.RemoteSkipInstallDependencies :: tail => from(tail).copy(remoteSkipInstallDependencies = true)
      case LocalWorkspaceOption.EnvVars(env) :: tail => {
        val old = from(tail*)
        old.copy(envVars = old.envVars ++ env)
      }
      case Nil => LocalWorkspaceOptions()
      case o   => throw AutoError(s"Unknown LocalWorkspaceOption: $o")

/** GitRepo contains info to acquire and setup a Pulumi program from a git repository.
  *
  * @param url
  *   URL to clone git repo
  * @param projectPath
  *   Optional path relative to the repo root specifying location of the pulumi program. Specifying this option will update the
  *   [[Workspace.workDir]] accordingly.
  * @param branch
  *   Optional branch to checkout.
  * @param commitHash
  *   Optional commit to checkout.
  * @param setup
  *   Optional function to execute after enlisting in the specified repo.
  * @param auth
  *   GitAuth is the different Authentication options for the Git repository
  * @param shallow
  *   Shallow disables fetching the repo's entire history.
  */
case class GitRepo(
  url: String,
  projectPath: NotProvidedOr[String] = NotProvided,
  branch: NotProvidedOr[String] = NotProvided,
  commitHash: NotProvidedOr[String] = NotProvided,
  setup: NotProvidedOr[SetupFn] = NotProvided,
  auth: NotProvidedOr[GitAuth] = NotProvided,
  shallow: Boolean = false
)

type SetupFn = Workspace => Either[Exception, Unit]

/** GitAuth is the authentication details that can be specified for a private Git repo.
  *
  * There are 3 different authentication paths:
  *   - [[PersonalAccessToken]]
  *   - [[SSHPrivateKeyPath]] or [[SSHPrivateKey]] (and it's potential passphrase)
  *   - [[UsernameAndPassword]]
  */
sealed trait GitAuth
object GitAuth:

  /** Use GitHub Personal Access Token
    * @param token
    *   a GitHub personal access token in replacement of your password
    */
  case class PersonalAccessToken(token: String) extends GitAuth

  /** Use SSH Private Key
    *
    * When using [[SSHPrivateKeyPath]], the URL of the repository must be in the format `git@github.com:org/repository.git` - if the url is
    * not in this format, then an error 'unable to clone repo: invalid auth method' will be returned
    *
    * @param path
    *   the absolute path to a private key for access to the git repo
    * @param passphrase
    *   the optional passphrase for the SSH Private Key
    */
  case class SSHPrivateKeyPath(path: String, passphrase: NotProvidedOr[String]) extends GitAuth

  /** Use SSH Private Key
    *
    * When using [[SSHPrivateKey]] the URL of the repository must be in the format `git@github.com:org/repository.git` - if the url is not
    * in this format, then an error 'unable to clone repo: invalid auth method' will be returned
    * @param key
    *   the (contents) private key for access to the git repo
    * @param passphrase
    *   the optional passphrase for the SSH Private Key
    */
  case class SSHPrivateKey(key: String, passphrase: NotProvidedOr[String]) extends GitAuth

  /** Use Username and Password
    * @param username
    *   the username to use when authenticating to a git repository
    * @param password
    *   the password that pairs with a username for authentication to a git repository
    */
  case class UsernameAndPassword(username: String, password: String) extends GitAuth
end GitAuth

/** EnvVarValue represents the value of an envvar. A value can be a secret, which is passed along to remote operations when used with remote
  * workspaces, otherwise, it has no effect.
  *
  * @param value
  *   the value of the environment variable
  * @param secret
  *   a boolean indicating whether the value is a secret or not
  */
case class EnvVarValue(value: String, secret: Boolean = false)
