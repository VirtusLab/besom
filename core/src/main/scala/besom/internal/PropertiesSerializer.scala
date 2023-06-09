package besom.internal

import com.google.protobuf.struct.{Struct, Value}, Value.Kind, Kind.*
import Constants.{IdPropertyName, UrnPropertyName}

case class SerializationResult(
  serialized: Struct,
  containsUnknowns: Boolean,
  propertyToDependentResources: Map[String, Set[Resource]]
)

object PropertiesSerializer:
  def serializeResourceProperties[A: ArgsEncoder](
    args: A
  ): Result[SerializationResult] =
    serializeFilteredProperties(args, key => key == IdPropertyName || key == UrnPropertyName)

  def serializeFilteredProperties[A: ArgsEncoder](
    args: A,
    filter: String => Boolean
  ): Result[SerializationResult] =
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
