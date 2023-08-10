package besom.codegen

import besom.codegen.metaschema.PropertyDefinition

case class TypeProperty(
  name: String,
  definition: PropertyDefinition,
  isRequired: Boolean
)