package besom.internal

import com.google.protobuf.struct.*, Value.Kind

case class SerializationResult(
  serialized: Struct,
  containsUnknowns: Boolean,
  propertyToDependentResources: Map[String, Set[Resource]]
)

object PropertiesSerializer:
  def serializeFilteredProperties[A: Encoder](
    label: String,
    args: A,
    filter: String => Boolean,
    keepResources: Boolean
  ): Result[SerializationResult] =
    ???
