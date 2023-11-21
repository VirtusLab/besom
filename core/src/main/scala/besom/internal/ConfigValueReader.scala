package besom.internal

import spray.json.*

import scala.util.Try

trait ConfigValueReader[A]:
  def read(key: String, rawValue: String): Either[Exception, A]

object ConfigValueReader:
  given stringReader: ConfigValueReader[String] with
    override def read(key: String, rawValue: String): Either[Exception, String] =
      Right(rawValue)
  given doubleReader: ConfigValueReader[Double] with
    override def read(key: String, rawValue: String): Either[Exception, Double] =
      Try(rawValue.toDouble).toEither.left.map(e => ConfigError(s"Config value '$key' is not a valid double: $rawValue", e))
  given intReader: ConfigValueReader[Int] with
    override def read(key: String, rawValue: String): Either[Exception, Int] =
      Try(rawValue.toInt).toEither.left.map(e => ConfigError(s"Config value '$key' is not a valid int: $rawValue", e))
  given booleanReader: ConfigValueReader[Boolean] with
    override def read(key: String, rawValue: String): Either[Exception, Boolean] =
      Try(rawValue.toBoolean).toEither.left.map(e => ConfigError(s"Config value '$key' is not a valid boolean: $rawValue", e))
  given jsonReader: ConfigValueReader[JsValue] with
    override def read(key: String, rawValue: String): Either[Exception, JsValue] =
      Try(rawValue.parseJson).toEither.left.map(e => ConfigError(s"Config value '$key' is not a valid JSON: $rawValue", e))
  given objectReader[A: JsonReader]: ConfigValueReader[A] with
    override def read(key: String, rawValue: String): Either[Exception, A] =
      Try(rawValue.parseJson.convertTo[A]).toEither.left.map(e =>
        ConfigError(s"Failed to deserialize JSON value for key '$key': ${e.getMessage}", e)
      )
