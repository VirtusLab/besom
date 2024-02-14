package besom.auto

import besom.json.*
import besom.json.DefaultJsonProtocol.*
import besom.model
import besom.util.*
import org.virtuslab.yaml.*

import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.Try

// FIXME: this is a hack to make the compiler happy

object yamlHack:
  import org.virtuslab.yaml.internal.dump.present.PresenterImpl
  import besom.auto.internal.HackedSerializerImpl

  def asYaml(node: Node): String =
    val events = HackedSerializerImpl.toEvents(node)
    PresenterImpl.asString(events)
end yamlHack

given JsonProtocol = DefaultJsonProtocol

given JsonFormat[Any] = new JsonFormat[Any]:
  override def write(obj: Any): JsValue =
    obj match
      case s: String    => JsString(s)
      case i: Int       => JsNumber(i)
      case b: Boolean   => JsBoolean(b)
      case a: List[Any] => JsArray(a.map(write).toVector)
//      case m: Map[String, Any] => JsObject(m.map((k, v) => (k, write(v))))
  override def read(json: JsValue): Any =
    json match
      case JsNull       => None
      case JsTrue       => true
      case JsFalse      => false
      case JsBoolean(b) => b
      case JsString(s)  => s
      case JsNumber(i)  => i
      case JsArray(a)   => a.map(read)
      case JsObject(fs) => fs.map((k, v) => (k, read(v)))

implicit def forOption[T](implicit encoder: YamlEncoder[T]): YamlEncoder[Option[T]] = {
  case Some(value) => encoder.asNode(value)
  case None        => Node.ScalarNode(null)
}

implicit def forAny: YamlEncoder[Any] = {
  case value: String    => YamlEncoder.forString.asNode(value)
  case value: Int       => YamlEncoder.forInt.asNode(value)
  case value: Boolean   => YamlEncoder.forBoolean.asNode(value)
  case value: List[Any] => YamlEncoder.forList[Any].asNode(value)
//  case value: Map[String, Any] => YamlEncoder.forMap[String, Any].asNode(value)
}

given JsonFormat[model.QName] = new JsonFormat[model.QName]:
  override def write(obj: model.QName): JsValue = JsString(obj)
  override def read(json: JsValue): model.QName = json match
    case JsString(s) => model.QName.parse(s)
    case _           => throw new RuntimeException("QName must be a string")
given YamlCodec[model.QName] = new YamlCodec[model.QName]:
  def asNode(obj: model.QName): Node = summon[YamlEncoder[String]].asNode(obj)
  def construct(node: Node)(implicit settings: LoadSettings = LoadSettings.empty): Either[ConstructError, model.QName] =
    summon[YamlDecoder[String]].construct(node).map(model.QName.parse(_))

given JsonFormat[String | ProjectRuntimeInfo] = new JsonFormat[String | ProjectRuntimeInfo]:
  override def write(obj: String | ProjectRuntimeInfo): JsValue = obj match
    case s: String                => JsString(s)
    case ProjectRuntimeInfo(n, _) => JsString(n)
  override def read(json: JsValue): String | ProjectRuntimeInfo = json match
    case JsString(s) => s
    case _           => throw new RuntimeException("ProjectRuntimeInfo must be a string")

given YamlCodec[String | ProjectRuntimeInfo] = new YamlCodec[String | ProjectRuntimeInfo]:
  def asNode(obj: String | ProjectRuntimeInfo): Node = obj match
    case s: String             => summon[YamlEncoder[String]].asNode(s)
    case p: ProjectRuntimeInfo => summon[YamlEncoder[ProjectRuntimeInfo]].asNode(p)
  def construct(node: Node)(implicit settings: LoadSettings = LoadSettings.empty): Either[ConstructError, String | ProjectRuntimeInfo] =
    summon[YamlDecoder[String]].construct(node).map(s => ProjectRuntimeInfo(s, Map.empty))

given JsonFormat[java.time.Instant] = new JsonFormat[java.time.Instant]:
  override def write(obj: java.time.Instant): JsValue = JsString(obj.toString)
  override def read(json: JsValue): java.time.Instant = json match
    case JsString(s) => java.time.Instant.parse(s)
    case _           => throw new RuntimeException("Instant must be a string")

// FIXME: end of hack

/** Workspace is the execution context containing a single Pulumi project, a program, and multiple stacks.
  *
  * Workspaces are used to manage the execution environment, providing various utilities such as plugin installation, environment
  * configuration `$PULUMI_HOME`, and creation, deletion, and listing of Stacks.
  */
