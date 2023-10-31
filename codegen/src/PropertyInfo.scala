package besom.codegen

import scala.meta._

import besom.codegen.metaschema.PropertyDefinition

case class PropertyInfo(
  name: Term.Name,
  isOptional: Boolean,
  baseType: Type,
  argType: Type,
  inputArgType: Type,
  defaultValue: Option[Term],
  constValue: Option[Term],
  isSecret: Boolean
)
