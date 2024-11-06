package yaga.extensions.aws.lambda.internal

import scala.util.Try
import besom.json.{JsonParser, JsonReader}

trait InputStreamReader[A]:
  def read(stream: java.io.InputStream): Either[Throwable, A]

object InputStreamReader:
  given fromBesomJsonFormat[A](using jsonReader: JsonReader[A]): InputStreamReader[A] with 
    override def read(stream: java.io.InputStream): Either[Throwable, A] =
      Try {
        val str = scala.io.Source.fromInputStream(stream, "UTF-8").mkString
        val json = JsonParser(str)
        jsonReader.read(json)
      }.toEither
