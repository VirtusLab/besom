package yaga.extensions.aws.lambda

import scala.util.Try

import besom.json.JsonWriter

trait EnvWriter[A]:
  def write(value: A): Either[Throwable, Map[String, String]]

object EnvWriter:
  val defaultEnvVariableName = "YAGA_AWS_LAMBDA_CONFIG"
  
  given fromBesomJsonWriter[A](using jsonWriter: JsonWriter[A]): EnvWriter[A] with
    override def write(value: A): Either[Throwable, Map[String, String]] =
      Try {
        val str = jsonWriter.write(value).toString
        Map(
          defaultEnvVariableName -> str
        )
      }.toEither