trait Workspace:

  protected[auto] def pulumi(additional: os.Shellable*)(opts: shell.ShellOption*): Either[ShellAutoError, shell.Result] =
    val allArgs = additional
    val allOpts = opts ++ List(
      shell.ShellOption.Cwd(workDir)
    ) ++ List(
      shell.ShellOption.Env(getEnvVars),
      shell.ShellOption.Env(shell.pulumi.env.PulumiDebugCommandsEnv -> "true")
    ) ++ pulumiHome.map(path => shell.ShellOption.Env(shell.pulumi.env.PulumiHomeEnv, path.toString))
      ++ Option.when(remote)(shell.ShellOption.Env(shell.pulumi.env.PulumiExperimentalEnv -> "true"))
    shell.pulumi(allArgs)(allOpts*)
  end pulumi

  /** Returns the settings object for the current project if any. */
  def projectSettings: Either[Exception, Project]

  /** Overwrites the settings object in the current project. There can only be a single project per workspace. Fails is new project name
    * does not match old.
    */
  def saveProjectSettings(project: Project): Either[Exception, Unit]

  /** Returns the settings object for the stack matching the specified stack name if any. */
  def stackSettings(stackName: String): Either[Exception, ProjectStack]

  /** Overwrites the settings object for the stack matching the specified stack name. */
  def saveStackSettings(stackName: String, projectStack: ProjectStack): Either[Exception, Unit]

  /** This is hook to provide additional args to every CLI commands before they are executed. Provided with stack name, returns a list of
    * args to append to an invoked command `["--config=...", ]`.
    */
  def serializeArgsForOp(stackName: String): Either[Exception, List[String]]

  /** This is a hook executed after every command. Called with the stack name. An extensibility point to perform workspace cleanup (CLI
    * operations may create/modify a `Pulumi.[stack].yaml`).
    */
  def postCommandCallback(stackName: String): Either[Exception, Unit]

  /** Adds the specified environments to the provided stack's configuration. */
  def addEnvironments(stackName: String, envs: String*): Either[Exception, Unit]

  /** ListEnvironments returns the list of environments from the provided stack's configuration */
  def listEnvironments(stackName: String): Either[Exception, List[String]]

  /** Removes the specified environment from the provided stack's configuration. */
  def removeEnvironment(stackName: String, env: String): Either[Exception, Unit]

  /** Returns the value associated with the specified stack name and key using the optional [[ConfigOption]], scoped to the current
    * workspace.
    */
  def getConfig(stackName: String, key: String, options: ConfigOption*): Either[Exception, ConfigValue]

  /** Returns the config map for the specified stack name, scoped to the current workspace. */
  def getAllConfig(stackName: String): Either[Exception, ConfigMap]

  /** Sets the specified key-value pair on the provided stack name using the optional ConfigOptions.
    */
  def setConfig(
    stackName: String,
    key: String,
    value: ConfigValue,
    options: ConfigOption*
  ): Either[Exception, Unit]

  /** Sets all values in the provided config map for the specified stack name using the optional ConfigOptions.
    */
  def setAllConfig(
    stackName: String,
    config: ConfigMap,
    options: ConfigOption*
  ): Either[Exception, Unit]

  /** Removes the specified key-value pair on the provided stack name using the optional ConfigOptions.
    */
  def removeConfig(stackName: String, key: String, options: ConfigOption*): Either[Exception, Unit]

  /** Removes all values in the provided key list for the specified stack name using the optional ConfigOptions.
    */
  def removeAllConfig(
    stackName: String,
    keys: List[String],
    options: ConfigOption*
  ): Either[Exception, Unit]

  /** Gets and sets the config map used with the last Update for Stack matching stack name. */
  def refreshConfig(stackName: String): Either[Exception, ConfigMap]

  /** Returns the value associated with the specified stack name and key. */
  def getTag(stackName: String, key: String): Either[Exception, String]

  /** Sets the specified key-value pair on the provided stack name. */
  def setTag(stackName: String, key: String, value: String): Either[Exception, Unit]

  /** Removes the specified key-value pair on the provided stack name. */
  def removeTag(stackName: String, key: String): Either[Exception, Unit]

  /** Returns the tag map for the specified stack name. */
  def listTags(stackName: String): Either[Exception, Map[String, String]]

  /** Returns the environment values scoped to the current workspace. */
  def getEnvVars: Map[String, String]

  /** Sets the specified map of environment values scoped to the current workspace. These values will be passed to all Workspace and Stack
    * level commands.
    */
  def setEnvVars(envVars: Map[String, String]): Unit

  /** Sets the specified environment value scoped to the current workspace. This value will be passed to all Workspace and Stack level
    * commands.
    */
  def setEnvVar(key: String, value: String): Unit

  /** Unsets the specified environment value scoped to the current workspace. This value will be removed from all Workspace and Stack level
    * commands.
    */
  def unsetEnvVar(key: String): Unit

  /** Returns the working directory to run Pulumi CLI commands. */
  def workDir: os.Path

  /** Returns the directory override for CLI metadata if set. This customizes the location of $PULUMI_HOME where metadata is stored and
    * plugins are installed.
    */
  def pulumiHome: Option[os.Path]

  /** The secrets provider to use for encryption and decryption of stack secrets. See:
    * https://www.pulumi.com/docs/intro/concepts/secrets/#available-encryption-providers
    */
  def secretsProvider: Option[String]

  /** Returns the version of the underlying Pulumi CLI/Engine. */
  def pulumiVersion: model.SemanticVersion

  /** Returns detailed information about the currently logged-in Pulumi identity.
    */
  def whoAmI: Either[Exception, WhoAmIResult]

  /** Edits the secrets provider for the given stack. */
  def changeStackSecretsProvider(
    stackName: String,
    newSecretsProvider: String | SecretsProviderType,
    options: ChangeSecretsProviderOption*
  ): Either[Exception, Unit]

  /** Returns a summary of the currently selected stack, if any. */
  def stack: Either[Exception, Option[StackSummary]]

  /** Creates and sets a new stack with the stack name, failing if one already exists. */
  def createStack(stackName: String): Either[Exception, Stack]

  /** Selects and sets an existing stack matching the stack name, failing if none exists. */
  def selectStack(stackName: String): Either[Exception, Stack]

  /** Deletes the stack and all associated configuration and history. */
  def removeStack(stackName: String, options: RemoveOption*): Either[Exception, Unit]

  /** Returns all Stacks created under the current Project. This queries underlying backend and may return stacks not present in the
    * Workspace.
    */
  def listStacks: Either[Exception, List[StackSummary]]

  /** Acquires the plugin matching the specified name and version. */
  def installPlugin(
    name: String,
    version: String,
    kind: String = "resource",
    server: NotProvidedOr[String] = NotProvided
  ): Either[Exception, Unit]

  /** Deletes the plugin matching the specified name and version. */
  def removePlugin(
    name: NotProvidedOr[String] = NotProvided,
    versionRange: NotProvidedOr[String] = NotProvided,
    kind: String = "resource"
  ): Either[Exception, Unit]

  /** Lists all installed plugins. */
  def listPlugins: Either[Exception, List[PluginInfo]]

  /** Program returns the program `pulumi.RunFunc` to be used for Preview/Update if any. If none is specified, the stack will refer to
    * ProjectSettings for this information.
    */
  def program: Option[RunFunc]

  /** Exports the deployment state of the stack matching the given name. This can be combined with ImportStack to edit a stack's state (such
    * as recovery from failed deployments).
    */
  def exportStack(stackName: String): Either[Exception, UntypedDeployment]

  /** Imports the specified deployment state into a pre-existing stack. This can be combined with ExportStack to edit a stack's state (such
    * as recovery from failed deployments).
    */
  def importStack(stackName: String, deployment: UntypedDeployment): Either[Exception, Unit]

  /** Gets the current set of Stack outputs from the last Stack.Up().
    */
  def stackOutputs(stackName: String): Either[Exception, OutputMap]

  protected[auto] def remote: Boolean

