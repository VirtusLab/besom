package besom.internal

import besom.internal.logging.BesomLogger
import besom.util.NonEmptyString
import scala.util.Try

class Config private (
  val projectName: NonEmptyString,
  private val configMap: Map[String, String],
  private val configSecretKeys: Set[String]
):
  private def fullKey(key: String): String = projectName + ":" + key

  private[besom] def unsafeGet(key: String): Option[String] = configMap.get(fullKey(key))

  /** This method differs in behavior from other Pulumi SDKs. In other SDKs, if you try to get a config key that is a
    * secret, you will obtain it (due to https://github.com/pulumi/pulumi/issues/7127 you won't even get a warning). We
    * choose to do the right thing here and not return the secret value as an unmarked plain string. For provider sdks
    * we have the unsafeGet method should it be absolutely necessary in practice. We also return all configs as Outputs
    * so that we can handle failure in pure, functional way.
    */
  def get(key: String)(using ctx: Context): Output[String] =
    if configSecretKeys.contains(key) then
      val result: Result[OutputData[String]] =
        ctx.logger.warn(s"Config key $key is a secret, refusing to fetch it as a plain string!") *>
          Result.pure(OutputData.empty())

      Output.ofData(result)
    else Output.ofData(OutputData(Set.empty, configMap.get(fullKey(key)), isSecret = false))

  def getSecret(key: String)(using ctx: Context): Output[String] =
    if configSecretKeys.contains(key) then
      Output.ofData(OutputData(Set.empty, configMap.get(fullKey(key)), isSecret = true))
    else
      Output.ofData(
        ctx.logger.warn(s"Config key $key is not a secret") *> Result.pure(OutputData.empty(isSecret = true))
      )

  def getDouble(key: String)(using Context): Output[Double] =
    get(key).flatMap { value =>
      Output.ofData {
        Result
          .evalEither(
            Try(value.toDouble).toEither.left.map(_ =>
              RuntimeException(s"Config value $key is not a valid double: $value")
            )
          )
          .map(OutputData(_))
      }
    }

  def getInt(key: String)(using Context): Output[Int] =
    get(key).flatMap { value =>
      Output.ofData {
        Result
          .evalEither(
            Try(value.toInt).toEither.left.map(_ => RuntimeException(s"Config value $key is not a valid int: $value"))
          )
          .map(OutputData(_))
      }
    }

  def getBoolean(key: String)(using Context): Output[Boolean] =
    get(key).flatMap { value =>
      Output.ofData {
        Result
          .evalEither(
            Try(value.toBoolean).toEither.left.map(_ =>
              RuntimeException(s"Config value $key is not a valid boolean: $value")
            )
          )
          .map(OutputData(_))
      }
    }

  def getSecretDouble(key: String)(using Context): Output[Double] =
    getSecret(key).flatMap { value =>
      Output.ofData {
        Result
          .evalEither(
            Try(value.toDouble).toEither.left.map(_ =>
              RuntimeException(s"Secret config value $key is not a valid double: $value")
            )
          )
          .map(OutputData(_))
      }
    }

  def getSecretInt(key: String)(using Context): Output[Int] =
    getSecret(key).flatMap { value =>
      Output.ofData {
        Result
          .evalEither(
            Try(value.toInt).toEither.left.map(_ =>
              RuntimeException(s"Secret config value $key is not a valid int: $value")
            )
          )
          .map(OutputData(_))
      }
    }

  def getSecretBoolean(key: String)(using Context): Output[Boolean] =
    getSecret(key).flatMap { value =>
      Output.ofData {
        Result
          .evalEither(
            Try(value.toBoolean).toEither.left.map(_ =>
              RuntimeException(s"Secret config value $key is not a valid boolean: $value")
            )
          )
          .map(OutputData(_))
      }
    }

object Config:
  /** CleanKey takes a configuration key, and if it is of the form "(string):config:(string)" removes the ":config:"
    * portion. Previously, our keys always had the string ":config:" in them, and we'd like to remove it. However, the
    * language host needs to continue to set it so we can be compatible with older versions of our packages. Once we
    * stop supporting older packages, we can change the language host to not add this :config: thing and remove this
    * function.
    */
  def cleanKey(key: String): String =
    val prefix = "config:"
    val idx    = key.indexOf(":")

    if idx > 0 && key.substring(idx + 1).startsWith(prefix) then
      key.substring(0, idx) + ":" + key.substring(idx + 1 + prefix.length)
    else key

  def apply(
    projectName: NonEmptyString,
    map: Map[NonEmptyString, String],
    configSecretKeys: Set[NonEmptyString]
  ): Result[Config] = Result.defer {
    val cleanedProjectName =
      if projectName.endsWith(":config") then
        NonEmptyString(projectName.replaceAll(":config$", "")).getOrElse {
          throw new RuntimeException(s"Invalid project name: $projectName - project name cannot be empty!")
        }
      else projectName

    new Config(
      projectName = cleanedProjectName,
      configMap = map.map { case (k, v) => (cleanKey(k), v) },
      configSecretKeys = configSecretKeys.map(identity)
    )
  }

  import Env.*

  def apply(projectName: NonEmptyString): Result[Config] =
    for
      configMap        <- Result.evalTry(Env.getConfigMap(EnvConfig))
      configSecretKeys <- Result.evalTry(Env.getConfigSecretKeys(EnvConfigSecretKeys))
      config           <- Config(projectName, configMap, configSecretKeys)
    yield config

trait ConfigFactory:
  def apply(projectName: NonEmptyString)(using Context): Output[Config] = Output(Config(projectName))
