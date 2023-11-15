package besom.internal

@SerialVersionUID(1L)
sealed abstract class BaseCoreError(message: Option[String], cause: Option[Throwable])
    extends Exception(message.orElse(cause.map(_.toString)).orNull, cause.orNull)
    with Product
    with Serializable

@SerialVersionUID(1L)
case class CoreError(message: Option[String], cause: Option[Throwable]) extends BaseCoreError(message, cause)
object CoreError {
  def apply(message: String)                   = new CoreError(Some(message), None)
  def apply(message: String, cause: Throwable) = new CoreError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new CoreError(None, Some(cause))
}

@SerialVersionUID(1L)
case class ConfigError(message: Option[String], cause: Option[Throwable]) extends BaseCoreError(message, cause)
object ConfigError {
  def apply(message: String)                   = new ConfigError(Some(message), None)
  def apply(message: String, cause: Throwable) = new ConfigError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new ConfigError(None, Some(cause))
}