end Workspace

/** ConfigValue is a configuration value used by a Pulumi program. Allows differentiating between secret and plaintext values by setting the
  * `Secret` property.
  */
case class ConfigValue(value: String, secret: Boolean = false) derives JsonFormat
object ConfigValue:
  def fromJson(json: String): Either[Exception, ConfigValue] = json.parseJson[ConfigValue]

/** ConfigOptions is a configuration option used by a Pulumi program. */
enum ConfigOption:
  /** Indicates that the key contains a path to a property in a map or list to get/set
    */
  case Path

/** ConfigMap is a map of ConfigValue used by Pulumi programs. Allows differentiating between secret and plaintext values.
  */
type ConfigMap = Map[String, ConfigValue]
object ConfigMap:
  def fromJson(json: String): Either[Exception, ConfigMap] = json.parseJson[ConfigMap]

/** TagMap is a key-value map of tag metadata associated with a stack.
  */
type TagMap = Map[String, String]
object TagMap:
  def fromJson(json: String): Either[Exception, TagMap] = json.parseJson[TagMap]

/** StackSummary is a description of a stack and its current status.
  */
case class StackSummary(
  name: String,
  current: Boolean,
  lastUpdate: Option[String],
  updateInProgress: Boolean,
  resourceCount: Option[Int],
  url: Option[String]
) derives JsonFormat
object StackSummary:
  def fromJsonList(json: String): Either[Exception, List[StackSummary]] =
    json.parseJson[List[StackSummary]]

