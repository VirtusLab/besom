package besom.internal

import besom.util.NonEmptyString

import scala.util.Try

/** Config is a bag of related configuration state. Each bag contains any number of configuration variables, indexed by
  * simple keys, and each has a name that uniquely identifies it; two bags with different names do not share values for
  * variables that otherwise share the same key. For example, a bag whose name is `pulumi:foo`, with keys `a`, `b`, and
  * `c`, is entirely separate from a bag whose name is `pulumi:bar` with the same simple key names. Each key has a fully
  * qualified names, such as `pulumi:foo:a`, ..., and `pulumi:bar:a`, respectively.
  */
//noinspection ScalaWeakerAccess
class Config private (
  val namespace: NonEmptyString,
  private[internal] val isProjectName: Boolean,
  private[internal] val configMap: Map[NonEmptyString, String],
  private[internal] val configSecretKeys: Set[NonEmptyString]
):
  private def fullKey(key: NonEmptyString): NonEmptyString = namespace +++ ":" +++ key

  private def tryGet(key: NonEmptyString): Option[String] = configMap.get(fullKey(key)) match
    case sme: Some[?]          => sme
    case None if isProjectName => configMap.get(key)
    case None                  => None

  /** This method is unsafe because it will return a secret value as a plain string. This is useful for provider SDKs.
    * @param key
    *   the requested configuration key
    * @return
    *   the configuration value of the requested type
    */
  // noinspection ScalaUnusedSymbol
  private[besom] def unsafeGet(key: NonEmptyString) = tryGet(key)

  /** This method differs in behavior from other Pulumi SDKs. In other SDKs, if you try to get a config key that is a
    * secret, you will obtain it (due to https://github.com/pulumi/pulumi/issues/7127 you won't even get a warning). We
    * choose to do the right thing here and not return the secret value as an unmarked plain string. For provider SDKs
    * we have the unsafeGet method should it be absolutely necessary in practice. We also return all configs as Outputs
    * so that we can handle failure in pure, functional way.
    * @param key
    *   the requested configuration key
    * @return
    *   the configuration value of the requested type
    */
  private def getRawValue(key: NonEmptyString)(using ctx: Context): Output[Option[String]] =
    if configSecretKeys.contains(key)
    then Output.secret(tryGet(key))
    else Output(tryGet(key))

  private def readConfigValue[A: ConfigValueReader](
    key: NonEmptyString,
    rawValue: Output[Option[String]]
  )(using Context): Output[Option[A]] =
    rawValue.flatMap { valueOpt =>
      Output.ofData {
        Result
          .evalEither(
            valueOpt.map(value => summon[ConfigValueReader[A]].read(key = key, rawValue = value)) match
              case None               => Right(None)
              case Some(Right(value)) => Right(Some(value))
              case Some(Left(error))  => Left(error)
          )
          .map(OutputData(_))
      }
    }

  /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
    * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @tparam A
    *   the type of the configuration value
    * @return
    *   an optional configuration value of the requested type
    */
  def get[A: ConfigValueReader](key: NonEmptyString)(using Context): Output[Option[A]] =
    readConfigValue(key, getRawValue(key))

  /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
    * secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @tparam A
    *   the type of the configuration value
    * @return
    *   the configuration value of the requested type or [[ConfigError]]
    */
  def require[A: ConfigValueReader](key: NonEmptyString)(using Context): Output[A] = {
    def secretOption = if configSecretKeys.contains(key) then "[--secret]" else ""
    get[A](key).flatMap { valueOpt =>
      valueOpt match
        case Some(value) => Output(value)
        case None =>
          Output.fail {
            ConfigError(
              s"""|Missing required configuration variable '${key}'
                  |Please set a value using the command `pulumi config set ${key} <value> $secretOption`""".stripMargin
            )
          }
    }
  }

  /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
    * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   an optional configuration [[String]] value
    */
  def getString(key: NonEmptyString)(using Context): Output[Option[String]] = get[String](key)

  /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
    * secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   the configuration [[String]] value or [[ConfigError]]
    */
  def requireString(key: NonEmptyString)(using Context): Output[String] = require[String](key)

  /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
    * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   an optional configuration [[Double]] value
    */
  def getDouble(key: NonEmptyString)(using Context): Output[Option[Double]] = get[Double](key)

  /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
    * secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   the configuration [[Double]] value or [[ConfigError]]
    */
  def requireDouble(key: NonEmptyString)(using Context): Output[Double] = require[Double](key)

  /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
    * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   an optional configuration [[Int]] value
    */
  def getInt(key: NonEmptyString)(using Context): Output[Option[Int]] = get[Int](key)

  /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
    * secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   the configuration [[Int]] value or [[ConfigError]]
    */
  def requireInt(key: NonEmptyString)(using Context): Output[Int] = require[Int](key)

  /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
    * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   an optional configuration [[Boolean]] value
    */
  def getBoolean(key: NonEmptyString)(using Context): Output[Option[Boolean]] = get[Boolean](key)

  /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
    * secret, it will be marked internally as such and redacted in console outputs.
    *
    * @param key
    *   the requested configuration key
    * @param Context
    *   the Besom context
    * @return
    *   the configuration [[Boolean]] value or [[ConfigError]]
    */
  def requireBoolean(key: NonEmptyString)(using Context): Output[Boolean] = require[Boolean](key)

