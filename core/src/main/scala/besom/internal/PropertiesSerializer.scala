package besom.internal

import besom.internal.Constants.{IdPropertyName, UrnPropertyName}
import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.struct.Value.Kind.*
import com.google.protobuf.struct.{Struct, Value}

case class SerializationResult(
  serialized: Struct,
  containsUnknowns: Boolean,
  propertyToDependentResources: Map[String, Set[Resource]]
)

object PropertiesSerializer:

  /** serializeResourceProperties walks the props object passed in, awaiting all interior promises besides those for `id` and `urn`,
    * creating a reasonable POJO object that can be remoted over to registerResource.
    */
  def serializeResourceProperties[A: ArgsEncoder](
    args: A
  )(using Context): Result[SerializationResult] =
    serializeFilteredProperties(args, key => key == IdPropertyName || key == UrnPropertyName)

  /** serializeFilteredProperties walks the props object passed in, awaiting all interior promises for properties with keys that match the
    * provided filter, creating a reasonable POJO object that can be remoted over to registerResource.
    */
  def serializeFilteredProperties[A: ArgsEncoder](
    args: A,
    filter: String => Boolean
  )(using Context): Result[SerializationResult] =
    summon[ArgsEncoder[A]].encode(args, filter).map { case (fieldsToMetadata, value) =>
      SerializationResult(value, detectUnknowns(fieldsToMetadata), fieldsToResources(fieldsToMetadata))
    }

  private[internal] def detectUnknowns(metadata: Map[String, Metadata]): Boolean = metadata.values.exists(_.unknown)
  private[internal] def fieldsToResources(metadata: Map[String, Metadata])(using Context): Map[String, Set[Resource]] =
    metadata.map { case (k, m) =>
      (k, m.dependencies.map(urn => DependencyResource(Output(urn)).asInstanceOf[Resource]).toSet)
    }