/** WhoAmIResult contains detailed information about the currently logged-in Pulumi identity.
  */
case class WhoAmIResult(user: String, organizations: List[String], url: String) derives JsonFormat
object WhoAmIResult:
  def fromJson(json: String): Either[Exception, WhoAmIResult] = json.parseJson[WhoAmIResult]

/** Basic secret provider types */
enum SecretsProviderType(val value: String):
  override def toString: String = value
  case Default extends SecretsProviderType("default")
  case Passphrase extends SecretsProviderType("passphrase")
  case AwsKms extends SecretsProviderType("awskms")
  case AzureKeyVault extends SecretsProviderType("azurekeyvault")
  case GcpKms extends SecretsProviderType("gcpkms")
  case HashiVault extends SecretsProviderType("hashivault")
object SecretsProviderType:
  implicit inline def spt2Str(inline spt: String | SecretsProviderType): String = spt.toString

/** Options for changing the secrets provider.
  */
sealed trait ChangeSecretsProviderOption

/** Represents the new passphrase when changing to a `passphrase` provider.
  *
  * @param newPassphrase
  *   The new passphrase.
  */
case class NewPassphraseOption(newPassphrase: String) extends ChangeSecretsProviderOption

/** Project is a Pulumi project manifest.
  *
  * JSON Schema is available at https://github.com/pulumi/pulumi/blob/v3.98.0/sdk/go/common/workspace/project.json
  *
  * @param name
  *   is a required fully qualified name of the project containing alphanumeric characters, hyphens, underscores, and periods.
  * @param runtime
  *   is a required runtime that executes code, e.g.: scala, nodejs, python, go, dotnet, java or yaml.
  * @param main
  *   is an optional override for the program's main entry-point location
  * @param description
  *   is an optional informational description
  * @param author
  *   is an optional author that created this project
  * @param website
  *   is an optional website for additional info about this project
  * @param license
  *   is the optional license governing this project's usage
  * @param config
  *   config is a map of config property keys to either values or structured declarations. Non-object values are allowed to be set directly.
  *   Anything more complex must be defined using the structured schema declaration, or the nested value declaration both shown below.
  * @param stackConfigDir
  *   indicates where to store the `Pulumi.[stack-name].yaml` files, combined with the folder `Pulumi.yaml` is in
  * @param template
  *   is an optional template manifest, if this project is a template
  * @param backend
  *   is an optional backend configuration
  * @param options
  *   is an optional set of project options
  * @param plugins
  *   contains available plugins
  */
case class Project(
  name: model.PackageName,
  runtime: String | ProjectRuntimeInfo,
  main: Option[String] = None,
  description: Option[String] = None,
  author: Option[String] = None,
  website: Option[String] = None,
  license: Option[String] = None,
  config: Map[String, ProjectConfigValue] = Map.empty,
  stackConfigDir: Option[String] = None,
  template: Option[ProjectTemplate] = None,
  backend: Option[ProjectBackend] = None,
  options: Option[ProjectOptions] = None,
  plugins: Option[Plugins] = None
) derives JsonFormat,
      YamlCodec:
  def save(path: os.Path): Either[Exception, Unit] =
    val content = path match
      case p if p.ext == "json"                   => summon[JsonWriter[Project]].write(this).prettyPrint
      case p if p.ext == "yaml" || p.ext == "yml" => yamlHack.asYaml(summon[YamlEncoder[Project]].asNode(this))
    end content
    Try {
      os.write.over(path, content, createFolders = true)
    }.toEither.left.map(e => AutoError(s"Failed to write project to '$path'", e))
  end save

