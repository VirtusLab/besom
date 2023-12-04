package besom.model.hcl2

/** Type represents a datatype in the Pulumi Schema. Types created by this package are identical if they are equal values.
  */
sealed trait Type {}

/** A case class representing an HCL2 attribute.
  *
  * @param name
  *   The attribute's name.
  * @param value
  *   The attribute's value represented by `Expression`.
  */
case class Attribute(
  name: String,
  value: Expression
)

/** Represents a semantically-analyzed HCL2 expression. */
trait Expression:
  /** @return the type of the expression. */
  def `type`(): Type

