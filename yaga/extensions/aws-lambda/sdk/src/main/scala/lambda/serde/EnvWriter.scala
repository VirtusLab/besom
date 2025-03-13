package yaga.extensions.aws.lambda.internal

import scala.util.Try

import yaga.json.JsonWriter

object EnvWriter:
  val defaultEnvVariableName = "YAGA_AWS_LAMBDA_CONFIG"

  def write[A : JsonWriter](value: A): Either[Throwable, Map[String, String]] =
    Try {
        val jsonStr = summon[JsonWriter[A]].write(value)
        Map(
          defaultEnvVariableName -> jsonStr
        )
      }.toEither
