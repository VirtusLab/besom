package besom.codegen

@SerialVersionUID(1L)
sealed abstract class CodegenError(message: Option[String], cause: Option[Throwable])
    extends Exception(message.orElse(cause.map(_.toString)).orNull, cause.orNull)
    with Product
    with Serializable

@SerialVersionUID(1L)
case class GeneralCodegenError(message: Option[String], cause: Option[Throwable])
    extends CodegenError(message, cause)
object GeneralCodegenError {
  def apply(message: String)                   = new GeneralCodegenError(Some(message), None)
  def apply(message: String, cause: Throwable) = new GeneralCodegenError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new GeneralCodegenError(None, Some(cause))
}

@SerialVersionUID(1L)
case class PulumiTypeCoordinatesError private (message: Option[String], cause: Option[Throwable])
    extends CodegenError(message, cause)
object PulumiTypeCoordinatesError {
  def apply(message: String)                   = new PulumiTypeCoordinatesError(Some(message), None)
  def apply(message: String, cause: Throwable) = new PulumiTypeCoordinatesError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new PulumiTypeCoordinatesError(None, Some(cause))
}

@SerialVersionUID(1L)
case class ClassCoordinatesError(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object ClassCoordinatesError {
  def apply(message: String)                   = new ClassCoordinatesError(Some(message), None)
  def apply(message: String, cause: Throwable) = new ClassCoordinatesError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new ClassCoordinatesError(None, Some(cause))
}

@SerialVersionUID(1L)
case class TypeMapperError(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object TypeMapperError {
  def apply(message: String)                   = new TypeMapperError(Some(message), None)
  def apply(message: String, cause: Throwable) = new TypeMapperError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new TypeMapperError(None, Some(cause))
}

@SerialVersionUID(1L)
case class SchemaProviderError(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object SchemaProviderError {
  def apply(message: String)                   = new SchemaProviderError(Some(message), None)
  def apply(message: String, cause: Throwable) = new SchemaProviderError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new SchemaProviderError(None, Some(cause))
}