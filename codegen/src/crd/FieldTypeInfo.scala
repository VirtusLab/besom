package besom.codegen.crd

import scala.meta.*

case class FieldTypeInfo(
  name: Name,
  description: Option[Seq[String]],
  isOptional: Boolean,
  baseType: Type,
  isSecret: Boolean
)

object FieldTypeInfo:
  def apply(
    name: String,
    baseType: Type,
    jsonSchema: JsonSchemaProps,
    parentJsonSchema: JsonSchemaProps,
    isSecret: Boolean = false
  ): FieldTypeInfo =
    FieldTypeInfo(
      name = Name(name),
      description = jsonSchema.description.map(_.value),
      isOptional = isOptional(parentJsonSchema.required, name),
      baseType = baseType,
      isSecret = isSecret
    )

  private def isOptional(required: Option[Set[String]], fieldName: String): Boolean =
    !required.getOrElse(Set.empty).contains(fieldName)