end Project
object Project:
  def load(path: os.Path): Either[Exception, Project] =
    path match
      case p if p.ext == "json" =>
        Try {
          os.read(p)
        }.toEither.left
          .map(e => AutoError(s"Failed to read project from '$path'", e))
          .flatMap(_.parseJson[Project].left.map(e => AutoError(s"Failed to parse project from '$path'", e)))
      case p if p.ext == "yaml" || p.ext == "yml" =>
        Try {
          os.read(p)
        }.toEither.left
          .map(e => AutoError(s"Failed to read project from '$path'", e))
          .flatMap(_.as[Project].left.map(e => AutoError(s"Failed to read project from '$path': ${e.msg}")))

/** ProjectRuntimeInfo is a configuration for the runtime used by the project
  * @param name
  *   required language runtime of the project, e.g: scala, nodejs, python, go, dotnet, java or yaml.
  * @param options
  *   The runtime attribute has an additional property called options where you can further specify runtime configuration.
  */
case class ProjectRuntimeInfo(
  name: String,
  options: Map[String, String] = Map.empty
) derives JsonFormat,
      YamlCodec

/** A config value included in the project manifest.
  *
  * @param type
  *   The type of this config property, either string, boolean, integer, or array.
  * @param description
  *   A description for this config property.
  * @param items
  *   A nested structured declaration of the type of the items in the array. Required if type is array
  * @param default
  *   The default value for this config property, must match the given type.
  * @param value
  *   The value of this configuration property.
  */
sealed trait ProjectConfigValue
object ProjectConfigValue:
  case class StringValue(
    description: Option[String] = None,
    default: Option[String] = None,
    value: Option[String] = None,
    secret: Boolean = false
  ) extends ProjectConfigValue
      derives JsonFormat,
        YamlCodec
  case class BooleanValue(
    description: Option[String] = None,
    default: Option[Boolean] = None,
    value: Option[Boolean] = None,
    secret: Boolean = false
  ) extends ProjectConfigValue
      derives JsonFormat,
        YamlCodec
  case class IntegerValue(
    description: Option[String] = None,
    default: Option[Int] = None,
    value: Option[Int] = None,
    secret: Boolean = false
  ) extends ProjectConfigValue
      derives JsonFormat,
        YamlCodec
  case class ArrayValue(
    items: ProjectConfigItemsType,
    description: Option[String] = None,
    default: List[ProjectConfigValue] = List.empty,
    value: List[ProjectConfigValue] = List.empty,
    secret: Boolean = false
  ) extends ProjectConfigValue
      derives JsonFormat,
        YamlCodec

  given JsonFormat[ProjectConfigValue] = new JsonFormat[ProjectConfigValue]:
    override def write(obj: ProjectConfigValue): JsValue = obj match
      case StringValue(desc, default, value, secret) =>
        JsObject(
          "type" -> JsString("string"),
          "description" -> desc.toJson,
          "default" -> default.toJson,
          "value" -> value.toJson,
          "secret" -> secret.toJson
        )
      case BooleanValue(desc, default, value, secret) =>
        JsObject(
          "type" -> JsString("boolean"),
          "description" -> desc.toJson,
          "default" -> default.toJson,
          "value" -> value.toJson,
          "secret" -> secret.toJson
        )
      case IntegerValue(desc, default, value, secret) =>
        JsObject(
          "type" -> JsString("integer"),
          "description" -> desc.toJson,
          "default" -> default.toJson,
          "value" -> value.toJson,
          "secret" -> secret.toJson
        )
      case ArrayValue(items, desc, default, value, secret) =>
        JsObject(
          "type" -> JsString("array"),
          "items" -> items.toJson,
          "description" -> desc.toJson,
          "default" -> default.toJson,
          "value" -> value.toJson,
          "secret" -> secret.toJson
        )
    override def read(json: JsValue): ProjectConfigValue = json match
      case JsObject(fs) =>
        fs.get("type") match
          case Some(JsString("string"))  => summon[JsonReader[StringValue]].read(json)
          case Some(JsString("boolean")) => summon[JsonReader[BooleanValue]].read(json)
          case Some(JsString("integer")) => summon[JsonReader[IntegerValue]].read(json)
          case Some(JsString("array"))   => summon[JsonReader[ArrayValue]].read(json)
          case t                         => throw new Exception(s"ProjectConfigValue is invalid $t")
      case _ => throw new Exception(s"ProjectConfigValue is invalid, expected object")

  given YamlCodec[ProjectConfigValue] = new YamlCodec[ProjectConfigValue]:
    def asNode(obj: ProjectConfigValue): Node = obj match
      case s: StringValue  => summon[YamlEncoder[StringValue]].asNode(s)
      case b: BooleanValue => summon[YamlEncoder[BooleanValue]].asNode(b)
      case i: IntegerValue => summon[YamlEncoder[IntegerValue]].asNode(i)
      case a: ArrayValue   => summon[YamlEncoder[ArrayValue]].asNode(a)
    def construct(node: Node)(implicit settings: LoadSettings = LoadSettings.empty): Either[ConstructError, ProjectConfigValue] =
      node match
        case Node.MappingNode(m, _) =>
          m.get(Node.ScalarNode("type")) match
            case Some(Node.ScalarNode("string", _))  => summon[YamlDecoder[StringValue]].construct(node)
            case Some(Node.ScalarNode("boolean", _)) => summon[YamlDecoder[BooleanValue]].construct(node)
            case Some(Node.ScalarNode("integer", _)) => summon[YamlDecoder[IntegerValue]].construct(node)
            case Some(Node.ScalarNode("array", _))   => summon[YamlDecoder[ArrayValue]].construct(node)
            case Some(t)                             => Left(ConstructError(s"ProjectConfigValue is invalid: $t"))
            case None                                => Left(ConstructError(s"ProjectConfigValue is invalid, field 'type' is missing"))
        case _ => throw new Exception(s"ProjectConfigValue is invalid, expected object")
