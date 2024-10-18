package yaga.extensions.aws.lambda

import scala.util.Try
import besom.json.JsonWriter

trait OutputStreamWriter[A]:
  def write(stream: java.io.OutputStream, a: A): Either[Throwable, Unit]

object OutputStreamWriter:
  given fromBesomJsonWriter[A](using jsonWriter: JsonWriter[A]): OutputStreamWriter[A] with
    override def write(stream: java.io.OutputStream, a: A): Either[Throwable, Unit] =
      Try {
        val str = jsonWriter.write(a).toString
        stream.write(str.getBytes("UTF-8"))
      }.toEither