//noinspection ScalaUnusedSymbol
object Config:
  /** CleanKey takes a configuration key, and if it is of the form `"(string):config:(string)"` removes the `":config:"`
    * portion. Previously, our keys always had the string `":config:"` in them, and we'd like to remove it. However, the
    * language host needs to continue to set it so we can be compatible with older versions of our packages. Once we
    * stop supporting older packages, we can change the language host to not add this `:config:` thing and remove this
    * function.
    */
  private def cleanKey(key: String): String =
    val prefix = "config:"
    val idx    = key.indexOf(":")

    if idx > 0 && key.substring(idx + 1).startsWith(prefix) then
      key.substring(0, idx) + ":" + key.substring(idx + 1 + prefix.length)
    else key

  private[internal] def apply(
    namespace: NonEmptyString,
    isProjectName: Boolean,
    configMap: Map[NonEmptyString, String],
    configSecretKeys: Set[NonEmptyString]
  ): Result[Config] =
    val cleanedConfigMap =
      Result.evalTry {
        Try {
          configMap.map { case (k, v) =>
            val cleanedKey = NonEmptyString(cleanKey(k)) match {
              case Some(value) => value
              case None        => throw CoreError(s"The config key '$k' was empty after cleaning")
            }
            (cleanedKey, v)
          }
        }
      }

    cleanedConfigMap.map(cm =>
      new Config(
        namespace = namespace,
        isProjectName = isProjectName,
        configMap = cm,
        configSecretKeys = configSecretKeys.map(identity)
      )
    )

  private def apply(namespace: NonEmptyString, isProjectName: Boolean): Result[Config] =
    for
      configMap        <- Result.evalTry(Env.getConfigMap(Env.EnvConfig))
      configSecretKeys <- Result.evalTry(Env.getConfigSecretKeys(Env.EnvConfigSecretKeys))
      config           <- Config(namespace, isProjectName, configMap, configSecretKeys)
    yield config

  private[internal] def forNamespace(
    namespace: NonEmptyString,
    configMap: Map[NonEmptyString, String],
    configSecretKeys: Set[NonEmptyString]
  ): Result[Config] =
    Config(namespace, isProjectName = false, configMap = configMap, configSecretKeys = configSecretKeys)

  private[internal] def forProject(projectName: NonEmptyString): Result[Config] =
    val cleanedProjectName =
      if projectName.endsWith(":config") then
        NonEmptyString(projectName.stripSuffix(":config")) match
          case Some(value) => Result(value)
          case None        => Result.fail(ConfigError(s"Project name '$projectName' was empty after cleaning"))
      else Result.pure(projectName)

    cleanedProjectName.flatMap(name => Config(name, isProjectName = true))

  extension (output: Output[Config])
    /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
      * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @tparam A
      *   the type of the configuration value
      * @return
      *   an optional configuration value of the requested type
      */
    def get[A: ConfigValueReader](key: NonEmptyString)(using Context): Output[Option[A]] =
      output.flatMap(_.get[A](key))

    /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
      * secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @tparam A
      *   the type of the configuration value
      * @return
      *   the configuration value of the requested type or [[ConfigError]]
      */
    def require[A: ConfigValueReader](key: NonEmptyString)(using Context): Output[A] =
      output.flatMap(_.require[A](key))

    /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
      * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   an optional configuration [[String]] value
      */
    def getString(key: NonEmptyString)(using Context): Output[Option[String]] = output.flatMap(_.getString(key))

    /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
      * secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   the configuration [[String]] value or [[ConfigError]]
      */
    def requireString(key: NonEmptyString)(using Context): Output[String] = output.flatMap(_.requireString(key))

    /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
      * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   an optional configuration [[Double]] value
      */
    def getDouble(key: NonEmptyString)(using Context): Output[Option[Double]] = output.flatMap(_.getDouble(key))

    /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
      * secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   the configuration [[Double]] value or [[ConfigError]]
      */
    def requireDouble(key: NonEmptyString)(using Context): Output[Double] = output.flatMap(_.requireDouble(key))

    /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
      * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   an optional configuration [[Int]] value
      */
    def getInt(key: NonEmptyString)(using Context): Output[Option[Int]] = output.flatMap(_.getInt(key))

    /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
      * secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   the configuration [[Int]] value or [[ConfigError]]
      */
    def requireInt(key: NonEmptyString)(using Context): Output[Int] = output.flatMap(_.requireInt(key))

    /** Loads an optional configuration or secret value by its key, or returns [[None]] if it doesn't exist. If the
      * configuration value is a secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   an optional configuration [[Boolean]] value
      */
    def getBoolean(key: NonEmptyString)(using Context): Output[Option[Boolean]] = output.flatMap(_.getBoolean(key))

    /** Loads a configuration or secret value by its key, or throws if it doesn't exist. If the configuration value is a
      * secret, it will be marked internally as such and redacted in console outputs.
      *
      * @param key
      *   the requested configuration key
      * @param Context
      *   the Besom context
      * @return
      *   the configuration [[Boolean]] value or [[ConfigError]]
      */
    def requireBoolean(key: NonEmptyString)(using Context): Output[Boolean] = output.flatMap(_.requireBoolean(key))

trait ConfigFactory:
  /** Creates a new Config with the given namespace.
    * @param namespace
    *   the configuration bag’s logical name that uniquely identifies it.
    * @param Context
    *   the Besom context.
    * @return
    *   a new Config with the given namespace.
    */
  def apply(namespace: NonEmptyString)(using Context): Output[Config] =
    val projectConfig = summon[Context].config
    Output(Config.forNamespace(namespace, projectConfig.configMap, projectConfig.configSecretKeys))
