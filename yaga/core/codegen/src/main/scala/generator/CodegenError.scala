// Copied (and adapted) from codegen/**/CodegenError.scala

package yaga.codegen.core.generator

@SerialVersionUID(1L)
sealed abstract class CodegenError(message: Option[String], cause: Option[Throwable])
    extends Exception(message.orElse(cause.map(_.toString)).orNull, cause.orNull)
    with Product
    with Serializable

@SerialVersionUID(1L)
case class GeneralCodegenException(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object GeneralCodegenException {
  def apply(message: String)                   = new GeneralCodegenException(Some(message), None)
  def apply(message: String, cause: Throwable) = new GeneralCodegenException(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new GeneralCodegenException(None, Some(cause))
}
