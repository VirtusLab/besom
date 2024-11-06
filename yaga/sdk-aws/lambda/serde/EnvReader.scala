package yaga.extensions.aws.lambda.internal

import scala.util.Try

import besom.json.{JsonParser, JsonReader}

trait EnvReader[A]:
  def read(env: Map[String, String]): Either[Throwable, A]

object EnvReader:
  given fromBesomJsonReader[A](using jsonReader: JsonReader[A]): EnvReader[A] with
    override def read(env: Map[String, String]): Either[Throwable, A] =
      Try {
        val str = env(EnvWriter.defaultEnvVariableName)
        val json = JsonParser(str)
        jsonReader.read(json)
      }.toEither
