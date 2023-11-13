package besom.internal

import besom.internal.logging.BesomLogger
import besom.util.NonEmptyString
import scala.util.Try

class Config private (
  val namespace: NonEmptyString,
  private[internal] val isProjectName: Boolean,
  private[internal] val configMap: Map[NonEmptyString, String],
  private[internal] val configSecretKeys: Set[NonEmptyString]
):
  private def fullKey(key: NonEmptyString): NonEmptyString = namespace +++ ":" +++ key

  private def tryGet(key: NonEmptyString): Option[String] = configMap.get(fullKey(key)) match
    case sme: Some[?] => sme
    case None if isProjectName => configMap.get(key)
    case None => None

  private[besom] def unsafeGet(key: NonEmptyString): Option[String] = tryGet(key)

  private def getRawValue(key: NonEmptyString)(using ctx: Context): Output[Option[String]] =
    if configSecretKeys.contains(key)
      then Output.secret(tryGet(key))
      else Output(tryGet(key))

  private def readConfigValue[A : ConfigValueReader](key: NonEmptyString, rawValue: Output[Option[String]])(using Context): Output[Option[A]] =
    rawValue.flatMap { valueOpt =>
      Output.ofData {
        Result
          .evalEither(
            valueOpt.map(value => summon[ConfigValueReader[A]].read(key = key, rawValue = value)) match
              case None => Right(None)
              case Some(Right(value)) => Right(Some(value))
              case Some(Left(error)) => Left(error)
          )
          .map(OutputData(_))
      }
    }

  def get[A : ConfigValueReader](key: NonEmptyString)(using Context): Output[Option[A]] =
    readConfigValue(key, getRawValue(key))

  def require[A : ConfigValueReader](key: NonEmptyString)(using Context): Output[A] =
    get[A](key).flatMap { valueOpt =>
      valueOpt match
        case Some(value) => Output(value)
        case None => Output.fail {
          val message =
            s"""|Missing required configuration variable '${key}'
                |Please set a value using the command `pulumi config set ${key} <value> [--secret]`""".stripMargin
          new Exception(message)
        }
    }

  def getString(key: NonEmptyString)(using Context): Output[Option[String]] = get[String](key)
  def requireString(key: NonEmptyString)(using Context): Output[String] = require[String](key)
  
  def getDouble(key: NonEmptyString)(using Context): Output[Option[Double]] = get[Double](key)
  def requireDouble(key: NonEmptyString)(using Context): Output[Double] = require[Double](key)
  
  def getInt(key: NonEmptyString)(using Context): Output[Option[Int]] = get[Int](key)
  def requireInt(key: NonEmptyString)(using Context): Output[Int] = require[Int](key)
  
  def getBoolean(key: NonEmptyString)(using Context): Output[Option[Boolean]] = get[Boolean](key)
  def requireBoolean(key: NonEmptyString)(using Context): Output[Boolean] = require[Boolean](key)

object Config:
  /** CleanKey takes a configuration key, and if it is of the form "(string):config:(string)" removes the ":config:"
    * portion. Previously, our keys always had the string ":config:" in them, and we'd like to remove it. However, the
    * language host needs to continue to set it so we can be compatible with older versions of our packages. Once we
    * stop supporting older packages, we can change the language host to not add this :config: thing and remove this
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
  ): Result[Config] = Result.defer {
    new Config(
      namespace = namespace,
      isProjectName = isProjectName,
      configMap = configMap.map { case (k, v) =>
        val cleanedKey = NonEmptyString(cleanKey(k)).getOrElse {
          throw new Exception(s"The config key ${k} was empty after cleaning")
        }
        (cleanedKey, v)
      },
      configSecretKeys = configSecretKeys.map(identity)
    )
  }

  import Env.*
  
  private def apply(namespace: NonEmptyString, isProjectName: Boolean): Result[Config] =
    for
      configMap        <- Result.evalTry(Env.getConfigMap(EnvConfig))
      configSecretKeys <- Result.evalTry(Env.getConfigSecretKeys(EnvConfigSecretKeys))
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
        NonEmptyString(projectName.replaceAll(":config$", "")).getOrElse {
          throw new RuntimeException(s"Invalid project name: $projectName - project name cannot be empty!")
        }
      else projectName
    Config(projectName, isProjectName = true)

  extension (output: Output[Config])
    def get[A : ConfigValueReader](key: NonEmptyString)(using Context): Output[Option[A]] =
      output.flatMap(_.get[A](key))
    def require[A : ConfigValueReader](key: NonEmptyString)(using Context): Output[A] =
      output.flatMap(_.require[A](key))

    def getString(key: NonEmptyString)(using Context): Output[Option[String]] = output.flatMap(_.getString(key))
    def requireString(key: NonEmptyString)(using Context): Output[String] = output.flatMap(_.requireString(key))

    def getDouble(key: NonEmptyString)(using Context): Output[Option[Double]] = output.flatMap(_.getDouble(key))
    def requireDouble(key: NonEmptyString)(using Context): Output[Double] = output.flatMap(_.requireDouble(key))
    
    def getInt(key: NonEmptyString)(using Context): Output[Option[Int]] = output.flatMap(_.getInt(key))
    def requireInt(key: NonEmptyString)(using Context): Output[Int] = output.flatMap(_.requireInt(key))

    def getBoolean(key: NonEmptyString)(using Context): Output[Option[Boolean]] = output.flatMap(_.getBoolean(key))
    def requireBoolean(key: NonEmptyString)(using Context): Output[Boolean] = output.flatMap(_.requireBoolean(key))

trait ConfigFactory:
  def apply(namespace: NonEmptyString)(using Context): Output[Config] =
    val projectConfig = summon[Context].config
    Output(Config.forNamespace(namespace, projectConfig.configMap, projectConfig.configSecretKeys))
