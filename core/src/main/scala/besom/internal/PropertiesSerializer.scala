package besom.internal

import com.google.protobuf.struct.{Struct, Value}, Value.Kind, Kind.*
import Constants.{IdPropertyName, UrnPropertyName}

/** Controls the serialization of RPC structures.
  *
  * @param keepOutputValues
  *   true if we are keeping output values If the monitor does not support output values, they will not be kept, even when set to true.
  */
case class SerializationOptions(
  keepOutputValues: Boolean
)

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
  ): Result[SerializationResult] =
    serializeFilteredProperties(args, key => key == IdPropertyName || key == UrnPropertyName)

  /** serializeFilteredProperties walks the props object passed in, awaiting all interior promises for properties with keys that match the
    * provided filter, creating a reasonable POJO object that can be remoted over to registerResource.
    */
  def serializeFilteredProperties[A: ArgsEncoder](
    args: A,
    filter: String => Boolean
  ): Result[SerializationResult] =
    summon[ArgsEncoder[A]].encode(args, filter).map { case (fieldsToResources, value) =>
      SerializationResult(value, detectUnknowns(Value(Kind.StructValue(value))), fieldsToResources)
    }

  private[internal] def detectUnknowns(value: Value): Boolean =
    value.kind match
      case StringValue(str) => str == Constants.UnknownStringValue
      case StructValue(struct) =>
        struct.fields.foldLeft(false) { case (prev, (_, value)) =>
          prev || detectUnknowns(value)
        }
      case ListValue(list) =>
        list.values.foldLeft(false) { case (prev, value) =>
          prev || detectUnknowns(value)
        }
      case _ => false // all other leaf types
