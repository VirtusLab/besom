package yaga.extensions.aws.lambda.internal

import scala.util.Try

import yaga.json.JsonReader

object EnvReader:
  inline def read[A : JsonReader](env: Map[String, String]): Either[Throwable, A] =
    Try {
        val jsonStr = env(EnvWriter.defaultEnvVariableName)
        summon[JsonReader[A]].read(jsonStr)
      }.toEither

  def configJson(env: Map[String, String]): String =
    env(EnvWriter.defaultEnvVariableName)