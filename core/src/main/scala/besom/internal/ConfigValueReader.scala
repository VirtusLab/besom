package besom.internal

import scala.util.Try

trait ConfigValueReader[A]:
  def read(key: String, rawValue: String): Either[Exception, A]

object ConfigValueReader:
  given stringReader: ConfigValueReader[String] with
    override def read(key: String, rawValue: String) =
      Right(rawValue)
  given doubleReader: ConfigValueReader[Double] with
    override def read(key: String, rawValue: String) =
      Try(rawValue.toDouble).toEither.left.map(_ =>
        RuntimeException(s"Config value $key is not a valid double: $rawValue")
      )
  given intReader: ConfigValueReader[Int] with
    override def read(key: String, rawValue: String) =
      Try(rawValue.toInt).toEither.left.map(_ =>
        RuntimeException(s"Config value $key is not a valid int: $rawValue")
      )
  given booleanReader: ConfigValueReader[Boolean] with
    override def read(key: String, rawValue: String) =
      Try(rawValue.toBoolean).toEither.left.map(_ =>
        RuntimeException(s"Config value $key is not a valid boolean: $rawValue")
      )
