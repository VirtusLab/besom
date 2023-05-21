package besom.internal

import com.google.protobuf.struct.Value, Value.Kind, Kind.*
import Constants.{IdPropertyName, UrnPropertyName}

case class SerializationResult(
  serialized: Value,
  containsUnknowns: Boolean,
  propertyToDependentResources: Map[String, Set[Resource]]
)

object PropertiesSerializer:
  def serializeResourceProperties[A: ArgsEncoder](
    label: String,
    args: A
  ): Result[SerializationResult] =
    serializeFilteredProperties(label, args, key => key != IdPropertyName && key != UrnPropertyName)

  def serializeFilteredProperties[A: ArgsEncoder](
    label: String,
    args: A,
    filter: String => Boolean
  ): Result[SerializationResult] =
    summon[ArgsEncoder[A]].encode(args, filter).map { case (fieldsToResources, value) =>
      SerializationResult(value, detectUnknowns(value), fieldsToResources)
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