end ProjectConfigValue

/** ProjectConfigItemsType is a config item type included in the project manifest. */
sealed trait ProjectConfigItemsType
object ProjectConfigItemsType:
  object StringType extends ProjectConfigItemsType
  object BooleanType extends ProjectConfigItemsType
  object IntegerType extends ProjectConfigItemsType
  case class ArrayType(items: ProjectConfigItemsType) extends ProjectConfigItemsType

  given JsonFormat[ProjectConfigItemsType] = new JsonFormat[ProjectConfigItemsType]:
    override def write(obj: ProjectConfigItemsType): JsValue = obj match
      case StringType    => JsObject("type" -> JsString("string"))
      case BooleanType   => JsObject("type" -> JsString("boolean"))
      case IntegerType   => JsObject("type" -> JsString("integer"))
      case ArrayType(it) => JsObject("type" -> JsString("array"), "items" -> write(it))
    override def read(json: JsValue): ProjectConfigItemsType = json match
      case JsObject(fs) =>
        fs.get("type") match
          case Some(JsString("string"))  => StringType
          case Some(JsString("boolean")) => BooleanType
          case Some(JsString("integer")) => IntegerType
          case Some(JsString("array"))   => ArrayType(read(fs("items")))
          case t                         => throw new Exception(s"ProjectConfigItemsType is invalid $t")
      case _ => throw new Exception(s"ProjectConfigItemsType is invalid, field 'type' is missing")

  given YamlCodec[ProjectConfigItemsType] = new YamlCodec[ProjectConfigItemsType]:
    def asNode(obj: ProjectConfigItemsType): Node = obj match
      case StringType  => Node.MappingNode((Node.ScalarNode("type"), Node.ScalarNode("string")))
      case BooleanType => Node.MappingNode((Node.ScalarNode("type"), Node.ScalarNode("boolean")))
      case IntegerType => Node.MappingNode((Node.ScalarNode("type"), Node.ScalarNode("integer")))
      case ArrayType(it) =>
        Node.MappingNode(
          (Node.ScalarNode("type"), Node.ScalarNode("array")),
          (Node.ScalarNode("items"), asNode(it))
        )
    @tailrec
    def construct(node: Node)(implicit settings: LoadSettings = LoadSettings.empty): Either[ConstructError, ProjectConfigItemsType] =
      node match
        case n @ Node.MappingNode(m, _) =>
          m.get(Node.ScalarNode("type")) match
            case Some(Node.ScalarNode("string", _))  => Right(StringType)
            case Some(Node.ScalarNode("boolean", _)) => Right(BooleanType)
            case Some(Node.ScalarNode("integer", _)) => Right(IntegerType)
            case Some(Node.ScalarNode("array", _)) =>
              m.get(Node.ScalarNode("items")) match
                case Some(it) => construct(it)
                case None     => Left(ConstructError(s"ProjectConfigItemsType is invalid, field 'items' is missing"))
            case Some(t) => Left(ConstructError(s"ProjectConfigItemsType is invalid: $t"))
            case None    => Left(ConstructError(s"ProjectConfigItemsType is invalid, field 'type' is missing"))
        case _ => throw new Exception(s"ProjectConfigItemsType is invalid, field 'type' is missing")
