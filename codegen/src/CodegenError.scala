package besom.codegen

@SerialVersionUID(1L)
sealed abstract class CodegenError(message: Option[String], cause: Option[Throwable])
    extends Exception(message.orElse(cause.map(_.toString)).orNull, cause.orNull)
    with Product
    with Serializable

@SerialVersionUID(1L)
case class GeneralCodegenError(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object GeneralCodegenError {
  def apply(message: String)                   = new GeneralCodegenError(Some(message), None)
  def apply(message: String, cause: Throwable) = new GeneralCodegenError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new GeneralCodegenError(None, Some(cause))
}

@SerialVersionUID(1L)
case class PulumiDefinitionCoordinatesError private (message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object PulumiDefinitionCoordinatesError {
  def apply(message: String)                   = new PulumiDefinitionCoordinatesError(Some(message), None)
  def apply(message: String, cause: Throwable) = new PulumiDefinitionCoordinatesError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new PulumiDefinitionCoordinatesError(None, Some(cause))
}

@SerialVersionUID(1L)
case class ScalaDefinitionCoordinatesError(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object ScalaDefinitionCoordinatesError {
  def apply(message: String)                   = new ScalaDefinitionCoordinatesError(Some(message), None)
  def apply(message: String, cause: Throwable) = new ScalaDefinitionCoordinatesError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new ScalaDefinitionCoordinatesError(None, Some(cause))
}

@SerialVersionUID(1L)
case class TypeError(message: Option[String], cause: Option[Throwable]) extends CodegenError(message, cause)
object TypeError {
  def apply(message: String)                   = new TypeError(Some(message), None)
  def apply(message: String, cause: Throwable) = new TypeError(Some(message), Some(cause))
  def apply(cause: Throwable)                  = new TypeError(None, Some(cause))
}

@SerialVersionUID(1L)
case class AggregateCodegenError(message: Option[String], cause: Option[Throwable], errors: Seq[Throwable])
    extends CodegenError(message, cause)
    with Iterable[Throwable] {
  def iterator: Iterator[Throwable] = errors.iterator

  def +++(errors: Throwable*): AggregateCodegenError = AggregateCodegenError(errors ++ errors: _*)
}
object AggregateCodegenError {
  def apply(errors: Throwable*): AggregateCodegenError = {
    def message(t: Throwable) = s"[${t.getClass.getSimpleName}] '${Option(t.getMessage).getOrElse("no message")}'"
    val msg                   = s"""Multiple Errors: ${errors.map(message).mkString(", ")}"""
    val cause                 = errors.headOption.map { head =>
      errors.foreach(head.addSuppressed)
      head
    }
    new AggregateCodegenError(Some(msg), cause, errors)
  }
}
