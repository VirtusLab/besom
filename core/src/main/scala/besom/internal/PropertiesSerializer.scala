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
    summon[ArgsEncoder[A]].encode(args, filter).map { case (fieldsToResources, value) =>
      SerializationResult(value, detectUnknowns(Value(Kind.StructValue(value))), fieldsToResources)
    }

  private[internal] def detectUnknowns(value: Value): Boolean =
    value.kind match
      case StringValue(str) => str == Constants.UnknownValue
      case StructValue(struct) =>
        struct.fields.foldLeft(false) { case (prev, (_, value)) =>
          prev || detectUnknowns(value)
        }
      case ListValue(list) =>
        list.values.foldLeft(false) { case (prev, value) =>
          prev || detectUnknowns(value)
        }
      case _ => false // all other leaf types