end ProjectConfigItemsType

/** ProjectTemplate is a Pulumi project template manifest.
  *
  * @param description
  *   an optional description of the template
  * @param quickstart
  *   contains optional text to be displayed after template creation
  * @param config
  *   an optional template config
  * @param important
  *   indicates the template is important and should be listed by default
  */
case class ProjectTemplate(
  description: Option[String] = None,
  quickstart: Option[String] = None,
  config: Map[String, ProjectTemplateConfigValue] = Map.empty,
  important: Boolean = false
) derives JsonFormat,
      YamlCodec

/** ProjectTemplateConfigValue is a config value included in the project template manifest.
  *
  * @param description
  *   an optional description for the config value
  * @param default
  *   an optional default value for the config value
  * @param secret
  *   may be set to true to indicate that the config value should be encrypted
  */
case class ProjectTemplateConfigValue(
  description: Option[String] = None,
  default: Option[String] = None,
  secret: Boolean = false
) derives JsonFormat,
      YamlCodec

/** ProjectBackend is a configuration for backend used by project
  *
  * @param url
  *   is optional field to explicitly set backend url
  */
case class ProjectBackend(url: Option[String] = None) derives JsonFormat, YamlCodec

/** ProjectOptions
  *
  * @param refresh
  *   is the ability to always run a refresh as part of a pulumi update / preview / destroy
  */
case class ProjectOptions(refresh: Option[String] = None) derives JsonFormat, YamlCodec

/** PluginOptions
  *
  * @param name
  *   is the name of the plugin
  * @param path
  *   is the path of the plugin
  * @param version
  *   is the version of the plugin
  */
case class PluginOptions(name: String, path: String, version: Option[String] = None) derives JsonFormat, YamlCodec

/** Plugins
  *
  * @param providers
  *   is the list of provider plugins
  * @param languages
  *   is the list of language plugins
  * @param analyzers
  *   is the list of analyzer plugins
  */
case class Plugins(
  providers: List[PluginOptions] = List.empty,
  languages: List[PluginOptions] = List.empty,
  analyzers: List[PluginOptions] = List.empty
) derives JsonFormat,
      YamlCodec

/** ProjectStack holds stack specific information about a project.
  *
  * @param secretsProvider
  *   this stack's secrets provider
  * @param encryptedKey
  *   the KMS-encrypted ciphertext for the data key used for secrets encryption. Only used for cloud-based secrets providers
  * @param encryptionSalt
  *   this stack's base64 encoded encryption salt. Only used for passphrase-based secrets providers
  * @param config
  *   an optional config bag
  * @param environment
  *   an optional environment definition or list of environments
  */
case class ProjectStack(
  secretsProvider: Option[String] = None,
  encryptedKey: Option[String] = None,
  encryptionSalt: Option[String] = None,
  config: Map[String, Any] = Map.empty,
  environment: Option[Environment] = None
) derives JsonFormat,
      YamlCodec:
  def asYaml: String            = summon[YamlEncoder[ProjectStack]].asNode(this).asYaml
  def save(path: os.Path): Unit = os.write.over(path, asYaml, createFolders = true)
object ProjectStack:
  def load(path: os.Path): Either[Exception, ProjectStack] =
    path match
      case p if p.ext == "json" =>
        os.read(p).parseJson[ProjectStack].left.map(e => AutoError(s"Failed to read project stack from '$path'", e))
      case p if p.ext == "yaml" || p.ext == "yml" =>
        os.read(p).as[ProjectStack].left.map(e => AutoError(s"Failed to read project stack from '$path': ${e.msg}"))

/** Environment is an optional environment definition or list of environments.
  *
  * @param envs
  *   a list of environments
  */
case class Environment(envs: List[String] = List.empty) derives JsonFormat, YamlCodec

/** Settings defines workspace settings shared amongst many related projects.
  *
  * @param stack
  *   an optional default stack to use
  */
case class Settings(stack: Option[String] = None) derives JsonFormat, YamlCodec

/** A parameter to be applied to a stack remove operation
  */
enum RemoveOption:
  /** Force causes the remove operation to occur even if there are resources existing in the stack
    */
  case Force

/** UntypedDeployment contains an inner, untyped deployment structure.
  *
  * @param version
  *   indicates the schema of the encoded deployment
  * @param deployment
  *   the opaque Pulumi deployment. This is conceptually of type `Deployment`, but we use `JsonNode` to permit round-tripping of stack
  *   contents when an older client is talking to a newer server. If we un-marshaled the contents, and then re-marshaled them, we could end
  *   up losing important information.
  */
case class UntypedDeployment(
  version: Option[Int] = None,
  deployment: Option[JsValue] = None
) derives JsonFormat:
  def toJson: String = summon[JsonWriter[UntypedDeployment]].write(this).prettyPrint
object UntypedDeployment:
  def fromJson(json: String): Either[Exception, UntypedDeployment] = json.parseJson[UntypedDeployment]

/** PluginInfo provides basic information about a plugin. Each plugin gets installed into a system-wide location, by default
  * `~/.pulumi/plugins/[kind]-[name]-[version]/`. A plugin may contain multiple files, however the primary loadable executable must be named
  * `pulumi-[kind]-[name]`.
  *
  * @param name
  *   the simple name of the plugin
  * @param path
  *   the path that a plugin was loaded from (this will always be a directory)
  * @param kind
  *   the kind of the plugin (language, resource, etc)
  * @param version
  *   the plugin's semantic version, if present
  * @param size
  *   the size of the plugin, in bytes
  * @param installTime
  *   the time the plugin was installed
  * @param lastUsedTime
  *   the last time the plugin was used
  * @param schemaPath
  *   if set, used as the path for loading and caching the schema
  * @param schemaTime
  *   if set and newer than the file at SchemaPath, used to invalidate a cached schema
  */
// TODO: can be moved to model module and used in core and codegen
case class PluginInfo(
  name: String,
  path: String,
  kind: PluginKind,
  version: Option[String] = None,
  size: Long,
  installTime: java.time.Instant,
  lastUsedTime: java.time.Instant,
  schemaPath: Option[String] = None,
  schemaTime: Option[java.time.Instant] = None
) derives JsonFormat:
  override def toString: String = name + version.map("-" + _).getOrElse("")
object PluginInfo:
  def fromJsonList(json: String): Either[Exception, List[PluginInfo]] =
    json.parseJson[List[PluginInfo]]

/** PluginKind represents a kind of a plugin that may be dynamically loaded and used by Pulumi. */
enum PluginKind(val value: String):
  /** Analyzer is a plugin that can be used as a resource analyzer.
    */
  case Analyzer extends PluginKind("analyzer")

  /** Language is a plugin that can be used as a language host.
    */
  case Language extends PluginKind("language")

  /** Resource is a plugin that can be used as a resource provider for custom CRUD operations.
    */
  case Resource extends PluginKind("resource")

  /** Converter is a plugin that can be used to convert from other ecosystems to Pulumi.
    */
  case Converter extends PluginKind("converter")
object PluginKind:
  given JsonFormat[PluginKind] = new JsonFormat[PluginKind]:
    override def write(obj: PluginKind): JsValue = JsString(obj.value)
    override def read(json: JsValue): PluginKind = json match
      case JsString(s) => PluginKind.from(s)
      case _           => throw new RuntimeException("PluginKind must be a string")

  def from(value: String): PluginKind = value match
    case "analyzer"  => Analyzer
    case "language"  => Language
    case "resource"  => Resource
    case "converter" => Converter
    case _           => throw new RuntimeException(s"Unknown plugin kind: $value")

type RunFunc = besom.Context => besom.Output[besom.internal.Exports]

/** OutputValue models a Pulumi Stack output, providing the plaintext value and a boolean indicating secretness.
  *
  * @param value
  *   the plaintext value of the output
  * @param secret
  *   a boolean indicating if the output is a secret
  */
case class OutputValue(value: Any, secret: Boolean = false)

/** OutputMap is the output result of running a Pulumi program. It is represented as a map from string to OutputValue.
  */
type OutputMap = Map[String, OutputValue]
object OutputMap:
  // represents the CLI response for an output marked as "secret"
  private val SecretSentinel = "[secret]"

  private[auto] def fromJson(
    masked: String,
    plaintext: String
  ): Either[Exception, OutputMap] =
    for
      m <- masked.parseJson[Map[String, String]].left.map(e => AutoError(s"Failed to parse masked output map", e))
      p <- plaintext.parseJson[Map[String, String]].left.map(e => AutoError(s"Failed to parse plaintext output map", e))
    yield for (k, v) <- p yield k -> OutputValue(v, m.get(k).contains(SecretSentinel))
end OutputMap
